package sdp.moneyrun.map;

import android.content.Intent;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.Intents;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import sdp.moneyrun.R;
import sdp.moneyrun.database.GameDatabaseProxy;
import sdp.moneyrun.game.Game;
import sdp.moneyrun.player.Player;
import sdp.moneyrun.ui.MainActivity;
import sdp.moneyrun.ui.game.EndGameActivity;
import sdp.moneyrun.ui.map.MapActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MapInstrumentedTest {

    @Nullable
    private final CountDownLatch moved = null;
    final double minZoomForBuilding = 15.;

    @BeforeClass
    public static void setPersistence() {
        if (!MainActivity.calledAlready) {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            MainActivity.calledAlready = true;
        }
    }

    @NonNull
    private Intent getStartIntent() {
        Player currentUser = new Player("3212", "CURRENT_USER", 0);
        Intent toStart = new Intent(ApplicationProvider.getApplicationContext(), MapActivity.class);
        toStart.putExtra("currentUser", currentUser);
        return toStart;
    }

    @NonNull
    public Game getGame() {
        String name = "Game";
        Player host = new Player("98934", "Bob", 0);
        int maxPlayerCount = 2;
        List<Riddle> riddles = new ArrayList<>();
        riddles.add(new Riddle("yes?", "blue", "green", "yellow", "brown", "a"));
        List<Coin> coins = new ArrayList<>();
        Location location = new Location("LocationManager#GPS_PROVIDER");
        location.setLatitude(37.42);
        location.setLongitude(-122.084);

        int num_coins = 0;
        double duration = 4;
        double radius = 20;

        return new Game(name, host, maxPlayerCount, riddles, coins, location, true, num_coins, radius, duration);
    }

    public Intent createIntentAndPutInDB() {
        Player host = new Player("1234567891", "Bob", 0);
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MapActivity.class);
        intent.putExtra("player", host);
        intent.putExtra("host", true);
        intent.putExtra("useDB", true);

        GameDatabaseProxy gdp = new GameDatabaseProxy();
        Game game = getGame();

        List<Player> players = game.getPlayers();
        players.add(host);

        String id = gdp.putGame(game);

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        intent.putExtra("currentGameId", id);

        return intent;
    }


    @Test
    public void moveCameraToWorks() {
        Intent intent = createIntentAndPutInDB();
        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {

            float lat = 8f;
            float lon = 8f;
            final AtomicBoolean finished = new AtomicBoolean(false);
            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                a.mapView.addOnCameraDidChangeListener(animated -> a.mapView.addOnDidFinishRenderingFrameListener(fully1 -> {
                    if (fully1) {
                        LatLng latLng = a.getMapboxMap().getCameraPosition().target;
                        assertEquals(latLng.getLatitude(), 8.0, 0.1);
                        assertEquals(latLng.getLongitude(), 8.0, 0.1);
                        finished.set(true);
                    }
                }));

                a.moveCameraTo(lat, lon);
            }));
            do {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
            } while (!finished.get());
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(-1, 2);
        }
    }

    @Test
    public void testSymbolManager() {
        Intent intent = createIntentAndPutInDB();
        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {

            final AtomicBoolean finished = new AtomicBoolean(false);

            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                if (fully) {
                    assertEquals(a.getSymbolManager().getIconAllowOverlap(), true);
                    assertEquals(a.getSymbolManager().getTextAllowOverlap(), true);
                    finished.set(true);
                }
            }));
            do {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
            } while (!finished.get());

        } catch (Exception e) {
            assertEquals(-1, 2);
            e.printStackTrace();
        }
    }

    @Test
    public void locationTracking() {
        Intent intent = createIntentAndPutInDB();

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);
            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                assertEquals(a.getMapboxMap().getLocationComponent().getCameraMode(), CameraMode.TRACKING);
                assertEquals(a.getMapboxMap().getLocationComponent().getRenderMode(), RenderMode.COMPASS);
                finished.set(true);
            }));
            do {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
            } while (!finished.get());

        } catch (Exception e) {
            assertEquals(-1, 2);
            e.printStackTrace();
        }
    }


    @Test
    public void chronometerTest() {
        Intent intent = createIntentAndPutInDB();

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {

            final AtomicBoolean finished = new AtomicBoolean(false);
            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                if (fully) {
                    finished.set(true);
                }
            }));
            do {
                try {
                    Thread.sleep(400);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
            } while (!finished.get());
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                assertEquals(-1, 2);
            }
            scenario.onActivity(a -> {
                assertFalse(a.getChronometer().isCountDown());
                assertTrue(a.getChronometer().getText().toString().contains("REMAINING TIME"));
            });


        }
    }

    @Test
    public void onExplanationNeededWorks() {
        Intent intent = createIntentAndPutInDB();

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            ArrayList<String> reasons = new ArrayList<>();
            reasons.add("e");

            scenario.onActivity(a -> a.onExplanationNeeded(reasons));
            assertEquals(1, 1);
        } catch (Exception e) {
            assertEquals(-1, 2);
            e.printStackTrace();
        }
    }

    @Test
    public void onPermissionResultWorks() {
        Intent intent = createIntentAndPutInDB();

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);
            boolean granted = true;

            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                a.onPermissionResult(granted);
                finished.set(true);
            }));
            do {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
            } while (!finished.get());
        } catch (Exception e) {
            assertEquals(-1, 2);
            e.printStackTrace();
        }
    }

    @Test
    public void addCoinAddsCoinToMap() {
        Intent intent = createIntentAndPutInDB();

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);

            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                if (fully) {
                    Location curloc = a.getCurrentLocation();
                    Coin coin = new Coin(curloc.getLatitude() / 2, curloc.getLongitude() / 2, 1);
                    a.addCoin(coin, true);
                    Coin coin2 = new Coin(curloc.getLatitude() / 3, curloc.getLongitude() / 100, 1);
                    a.addCoin(coin2, true);
                    finished.set(true);
                }
            }));
            do {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
            } while (!finished.get());
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            scenario.onActivity(a -> assertEquals(2, a.getSymbolManager().getAnnotations().size()));
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(-1, 2);
        }
    }

    @Test(expected = Exception.class)
    public void addCoinFailsCorrectly() {
        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(MapActivity.class)) {

            scenario.onActivity(a -> {
                a.addCoin(null, true);
            });
        }
    }

    @Test(expected = Exception.class)
    public void removeCoinFailsCorrectly() {
        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(MapActivity.class)) {

            scenario.onActivity(a -> {
                a.removeCoin(null, true);
            });
        }
    }

    @Test(expected = Exception.class)
    public void placeRandomCoinsFailsCorrectly1() {
        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(MapActivity.class)) {
            scenario.onActivity(a -> {
                a.placeRandomCoins(-1, 2, 1);
            });
        }
    }

    @Test(expected = Exception.class)
    public void placeRandomCoinsFailsCorrectly2() {
        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(MapActivity.class)) {
            scenario.onActivity(a -> {
                a.placeRandomCoins(1, 2, 4);
            });
        }
    }


    @Test
    public void endGameStartsActivity() {
        Intent intent = createIntentAndPutInDB();

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            Intents.init();

            final AtomicBoolean finished = new AtomicBoolean(false);

            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                Game.endGame(a.getLocalPlayer().getCollectedCoins().size(), a.getLocalPlayer().getScore(), a.getPlayerId(),new ArrayList<>(), a);
                finished.set(true);
            }));
            do {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
            } while (!finished.get());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            intended(hasComponent(EndGameActivity.class.getName()));
            Intents.release();
        }
    }

    @Test
    public void questionButtonWorks() {

        Intent intent = createIntentAndPutInDB();

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);

            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> finished.set(true)));
            do {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
            } while (!finished.get());

            onView(withId(R.id.new_question)).perform(ViewActions.click());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            onView(withId(R.id.ask_question_popup)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void questionWorksOnCorrectAnswer() {

        String question = "What is the color of the sky";
        String correctAnswer = "blue";
        Riddle riddle = new Riddle(question, correctAnswer, "blue", "green", "yellow", "brown");
        Intent intent = createIntentAndPutInDB();

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);

            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                if (fully) {
                    a.onButtonShowQuestionPopupWindowClick(a.findViewById(R.id.mapView), true, R.layout.question_popup, riddle, null);

                    finished.set(true);
                }
            }));
            do {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
            } while (!finished.get());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            onView(withId(R.id.question_choice_1)).perform(ViewActions.click());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            onView(withId(R.id.correct_answer_popup)).check(matches(isDisplayed()));

            onView(withId(R.id.collect_coin)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void questionWorksOnWrongAnswer() {

        String question = "What is the color of the sky";
        String correctAnswer = "blue";
        Riddle riddle = new Riddle(question, correctAnswer, "blue", "green", "yellow", "brown");
        Intent intent = createIntentAndPutInDB();

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);

            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                a.onButtonShowQuestionPopupWindowClick(a.findViewById(R.id.mapView), true, R.layout.question_popup, riddle, null);
                finished.set(true);
            }));
            do {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
            } while (!finished.get());


            onView(withId(R.id.question_choice_2)).perform(ViewActions.click());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            onView(withId(R.id.wrong_answer_popup)).check(matches(isDisplayed()));
            onView(withId(R.id.continue_run)).check(matches(isDisplayed()));

        }

    }


    @Test(expected = NoMatchingViewException.class)
    public void continueRunButtonWorks() {
        String question = "What is the color of the sky";
        String correctAnswer = "blue";
        Riddle riddle = new Riddle(question, correctAnswer, "blue", "green", "yellow", "brown");
        Intent intent = createIntentAndPutInDB();

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {

            final AtomicBoolean finished = new AtomicBoolean(false);

            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                a.onButtonShowQuestionPopupWindowClick(a.findViewById(R.id.mapView), true, R.layout.question_popup, riddle, null);
                finished.set(true);
            }));
            do {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
            } while (!finished.get());

            onView(withId(R.id.question_choice_2)).perform(ViewActions.click());
            try {
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            onView(withId(R.id.continue_run)).perform(ViewActions.click());
            try {
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            onView(withId(R.id.ask_question_popup)).check(matches(not(isDisplayed())));


        }
    }

    @Test(expected = NoMatchingViewException.class)
    public void collectCoinButtonWorks() {
        String question = "What is the color of the sky";
        String correctAnswer = "blue";
        Riddle riddle = new Riddle(question, correctAnswer, "blue", "green", "yellow", "brown");
        Intent intent = createIntentAndPutInDB();

        ExpectedException exception = ExpectedException.none();

        exception.expect(NoMatchingViewException.class);
        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {

            final AtomicBoolean finished = new AtomicBoolean(false);

            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> finished.set(true)));
            do {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
            } while (!finished.get());

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            scenario.onActivity(a -> a.onButtonShowQuestionPopupWindowClick(a.findViewById(R.id.mapView), true, R.layout.question_popup, riddle, null));

            onView(withId(R.id.question_choice_1)).perform(ViewActions.click());
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            onView(withId(R.id.collect_coin)).perform(ViewActions.click());
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            onView(withId(R.id.ask_question_popup)).check(matches(not(isDisplayed())));

        }
    }


    @Test
    public void showScoreWorks() {
        Intent intent = createIntentAndPutInDB();

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);

            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                if (fully) {
                    finished.set(true);
                }
            }));
            do {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
            } while (!finished.get());
            String default_text = "Score: 0";
            Espresso.onView(withId(R.id.map_score_view)).check(matches(withText(default_text)));
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            scenario.onActivity(a -> {
                Location curloc = a.getCurrentLocation();
                Coin coin = new Coin(curloc.getLatitude(), curloc.getLongitude(), 1);
                a.addCoin(coin, true);
                a.removeCoin(coin, true);

            });
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String updated_text = "Score: 1";
            Espresso.onView(withId(R.id.map_score_view)).check(matches(withText(updated_text)));

        }
    }


    @Test
    public void collectCoinButtonCollectsCoin() {

        Intent intent = createIntentAndPutInDB();
        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {

            String question = "What is the color of the sky";
            String correctAnswer = "blue";

            Riddle riddle = new Riddle(question, correctAnswer, "blue", "green", "yellow", "brown");

            final AtomicBoolean finished = new AtomicBoolean(false);

            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> finished.set(true)));
            do {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
            } while (!finished.get());

            scenario.onActivity(a -> {
                Location curloc = a.getCurrentLocation();
                Coin coin = new Coin(curloc.getLatitude() / 2, curloc.getLongitude(), 1);

                a.onButtonShowQuestionPopupWindowClick(a.findViewById(R.id.mapView), true, R.layout.question_popup, riddle, coin);
            });

            onView(withId(R.id.question_choice_1)).perform(ViewActions.click());
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                fail();
            }
            onView(withId(R.id.collect_coin)).perform(ViewActions.click());

            scenario.onActivity(a -> assertEquals(0, a.getSymbolManager().getAnnotations().size()));

        }
    }

    @Test
    public void closeButtonWorks() {
        Intent intent = createIntentAndPutInDB();

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            Intents.init();
            final AtomicBoolean finished = new AtomicBoolean(false);

            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                if (fully) {
                    finished.set(true);
                }
            }));
            do {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
            } while (!finished.get());
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            onView(withId(R.id.close_map)).perform(ViewActions.click());
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            assertEquals(Lifecycle.State.DESTROYED, scenario.getState());

            Intents.release();
        }
    }

    @Test
    public void LakeLemanIsDetectedAsInappropriateTest() {
        Intent intent = createIntentAndPutInDB();
        double lat = 46.49396808615545;
        double lon = 6.638823143919147;
        Location location = new Location("");
        location.setLatitude(lat);
        location.setLongitude(lon);
        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);


            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                if (fully) {

                    a.mapView.addOnCameraDidChangeListener(animated -> a.mapView.addOnDidFinishRenderingFrameListener(fully1 -> {
                        if (fully1) {
                            finished.set(true);
                        }
                    }));

                    a.moveCameraWithoutAnimation(lat, lon, minZoomForBuilding);

                }
            }));
            while (true) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
                if (finished.get()) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        assertEquals(-1, 2);
                    }

                    break;
                }
            }

            scenario.onActivity(a -> {
                assert (!a.isLocationAppropriate(location));
            });
        }
    }

    @Test
    public void SwissPlasmaCenterIsDetectedAsInappropriateTest() {
        double lat = 46.517583898897826;
        double lon = 6.565050387400619;
        Location location = new Location("");
        location.setLatitude(lat);
        location.setLongitude(lon);
        Intent intent = createIntentAndPutInDB();
        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);


            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                if (fully) {

                    a.mapView.addOnCameraDidChangeListener(animated -> a.mapView.addOnDidFinishRenderingFrameListener(fully1 -> {
                        if (fully1) {
                            finished.set(true);
                        }

                    }));

                    a.moveCameraWithoutAnimation(lat, lon, minZoomForBuilding);

                }
            }));
            while (true) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
                if (finished.get()) {
                    try {
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        assertEquals(-1, 2);
                    }

                    break;
                }
            }
            scenario.onActivity(a -> {
                assert (!a.isLocationAppropriate(location));
            });
        }
    }


    @Test
    public void RandomBuildingIsDetectedAsInappropriateTest() {
        double lat = 46.517396499876476;
        double lon = 6.645705058098468;
        Location location = new Location("");
        location.setLatitude(lat);
        location.setLongitude(lon);
        Intent intent = createIntentAndPutInDB();

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);


            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                if (fully) {
                    a.mapView.addOnCameraDidChangeListener(animated -> a.mapView.addOnDidFinishRenderingFrameListener(fully1 -> {
                        if (fully1) {
                            finished.set(true);
                        }
                    }));
                    a.moveCameraWithoutAnimation(lat, lon, minZoomForBuilding);
                }
            }));
            while (true) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
                if (finished.get()) {
                    try {
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        assertEquals(-1, 2);
                    }
                    break;
                }
            }
            scenario.onActivity(a -> {
                assert (!a.isLocationAppropriate(location));
            });
        }
    }

    @Test
    public void RandomParkIsDetectedAsAppropriateTest() {
        double minZoomForBuilding = 16.;
        double lat = 46.51479170858094;
        double lon = 6.621513963216489;
        Location location = new Location("");
        location.setLatitude(lat);
        location.setLongitude(lon);
        Intent intent = createIntentAndPutInDB();

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);


            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                if (fully) {
                    a.mapView.addOnCameraDidChangeListener(animated -> a.mapView.addOnDidFinishRenderingFrameListener(fully1 -> {
                        if (fully1) {
                            finished.set(true);
                        }
                    }));
                    a.moveCameraWithoutAnimation(lat, lon, minZoomForBuilding);
                }
            }));
            while (true) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
                if (finished.get()) {
                    try {
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        assertEquals(-1, 2);
                    }

                    break;
                }
            }
            scenario.onActivity(a -> {
                a.isLocationAppropriate(location);
                assert (a.isLocationAppropriate(location));
            });
        }
    }

    @Test
    public void RandomFreePlaceIsDetectedAsAppropriateTest() {
        double lat = 46.51192799046872;
        double lon = 6.619113183264966;
        Location location = new Location("");
        location.setLatitude(lat);
        location.setLongitude(lon);
        Intent intent = createIntentAndPutInDB();

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);


            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                if (fully) {
                    a.mapView.addOnCameraDidChangeListener(animated -> a.mapView.addOnDidFinishRenderingFrameListener(fully1 -> {
                        if (fully1) {
                            finished.set(true);
                        }
                    }));
                    a.moveCameraWithoutAnimation(lat, lon, minZoomForBuilding);
                }
            }));
            while (true) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
                if (finished.get()) {
                    try {
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        assertEquals(-1, 2);
                    }

                    break;
                }
            }
            scenario.onActivity(a -> {
                a.isLocationAppropriate(location);
                assert (a.isLocationAppropriate(location));
            });
        }
    }

    @Test
    public void sportCenterIsDetectedAsInappropriateTest() {
        double lat = 46.511488;
        double lon = 6.618642;
        Location location = new Location("");
        location.setLatitude(lat);
        location.setLongitude(lon);
        Intent intent = createIntentAndPutInDB();

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);


            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                if (fully) {
                    a.mapView.addOnCameraDidChangeListener(animated -> a.mapView.addOnDidFinishRenderingFrameListener(fully1 -> {
                        if (fully1) {
                            finished.set(true);
                        }
                    }));
                    a.moveCameraWithoutAnimation(lat, lon, minZoomForBuilding);
                }
            }));
            while (true) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
                if (finished.get()) {
                    try {
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        assertEquals(-1, 2);
                    }

                    break;
                }
            }
            scenario.onActivity(a -> {
                a.isLocationAppropriate(location);
                assert (!a.isLocationAppropriate(location));
            });
        }
    }

    @Test
    public void RouteCantonaleIsDetectedAsInappropriateTest() {
        double lat = 46.517319;
        double lon = 6.568376;
        Location location = new Location("");
        location.setLatitude(lat);
        location.setLongitude(lon);
        Intent intent = createIntentAndPutInDB();

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);


            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                if (fully) {
                    a.mapView.addOnCameraDidChangeListener(animated -> a.mapView.addOnDidFinishRenderingFrameListener(fully1 -> {
                        if (fully1) {
                            finished.set(true);
                        }
                    }));
                    a.moveCameraWithoutAnimation(lat, lon, minZoomForBuilding);
                }
            }));
            while (true) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
                if (finished.get()) {
                    try {
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        assertEquals(-1, 2);
                    }

                    break;
                }
            }
            scenario.onActivity(a -> {
                a.isLocationAppropriate(location);
                assert (!a.isLocationAppropriate(location));
            });
        }
    }

    @Test
    public void highwayIsDetectedAsInappropriateTest() {
        double lat = 46.526493;
        double lon = 6.580576;
        Location location = new Location("");
        location.setLatitude(lat);
        location.setLongitude(lon);
        Intent intent = createIntentAndPutInDB();

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);

            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                if (fully) {
                    a.mapView.addOnCameraDidChangeListener(animated -> a.mapView.addOnDidFinishRenderingFrameListener(fully1 -> {
                        if (fully1) {
                            finished.set(true);
                        }
                    }));
                    a.moveCameraWithoutAnimation(lat, lon, minZoomForBuilding);
                }
            }));
            while (true) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
                if (finished.get()) {
                    try {
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        assertEquals(-1, 2);
                    }

                    break;
                }
            }
            scenario.onActivity(a -> {
                a.isLocationAppropriate(location);
                assert (!a.isLocationAppropriate(location));
            });
        }
    }

    @Test
    public void placingCoins() {
        Intent intent = createIntentAndPutInDB();

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);

            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                if (fully) {

                    a.mapView.addOnCameraDidChangeListener(animated -> a.mapView.addOnDidFinishRenderingFrameListener(fully1 -> {
                        if (fully1) {
                            finished.set(true);
                        }
                    }));
                    a.moveCameraWithoutAnimation(a.getCurrentLocation().getLatitude(), a.getCurrentLocation().getLongitude(), minZoomForBuilding);

                }
            }));
            while (true) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
                if (finished.get()) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        assertEquals(-1, 2);
                    }

                    break;
                }
            }
            scenario.onActivity(a -> {
                int numberOfCoins = 7;
                a.placeRandomCoins(numberOfCoins, 100, 2);
                assertEquals(a.getLocalPlayer().getLocallyAvailableCoins().size(), numberOfCoins);
            });
        }
    }


    @Test
    public void CollectingACoinRemovesCoinFromDBTest() {

        Player currentUser = new Player("3212", "CURRENT_USER", 0);
        Intent toStart = new Intent(ApplicationProvider.getApplicationContext(), MapActivity.class);
        toStart.putExtra("currentUser", currentUser);

        String name = "Game";
        Player host = new Player("98934", "Bob", 0);
        int maxPlayerCount = 2;
        List<Riddle> riddles = new ArrayList<>();
        riddles.add(new Riddle("yes?", "blue", "green", "yellow", "brown", "a"));
        List<Coin> coins = new ArrayList<>();
        Location location = new Location("LocationManager#GPS_PROVIDER");
        location.setLatitude(37.42);
        location.setLongitude(-122.084);

        int num_coins = 2;
        double duration = 4;
        double radius = 20;

        Game game = new Game(name, host, maxPlayerCount, riddles, coins, location, true, num_coins, radius, duration);

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MapActivity.class);
        intent.putExtra("player", host);
        intent.putExtra("host", true);
        intent.putExtra("useDB", true);

        GameDatabaseProxy gdp = new GameDatabaseProxy();

        List<Player> players = game.getPlayers();
        players.add(host);

        String id = gdp.putGame(game);

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        intent.putExtra("currentGameId", id);


        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);
            scenario.onActivity(a -> {
                a.mapView.addOnDidFinishRenderingMapListener(new MapView.OnDidFinishRenderingMapListener() {
                    @Override
                    public void onDidFinishRenderingMap(boolean fully) {
                        if (fully) {
                            finished.set(true);
                        }
                    }
                });
            });
            do {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
            } while (!finished.get());
            try {
                Thread.sleep(4000);
            } catch (Exception e) {
                assertEquals(-1, 2);
            }

            final DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
            final GameDatabaseProxy db = new GameDatabaseProxy();
            //FIRST CHECK THAT IT IS INITIALIZED WELL
            Task<DataSnapshot> dataTask = ref.child("games").child(id).get();
            dataTask.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Game fromDB = db.getGameFromTaskSnapshot(task);
                    scenario.onActivity(activity -> {
                        assertEquals(2, activity.coinsToPlace);
                        assertEquals(2, activity.getSymbolManager().getAnnotations().size());
                        assertEquals(2, activity.getLocalPlayer().getLocallyAvailableCoins().size());
                    });
                    assertEquals(2, fromDB.getCoins().size());
                    scenario.onActivity(activity -> {
                        activity.removeCoin(fromDB.getCoins().get(0), true);
                    });
                } else {
                    fail();
                }
            });
            try {
                Thread.sleep(4000);
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }

            dataTask = ref.child("games").child(id).get();
            dataTask.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Game fromDB = db.getGameFromTaskSnapshot(task);
                    assertEquals(fromDB.getCoins().size(), 1);
                } else {
                    fail();
                }
            });

        }
    }

    @Test
    public void RemovingACoinFromDBRemovesCoinFromTheMapTest() {
        Player currentUser = new Player("3212", "CURRENT_USER", 0);
        Intent toStart = new Intent(ApplicationProvider.getApplicationContext(), MapActivity.class);
        toStart.putExtra("currentUser", currentUser);

        String name = "Game";
        Player host = new Player("98934", "Bob", 0);
        int maxPlayerCount = 2;
        List<Riddle> riddles = new ArrayList<>();
        riddles.add(new Riddle("yes?", "blue", "green", "yellow", "brown", "a"));
        List<Coin> coins = new ArrayList<>();
        Location location = new Location("LocationManager#GPS_PROVIDER");
        location.setLatitude(37.42);
        location.setLongitude(-122.084);

        int num_coins = 2;
        double duration = 4;
        double radius = 20;

        Game game = new Game(name, host, maxPlayerCount, riddles, coins, location, true, num_coins, radius, duration);

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MapActivity.class);
        intent.putExtra("player", host);
        intent.putExtra("host", true);
        intent.putExtra("useDB", true);

        GameDatabaseProxy gdp = new GameDatabaseProxy();

        List<Player> players = game.getPlayers();
        players.add(host);

        String id = gdp.putGame(game);

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        intent.putExtra("currentGameId", id);

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);
            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> finished.set(true)));
            do {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
            } while (!finished.get());
            try {
                Thread.sleep(4000);
            } catch (Exception e) {
                assertEquals(-1, 2);
            }

            final DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
            final GameDatabaseProxy db = new GameDatabaseProxy();
            //FIRST CHECK THAT IT IS INITIALIZED WELL
            Task<DataSnapshot> dataTask = ref.child("games").child(id).get();
            dataTask.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Game fromDB = db.getGameFromTaskSnapshot(task);
                    assertEquals(fromDB.getCoins().size(), 2);
                    ArrayList<Coin> newCoins = new ArrayList<>();
                    newCoins.add(fromDB.getCoins().get(0));
                    fromDB.setCoins(newCoins, true);
                    db.updateGameInDatabase(fromDB, null);
                } else {
                    fail();
                }
            });

            try {
                Thread.sleep(10000);
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
            scenario.onActivity(activity -> {
                assertEquals(1, activity.getLocalPlayer().getLocallyAvailableCoins().size());
                assertEquals(1, activity.symbolManager.getAnnotations().size());
            });
        }
    }

    @Test
    public void leaderBoardWorks() {
        Player host = new Player("1234567891", "Bob", 0);
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MapActivity.class);
        intent.putExtra("player", host);
        intent.putExtra("host", true);

        GameDatabaseProxy gdp = new GameDatabaseProxy();
        Game game = getGame();
        List<Player> players = game.getPlayers();
        Player justJoined = new Player("3", "Gaston", 2);
        players.add(justJoined);
        game.setPlayers(players, false);
        String id = gdp.putGame(game);

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        intent.putExtra("currentGameId", id);

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);

            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> finished.set(true)));
            do {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
            } while (!finished.get());
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            scenario.onActivity(activity -> activity.onButtonShowLeaderboard(activity.findViewById(R.id.mapView), true, R.layout.in_game_scores));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            scenario.onActivity(activity -> {
                MapPlayerListAdapter listAdapter = activity.getLdbListAdapter();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                assertEquals(2, listAdapter.getCount());
                assert listAdapter.getItem(0).getScore() > listAdapter.getItem(1).getScore();
            });

        }
    }

    @Test
    public void mapParametersFromGame() {
        Player currentUser = new Player("3212", "CURRENT_USER", 0);
        Intent toStart = new Intent(ApplicationProvider.getApplicationContext(), MapActivity.class);
        toStart.putExtra("currentUser", currentUser);

        String name = "Game";
        Player host = new Player("98934", "Bob", 0);
        int maxPlayerCount = 2;
        List<Riddle> riddles = new ArrayList<>();
        riddles.add(new Riddle("yes?", "blue", "green", "yellow", "brown", "a"));
        List<Coin> coins = new ArrayList<>();
        Location location = new Location("LocationManager#GPS_PROVIDER");
        location.setLatitude(37.42);
        location.setLongitude(-122.084);

        int num_coins = 2;
        double duration = 4;
        double radius = 20;

        Game game = new Game(name, host, maxPlayerCount, riddles, coins, location, true, num_coins, radius, duration);

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MapActivity.class);
        intent.putExtra("player", host);
        intent.putExtra("host", true);
        intent.putExtra("useDB", true);

        GameDatabaseProxy gdp = new GameDatabaseProxy();

        List<Player> players = game.getPlayers();
        players.add(host);

        String id = gdp.putGame(game);

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        intent.putExtra("currentGameId", id);

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);

            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                if (fully) {
                    finished.set(true);
                }
            }));
            do {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
            } while (!finished.get());
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            scenario.onActivity(activity -> {
                assertEquals(2, activity.coinsToPlace);
                assertEquals(location.getLatitude(), activity.getGameCenter().getLatitude(), 0.01);
                assertEquals(location.getLongitude(), activity.getGameCenter().getLongitude(), 0.01);
                assertEquals(4 * 60, activity.getGameDuration(),0.01);
                assertEquals(20, activity.getGameRadius(), 0.001);

            });
        }
    }

    @Test
    public void checkIfCircularManagerIsInitiatedProperly() {
        Intent intent = createIntentAndPutInDB();
        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);

            scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                if (fully) {

                    a.mapView.addOnCameraDidChangeListener(animated -> a.mapView.addOnDidFinishRenderingFrameListener(fully1 -> {
                        if (fully1) {
                            finished.set(true);
                        }
                    }));
                    a.moveCameraWithoutAnimation(a.getCurrentLocation().getLatitude(), a.getCurrentLocation().getLongitude(), minZoomForBuilding);

                }
            }));
            while (true) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
                if (finished.get()) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        assertEquals(-1, 2);
                    }

                    break;
                }
            }
            scenario.onActivity(a -> assertNotNull(a.getCircleManager()));
        }
    }


    @Test
    public void checkCircleInitialized() {
        Intent intent = createIntentAndPutInDB();
        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            final AtomicBoolean finished = new AtomicBoolean(false);

            scenario.onActivity(a -> {
                a.mapView.addOnDidFinishRenderingMapListener(new MapView.OnDidFinishRenderingMapListener() {
                    @Override
                    public void onDidFinishRenderingMap(boolean fully) {
                        if (fully) {

                            a.mapView.addOnCameraDidChangeListener(new MapView.OnCameraDidChangeListener() {
                                @Override
                                public void onCameraDidChange(boolean animated) {
                                    a.mapView.addOnDidFinishRenderingFrameListener(new MapView.OnDidFinishRenderingFrameListener() {
                                        @Override
                                        public void onDidFinishRenderingFrame(boolean fully) {
                                            if (fully) {
                                                finished.set(true);
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    }
                });
            });
            while (true) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    assertEquals(-1, 2);
                }
                if (finished.get()) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        assertEquals(-1, 2);
                    }

                    break;
                }
            }
            scenario.onActivity(a -> {
                assertNotNull(a.getCircleManager());
                a.initCircle();
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                assertEquals(a.getCircleManager().getAnnotations().size(), 1);
            });
        }
    }

    @Test
    public void gameEndsWhenOutsideTheRadius(){
            Intent intent = createIntentAndPutInDB();
            try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
                Intents.init();

                final AtomicBoolean finished = new AtomicBoolean(false);

                scenario.onActivity(a -> a.mapView.addOnDidFinishRenderingMapListener(fully -> {
                    boolean c = a.checkIfLegalPosition(new Coin(90,90,2),3,0.0,0.0);
                    if (c)
                        Game.endGame(a.getLocalPlayer().getCollectedCoins().size(), a.getLocalPlayer().getScore(), a.getPlayerId(),new ArrayList<>(), a);
                    assertEquals(c,true);
                    finished.set(true);
                }));
                do {
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        assertEquals(-1, 2);
                    }
                } while (!finished.get());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                intended(hasComponent(EndGameActivity.class.getName()));
                Intents.release();
            }
        }


}