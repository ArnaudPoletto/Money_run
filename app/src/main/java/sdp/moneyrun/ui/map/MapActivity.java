package sdp.moneyrun.ui.map;

import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.collection.LongSparseArray;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.CircleManager;
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.utils.BitmapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import sdp.moneyrun.Helpers;
import sdp.moneyrun.R;
import sdp.moneyrun.database.GameDatabaseProxy;
import sdp.moneyrun.database.RiddlesDatabase;
import sdp.moneyrun.game.Game;
import sdp.moneyrun.map.Coin;
import sdp.moneyrun.map.CoinGenerationHelper;
import sdp.moneyrun.map.LocationCheckObjectivesCallback;
import sdp.moneyrun.map.MapPlayerListAdapter;
import sdp.moneyrun.map.Riddle;
import sdp.moneyrun.map.TrackedMap;
import sdp.moneyrun.player.LocalPlayer;
import sdp.moneyrun.player.Player;


/*
this map implements all the functionality we will need.
 */
@SuppressWarnings({"CanBeFinal", "FieldCanBeLocal", "FieldMayBeFinal"})
public class MapActivity extends TrackedMap implements OnMapReadyCallback {
    public static final double THRESHOLD_DISTANCE = 5;
    private static final double ZOOM_FOR_FEATURES = 15.;
    private static int chronometerCounter = 0;
    private final String TAG = MapActivity.class.getSimpleName();
    private final String COIN_ID = "COIN";
    private final float ICON_SIZE = 1.5f;
    private Chronometer chronometer;
    @Nullable
    private RiddlesDatabase riddleDb;
    private Location currentLocation;
    private Player player;
    private GameDatabaseProxy proxyG;
    private TextView currentScoreView;
    private Button exitButton;
    private Button questionButton;
    private Button leaderboardButton;
    private LocalPlayer localPlayer;
    @Nullable
    private Game game;
    private String gameId;
    private boolean addedCoins = false;
    private boolean host = false;
    private MapPlayerListAdapter ldbListAdapter;
    public int coinsToPlace;

    private CircleManager circleManager;
    private double game_radius;
    private int game_time;
    private Location game_center;
    private float circleRadius;
    private double shrinkingFactor = 0.9;

    private ArrayList<Coin> seenCoins;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();

        getExtras();
        initializeVariables();

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        createMap(savedInstanceState, R.id.mapView, R.layout.activity_map);
        mapView.getMapAsync(this);

        getViews();

        String default_score = getString(R.string.map_score_text, 0);
        currentScoreView.setText(default_score);

        addExitButton();
        addQuestionButton();
        addLeaderboardButton();

