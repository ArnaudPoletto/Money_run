package sdp.moneyrun.menu;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;
import java.util.List;

import sdp.moneyrun.database.GameDatabaseProxy;
import sdp.moneyrun.database.GameDbData;
import sdp.moneyrun.map.Coin;
import sdp.moneyrun.game.Game;
import sdp.moneyrun.map.LocationRepresentation;
import sdp.moneyrun.player.Player;
import sdp.moneyrun.R;
import sdp.moneyrun.map.Riddle;
import sdp.moneyrun.ui.game.GameLobbyActivity;

public class NewGameImplementation extends MenuImplementation {
    public NewGameImplementation(Activity activity,
                                 DatabaseReference databaseReference,
                                 Player user,
                                 ActivityResultLauncher<String[]> requestPermissionsLauncher,
                                 FusedLocationProviderClient fusedLocationClient){
        super(activity, databaseReference, user, requestPermissionsLauncher, fusedLocationClient);
    }

    /**
     * Event that occurs when the user wants to add a new game.
     *
     * @param view the current view
     */
    public void onClickShowNewGamePopupWindow(View view) {
        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater)
                activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.new_game_popup, null);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);

        // show the popup window at wanted location
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

        LinearLayout newGameLayout = popupView.findViewById(R.id.newGameLayout);
        Button newGameButton = newGameLayout.findViewById(R.id.newGameSubmit);

        newGameButton.setOnClickListener(v -> onSubmitPostNewGame(newGameLayout));
        //TODO, post the game, but also join it and launch the lobby activity
    }

    /**
     * Create a new game.
     *
     * @param newGameLayout the game layout
     */
    public void onSubmitPostNewGame(LinearLayout newGameLayout) {
        TextView nameGameView = newGameLayout.findViewById(R.id.nameGameField);
        TextView maxPlayerNumberView = newGameLayout.findViewById(R.id.maxPlayerCountField);
        String gameName = nameGameView.getText().toString().trim();
        String maxPlayerNumberStr = maxPlayerNumberView.getText().toString().trim();
        if (gameName.isEmpty()) {
            nameGameView.setError("This field is required");
            return;
        }
        if (maxPlayerNumberStr.isEmpty()) {
            maxPlayerNumberView.setError("This field is required");
            return;
        }

        int maxPlayerNumber = Integer.parseInt(maxPlayerNumberStr);

        if (maxPlayerNumber < 1) {
            maxPlayerNumberView.setError("There should be at least one player in a game");
            return;
        }

        postNewGame(gameName, maxPlayerNumber);
    }

    /**
     * Post a new game.
     *
     * @param name              the game name
     * @param maxPlayerCount    the maximum number of players in the game
     * @return the game
     */
    @SuppressLint("MissingPermission")
    public void postNewGame(String name, int maxPlayerCount) {
        DatabaseReference gameReference = databaseReference.child(activity.getString(R.string.database_game)).push();
        DatabaseReference startLocationReference = databaseReference
                .child(activity.getString(R.string.database_game))
                .child(gameReference.getKey()).child(activity.getString(R.string.database_game_start_location));

        // Grant permissions if necessary
        requestLocationPermissions(requestPermissionsLauncher);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(activity, location -> {
                    // Got last known location. In some rare situations this can be null
                    // In this case, the game cannot be instanciated
                    if (location == null) {
                        Log.e("location", "Error getting location");
                    }

                    // Build new game given fields filled by user
                    List<Riddle> riddles = new ArrayList<>();
                    List<Coin> coins = new ArrayList<>();


                    //this coin is added, otherwise if a list is added as a node to the db
                    //and the list is empty, no node will get created
                    //so when later we try to get the game from the db, we will get a null value
                    //for the list of coins and that will trigger an exception :/
                    coins.add(new Coin(1,1,99));

                    Game game = new Game(name, user, maxPlayerCount, riddles, coins, location, true);

                    // post game to database
                    GameDatabaseProxy gdb = new GameDatabaseProxy();
                    gdb.putGame(game);
                    Intent lobbyIntent = new Intent(activity.getApplicationContext(), GameLobbyActivity.class);
                    lobbyIntent.putExtra(activity.getString(R.string.join_game_lobby_intent_extra_id), game.getId());
                    lobbyIntent.putExtra(activity.getString(R.string.join_game_lobby_intent_extra_user), user);
                    activity.startActivity(lobbyIntent);
                    activity.finish();

                    // Post location to database
                    //LocationRepresentation locationRep = new LocationRepresentation(location.getLatitude(), location.getLongitude());
                    //startLocationReference.setValue(locationRep);
                });
    }
}