        mapView.addOnDidFinishRenderingMapListener(fully -> {
            if (gameId != null) {
                initializeGame(gameId);
            }
        });
    }

    public void getExtras(){
        player = (Player) getIntent().getSerializableExtra("player");
        gameId = getIntent().getStringExtra("currentGameId");
        if (gameId == null) {
            gameId = getIntent().getStringExtra("gameId");
        }
        host = getIntent().getBooleanExtra("host", false);
    }

    public void initializeVariables(){
        seenCoins = new ArrayList<>();
        proxyG = new GameDatabaseProxy();
        localPlayer = new LocalPlayer();
        try {
            riddleDb = RiddlesDatabase.createInstance(getApplicationContext());
        } catch (RuntimeException e) {
            riddleDb = RiddlesDatabase.getInstance();
        }
    }

    public void getViews(){
        currentScoreView = findViewById(R.id.map_score_view);
        chronometer = findViewById(R.id.mapChronometer);
        exitButton = findViewById(R.id.close_map);
        questionButton = findViewById(R.id.new_question);
        leaderboardButton = findViewById(R.id.in_game_scores_button);
    }

    /**
     * @param gameId The game ID to fetch the game from the DB
     *               Place the coins if user is host and coins have not been placed yet
     *               It then finds the game and put the coins in it, the database is then updated
     *               finishes by adding a listener for the coins
     */
    public void initializeGame(String gameId) {

        proxyG.getGameDataSnapshot(gameId).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                game = proxyG.getGameFromTaskSnapshot(task);
                coinsToPlace = game.getNumCoins();
                game_radius = game.getRadius();
                circleRadius = (float) game_radius;
                game_time = (int) Math.floor(game.getDuration() * 60);
                game_center = game.getStartLocation();
                initChronometer();

                if (!addedCoins && host) {
                    placeRandomCoins(coinsToPlace, game_radius, THRESHOLD_DISTANCE);
                    addedCoins = true;
                    initCircle();
                }
                addCoinsListener();
                if (host) {
                    game.setCoins(localPlayer.getLocallyAvailableCoins(), true);
                    proxyG.updateGameInDatabase(game, null);
                } else {
                    localPlayer.setLocallyAvailableCoins((ArrayList<Coin>) game.getCoins());
                }

            } else {
                Log.e(TAG, task.getException().getMessage());
            }
        });
    }

    private void addCoinsListener() {

        proxyG.addCoinListener(game, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GenericTypeIndicator<List<Coin>> t = new GenericTypeIndicator<List<Coin>>() {
                };
                List<Coin> newCoins = snapshot.getValue(t);
                if (newCoins == null) {
                    return;
                }
                localPlayer.syncAvailableCoinsFromDb(new ArrayList<>(newCoins));

                symbolManager.deleteAll();
                for (Coin coin : newCoins) {
                    addCoin(coin, false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(Game.class.getSimpleName(), error.getMessage());
            }

        });

    }


    private void addLeaderboardButton() {
        leaderboardButton.setOnClickListener(v -> onButtonShowLeaderboard(mapView, true, R.layout.in_game_scores));
    }


    /**
     * Add the functionality of leaving the map Activity
     */
    private void addExitButton() {
        exitButton.setOnClickListener(v -> finish()); //TODO: end game not finish
    }

    /**
     * Add functionality to the question button so that we can see random questions popup
     */
    private void addQuestionButton() {
        questionButton.setOnClickListener(v -> onButtonShowQuestionPopupWindowClick(mapView, true, R.layout.question_popup, riddleDb.getRandomRiddle(), null));
    }

    /**
     * @param mapboxMap the map where everything will be done
     *                  this overrides the OnMapReadyCallback in the implemented interface
     *                  We set up the symbol manager here, it will allow us to add markers and other visual stuff on the map
     *                  Then we setup the location tracking
     */
    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {

        callback = new LocationCheckObjectivesCallback(this);

        mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {

            GeoJsonOptions geoJsonOptions = new GeoJsonOptions().withTolerance(0.4f);
            symbolManager = new SymbolManager(mapView, mapboxMap, style, null, geoJsonOptions);
            symbolManager.setIconAllowOverlap(true);
            symbolManager.setTextAllowOverlap(true);
            style.addImage(COIN_ID, BitmapUtils.getBitmapFromDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.coin_image)), false);
            enableLocationComponent(style);

            circleManager = new CircleManager(mapView, mapboxMap, style);

        });

        this.mapboxMap = mapboxMap;
    }

    public Chronometer getChronometer() {
        return chronometer;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public LocalPlayer getLocalPlayer() {
        return localPlayer;
    }

    public double getGameRadius(){return game_radius;}
    public double getGameDuration(){return game_time;}
    public Location getGameCenter(){return game_center;}


    @Nullable
    public String getPlayerId() {
        return player.getPlayerId();
    }

    public MapPlayerListAdapter getLdbListAdapter() {
        return ldbListAdapter;
    }

    /**
     * The chronometer will countdown from the maximum time of a game to 0
     */
    private void initChronometer() {

        chronometer.start();
        chronometer.setFormat("REMAINING TIME " + (game_time - chronometerCounter));
        chronometer.setOnChronometerTickListener(chronometer -> {
            if (chronometerCounter < game_time) {
                chronometerCounter += 1;
            } else {
                Game.endGame(localPlayer.getCollectedCoins().size(), localPlayer.getScore(), player.getPlayerId(), MapActivity.this);
            }
            chronometer.setFormat("REMAINING TIME " + (game_time - chronometerCounter));
        });
    }


    /**
     * Creates the popup where the question and the possible answers will be shown to the user
     *
     * @param view      The view on top of which the popup will be shown
     * @param focusable whether the popup is focused
     * @param layoutId  the layoutId of the popup
     * @param riddle    The riddle that will be asked
     * @param coin      the coin that triggered a riddle
     */
    public void onButtonShowQuestionPopupWindowClick(View view, Boolean focusable, int layoutId, @NonNull Riddle riddle, Coin coin) {

        PopupWindow popupWindow = Helpers.onButtonShowPopupWindowClick(this, view, focusable, layoutId);
        TextView tv = popupWindow.getContentView().findViewById(R.id.question);
        int correctId;

        //changes the text to the current question
        tv.setText(riddle.getQuestion());

        int[] buttonIds = {R.id.question_choice_1, R.id.question_choice_2, R.id.question_choice_3, R.id.question_choice_4};
        TextView buttonView;

        //Loops to find the ID of the button solution and assigns the text to each button
        for (int i = 0; i < 4; i++) {

            buttonView = popupWindow.getContentView().findViewById(buttonIds[i]);
            buttonView.setText(riddle.getPossibleAnswers()[i]);

            //Found the id of the solution button
            if (riddle.getPossibleAnswers()[i].equals(riddle.getAnswer())) {
                correctId = buttonIds[i];
                correctAnswerListener(popupWindow, correctId, coin, riddle);
            } else {
                wrongAnswerListener(popupWindow, buttonIds[i], coin, riddle);
            }
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onButtonShowLeaderboard(View view, Boolean focusable, int layoutId) {
        PopupWindow popupWindow = Helpers.onButtonShowPopupWindowClick(this, view, focusable, layoutId);

        proxyG.getGameDataSnapshot(gameId).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                game = proxyG.getGameFromTaskSnapshot(task);
                ArrayList<Player> playerList = new ArrayList<>(game.getPlayers());
                playerList.sort((o1, o2) -> Integer.compare(o2.getScore(), o1.getScore()));

                for (Player p : playerList) {
                    System.out.println(p.getName() + p.getScore());
                }
                ldbListAdapter = new MapPlayerListAdapter(this, new ArrayList<>());
                ListView leaderboard = popupWindow.getContentView().findViewById(R.id.in_game_scores_listview);
                ldbListAdapter.addAll(playerList);
                leaderboard.setAdapter(ldbListAdapter);
            } else {
                Log.e(TAG, task.getException().getMessage());
            }
        });

    }

    public void closePopupListener(@NonNull PopupWindow popupWindow, int Id) {
        popupWindow.getContentView().findViewById(Id).setOnClickListener(v -> popupWindow.dismiss());
    }

    /**
     * Listener on the wrong possible answers of the riddle
     *
     * @param popupWindow the current popup where the question is displayed
     * @param btnId       the id of one of the incorrect answer buttons
     * @param coin        the coin that triggered the question
     * @param riddle      the riddle that is being displayed
     */
    public void wrongAnswerListener(@NonNull PopupWindow popupWindow, int btnId, @Nullable Coin coin, @NonNull Riddle riddle) {

        popupWindow.getContentView().findViewById(btnId).setOnClickListener(v -> {

            popupWindow.dismiss();
            PopupWindow wrongAnswerPopupWindow = Helpers.onButtonShowPopupWindowClick(MapActivity.this, mapView, true, R.layout.wrong_answer_popup);
            TextView tv = wrongAnswerPopupWindow.getContentView().findViewById(R.id.question);
            tv.setText(riddle.getQuestion());

            tv = wrongAnswerPopupWindow.getContentView().findViewById(R.id.incorrect_answer_text);
            tv.setText(String.format("%s: '%s'", getString(R.string.incorrect_answer_message), riddle.getAnswer()));
            tv.setTextColor(Color.RED);


            closePopupListener(wrongAnswerPopupWindow, R.id.continue_run);

            if (coin != null) {
                removeCoin(coin, false);
            }
        });
    }


    /**
     * Listener on the correct answer of the riddle
     *
     * @param popupWindow the current popup where the question is displayed
     * @param btnId       the id of the correct answer button
     * @param coin        the coin that triggered the question
     * @param riddle      the riddle that is being displayed
     */
    public void correctAnswerListener(@NonNull PopupWindow popupWindow, int btnId, @Nullable Coin coin, @NonNull Riddle riddle) {

        popupWindow.getContentView().findViewById(btnId).setOnClickListener(v -> {

            popupWindow.dismiss();
            PopupWindow correctAnswerPopupWindow = Helpers.onButtonShowPopupWindowClick(MapActivity.this, mapView, true, R.layout.correct_answer_popup);
            TextView tv = correctAnswerPopupWindow.getContentView().findViewById(R.id.question);
            tv.setText(riddle.getQuestion());

            tv = correctAnswerPopupWindow.getContentView().findViewById(R.id.incorrect_answer_text);
            tv.setTextColor(Color.GREEN);

            closePopupListener(correctAnswerPopupWindow, R.id.collect_coin);
            if (coin != null)
                removeCoin(coin, true);
        });
    }


    /**
     * @param coin Adds a coins to the list of remaining coins and adds it to the map
     */
    public void addCoin(@Nullable Coin coin, boolean addLocal) {
        if (coin == null) {
            throw new NullPointerException("added coin cannot be null");
        }

        if (addLocal) {
            localPlayer.addLocallyAvailableCoin(coin);
        }
        symbolManager.create(new SymbolOptions().withLatLng(new LatLng(coin.getLatitude(), coin.getLongitude())).withIconImage(COIN_ID).withIconSize(ICON_SIZE));
    }


    /**
     * @param coin removes a coin from the list of remaining coins, adds it to the list of collected coin
     *             and removes it from the map
     */
    public void removeCoin(@Nullable Coin coin, Boolean collected) {
        if (coin == null) {
            throw new NullPointerException("removed coined is null");
        }

        localPlayer.updateCoins(coin, collected);

        if (collected) {
            String default_score = getString(R.string.map_score_text, localPlayer.getScore());
            currentScoreView.setText(default_score);
            List<Player> temp_players = game.getPlayers();
            temp_players.remove(player);
            player.setScore(localPlayer.getScore(),true);
            temp_players.add(player);
            game.setPlayers(temp_players,true);
            game.setCoins(localPlayer.toSendToDb(),true);
            proxyG.updateGameInDatabase(game,null);
        }

        deleteCoinFromMap(coin);

        circleRadius *= shrinkingFactor;
        initCircle();

    }

    public void deleteCoinFromMap(Coin coin){
        LongSparseArray<Symbol> symbols = symbolManager.getAnnotations();

        for (int i = 0; i < symbols.size(); ++i) {
            Symbol symbol = symbols.valueAt(i);
            if (coin.getLatitude() == symbol.getLatLng().getLatitude() && coin.getLongitude() == symbol.getLatLng().getLongitude()) {
                symbolManager.delete(symbol);
            }
        }
    }

    /**
     * @param number    number of coins to add
     * @param maxRadius max distance of a coin from the center
     * @param minRadius min distance of a coin from the center
     */
    public void placeRandomCoins(int number, double maxRadius, double minRadius) {
        if (number < 0 || maxRadius <= 0 || minRadius <= 0)
            throw new IllegalArgumentException("Number of coins to place is less than 0, number of coin is  " + number);
        if (minRadius >= maxRadius)
            throw new IllegalArgumentException("Min radius cannot be bigger or equal than max Radius ");

        for (int i = 0; i < number; i++) {
            Location loc = CoinGenerationHelper.getRandomLocation(getCurrentLocation(), maxRadius, minRadius);
            addCoin(new Coin(loc.getLatitude(), loc.getLongitude(), CoinGenerationHelper.coinValue(loc, getCurrentLocation())), true);
        }
    }

    /**
     * @param location Used to check if location is near a coin or not
     */
    @Override
    public void checkObjectives(@NonNull Location location) {
        currentLocation = location;
        Coin coin = nearestCoin(location, localPlayer.getLocallyAvailableCoins(), THRESHOLD_DISTANCE);
        if (coin != null && !seenCoins.contains(coin)) {
            seenCoins.add(coin);
            onButtonShowQuestionPopupWindowClick(mapView, true, R.layout.question_popup, riddleDb.getRandomRiddle(), coin);
        }

    }

    /**
     * Draws a circle of a predefined radius that corresponds to the radius inside which coins get generated
     * and it shrinks by a shrinking factor that is also predefined
     */
    public void initCircle() {
        circleManager.deleteAll();
        CircleOptions circleOptions = new CircleOptions();
        circleOptions = circleOptions.withCircleRadius(circleRadius);
        circleOptions = circleOptions.withCircleOpacity(0.4f);
        circleOptions = circleOptions.withCircleColor("" + Color.blue(8));
        System.out.println("Current lat is " + getCurrentLocation().getLatitude() + " and longitude is : " + getCurrentLocation().getLongitude());
        circleOptions.withLatLng(new LatLng(getCurrentLocation().getLatitude(), getCurrentLocation().getLongitude()));
        circleManager.create(circleOptions);
    }

    public CircleManager getCircleManager() {
        return circleManager;
    }

}
