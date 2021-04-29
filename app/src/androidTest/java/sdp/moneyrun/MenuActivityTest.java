package sdp.moneyrun;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle.State;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sdp.moneyrun.database.GameDatabaseProxy;
import sdp.moneyrun.database.GameDbData;
import sdp.moneyrun.game.Game;
import sdp.moneyrun.game.GameRepresentation;
import sdp.moneyrun.map.Coin;
import sdp.moneyrun.map.LocationRepresentation;
import sdp.moneyrun.map.Riddle;
import sdp.moneyrun.menu.JoinGameImplementation;
import sdp.moneyrun.player.Player;
import sdp.moneyrun.ui.game.GameLobbyActivity;
import sdp.moneyrun.ui.map.MapActivity;
import sdp.moneyrun.ui.menu.LeaderboardActivity;
import sdp.moneyrun.ui.menu.MenuActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.DrawerMatchers.isClosed;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


@RunWith(AndroidJUnit4.class)
public class MenuActivityTest {

    //Since the features of Menu now depend on the intent it is usually launched with
    //We also need to launch MenuActivity with a valid intent for tests to pass
    private Intent getStartIntent() {
        Player currentUser = new Player(999, "CURRENT_USER", "Epfl"
                , 0, 0, 0);
        Intent toStart = new Intent(ApplicationProvider.getApplicationContext(), MenuActivity.class);
        toStart.putExtra("user", currentUser);
        return  toStart;
    }

    @Rule
    public ActivityScenarioRule<MenuActivity> testRule = new ActivityScenarioRule<>(getStartIntent());

    //adapted from https://stackoverflow.com/questions/28408114/how-can-to-test-by-espresso-android-widget-textview-seterror/28412476
    private static Matcher<View> withError(final String expected) {
        return new TypeSafeMatcher<View>() {

            @Override
            public boolean matchesSafely(View view) {
                if (!(view instanceof EditText)) {
                    return false;
                }
                EditText editText = (EditText) view;
                return editText.getError().toString().equals(expected);
            }

            @Override
            public void describeTo(Description description) {

            }
        };
    }

    public Game getGame(){
        String name = "JoinGameImplementationTest";
        Player host = new Player(3,"Bob", "Epfl",0,0,0);
        int maxPlayerCount = 2;
        List<Riddle> riddles = new ArrayList<>();
        riddles.add(new Riddle("yes?", "blue", "green", "yellow", "brown", "a"));
        List<Coin> coins = new ArrayList<>();
        coins.add(new Coin(0., 0., 1));
        Location location = new Location("LocationManager#GPS_PROVIDER");
        location.setLatitude(37.4219473);
        location.setLongitude(-122.0840015);
        return new Game(name, host, maxPlayerCount, riddles, coins, location, true);
    }

    @Test
    public void activityStartsProperly() {
        assertEquals(State.RESUMED, testRule.getScenario().getState());
    }

    @Test
    public void joinGamePopupIsDisplayed() {
        try (ActivityScenario<MenuActivity> scenario = ActivityScenario.launch(getStartIntent())) {
            Intents.init();

            onView(ViewMatchers.withId(R.id.join_game)).perform(ViewActions.click());
            onView(ViewMatchers.withId(R.id.join_popup)).check(matches(isDisplayed()));

            Intents.release();
        }
    }

    @Test
    public void joinGamePopupDoesntCrashAfterPlayerChange() {
        GameDatabaseProxy gdp = new GameDatabaseProxy();
        Game game = getGame();
        gdp.putGame(game);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //To get the Button ID of the button corresponding to this Game, we have
        //to get all the games in the DB, and find out how many are visible, aka
        //how many have buttons since thats how the ids are given out. Tedious but necessary.
        Task<DataSnapshot> dbGames = FirebaseDatabase.getInstance().getReference().child("games").get();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(!dbGames.isSuccessful()){
            fail();
        }
        int visibleGames = 0;
        for(DataSnapshot d : dbGames.getResult().getChildren()){
            if(d.child("isVisible").getValue(Boolean.class)){
                visibleGames += 1;
            }
        }
        List<Player> playerList = new ArrayList<>();
        playerList.add(new Player(5,"Aragon", "Epfl",0,0,0));
        try (ActivityScenario<MenuActivity> scenario = ActivityScenario.launch(getStartIntent())) {
            Intents.init();
            onView(ViewMatchers.withId(R.id.join_game)).perform(ViewActions.click());
            onView(ViewMatchers.withId(R.id.join_popup)).check(matches(isDisplayed()));
            onView(ViewMatchers.withId(visibleGames-1)).perform(ViewActions.scrollTo());
            game.setPlayers(playerList, false);
            Thread.sleep(2000);
            onView(ViewMatchers.withId(R.id.join_popup)).check(matches(isDisplayed()));
            playerList.add(new Player(6,"Heimdalr", "Epfl",0,0,0));
            game.setPlayers(playerList, false);
            Thread.sleep(2000);
            onView(ViewMatchers.withId(R.id.join_popup)).check(matches(isDisplayed()));
            Intents.release();
        }catch (Exception e) {
            fail();
        }
    }

    @Test
    public void mapButtonAndSplashScreenWorks() {
        try (ActivityScenario<MenuActivity> scenario = ActivityScenario.launch(getStartIntent())) {
            Intents.init();
            Espresso.onView(withId(R.id.map_button)).perform(ViewActions.click());
            Thread.sleep(100);

            onView(ViewMatchers.withId(R.id.splashscreen)).check(matches(isDisplayed()));
            Thread.sleep(10000);
            intended(hasComponent(MapActivity.class.getName()));

            Intents.release();
        } catch (InterruptedException e) {
            e.printStackTrace();

            Intents.release();
        }
    }

    @Test
    public void newGamePopupIsDisplayed() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MenuActivity.class);
        Player user = new Player(3,"Bob", "Epfl",0,0,0);
        intent.putExtra("user", user);

        try (ActivityScenario<MenuActivity> scenario = ActivityScenario.launch(intent)) {

            Intents.init();

            onView(ViewMatchers.withId(R.id.new_game)).perform(ViewActions.click());
            onView(ViewMatchers.withId(R.id.new_game_popup)).check(matches(isDisplayed()));

            Intents.release();
        }
    }

    @Test
    public void leaderboardButtonWorks() {
        try (ActivityScenario<MenuActivity> scenario = ActivityScenario.launch(getStartIntent())) {
            Intents.init();

            onView(withId(R.id.drawer_layout))
                    .check(matches(isClosed(Gravity.LEFT)))
                    .perform(DrawerActions.open());
            Thread.sleep(1000);
            Espresso.onView(withId(R.id.leaderboard_button)).perform(ViewActions.click());
            intended(hasComponent(LeaderboardActivity.class.getName()));

            Intents.release();
        }
        catch (Exception e){
            e.printStackTrace();
            assertEquals(-2,1);

        }
    }

    @Test
    public void navigationViewOpens() {
        try (ActivityScenario<MenuActivity> scenario = ActivityScenario.launch(getStartIntent())) {
            Intents.init();

            onView(withId(R.id.drawer_layout))
                    .check(matches(isClosed(Gravity.LEFT)))
                    .perform(DrawerActions.open());

            Intents.release();
        }
    }

    @Test
    public void newGameEmptyNameFieldError() {
        try (ActivityScenario<MenuActivity> scenario = ActivityScenario.launch(getStartIntent())) {
            Intents.init();

            onView(ViewMatchers.withId(R.id.new_game)).perform(ViewActions.click());

            Thread.sleep(1000);

            final String max_player_count = String.valueOf(12);
            final String expected = "This field is required";

            Espresso.onView(withId(R.id.newGameSubmit)).perform(ViewActions.click());
            Espresso.onView(withId(R.id.nameGameField)).check(matches(withError(expected)));

            Espresso.onView(withId(R.id.maxPlayerCountField)).perform(typeText(max_player_count), closeSoftKeyboard());
            Espresso.onView(withId(R.id.newGameSubmit)).perform(ViewActions.click());
            Espresso.onView(withId(R.id.nameGameField)).check(matches(withError(expected)));

            Intents.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Intents.release();
        }
    }

    @Test
    public void newGameEmptyMaxPlayerCountFieldError() {
        try (ActivityScenario<MenuActivity> scenario = ActivityScenario.launch(getStartIntent())) {
            Intents.init();

            onView(ViewMatchers.withId(R.id.new_game)).perform(ViewActions.click());

            Thread.sleep(1000);

            final String game_name = "new game";
            final String expected = "This field is required";

            Espresso.onView(withId(R.id.nameGameField)).perform(typeText(game_name), closeSoftKeyboard());
            Espresso.onView(withId(R.id.newGameSubmit)).perform(ViewActions.click());
            Espresso.onView(withId(R.id.maxPlayerCountField)).check(matches(withError(expected)));

            Intents.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Intents.release();
        }
    }

    @Test
    public void newGameZeroMaxPlayerCountFieldError() {
        try (ActivityScenario<MenuActivity> scenario = ActivityScenario.launch(getStartIntent())) {
            Intents.init();

            onView(ViewMatchers.withId(R.id.new_game)).perform(ViewActions.click());

            Thread.sleep(1000);

            final String game_name = "new game";
            final String max_player_count_zero = String.valueOf(0);
            final String expected_zero_players = "There should be at least one player in a game";

            Espresso.onView(withId(R.id.nameGameField)).perform(typeText(game_name), closeSoftKeyboard());
            Espresso.onView(withId(R.id.maxPlayerCountField)).perform(typeText(max_player_count_zero), closeSoftKeyboard());
            Espresso.onView(withId(R.id.newGameSubmit)).perform(ViewActions.click());
            Espresso.onView(withId(R.id.maxPlayerCountField)).check(matches(withError(expected_zero_players)));

            Intents.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Intents.release();
        }
    }

    @Test
    public void newGameWorks() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MenuActivity.class);
        Player user = new Player(3,"Bob", "Epfl",0,0,0);
        intent.putExtra("user", user);

        try (ActivityScenario<MenuActivity> scenario = ActivityScenario.launch(intent)) {
            Intents.init();

            onView(ViewMatchers.withId(R.id.new_game)).perform(ViewActions.click());

            Thread.sleep(1000);

            final String game_name = "test game";
            final String max_player_count = String.valueOf(1);

            Espresso.onView(withId(R.id.nameGameField)).perform(typeText(game_name), closeSoftKeyboard());
            Espresso.onView(withId(R.id.maxPlayerCountField)).perform(typeText(max_player_count), closeSoftKeyboard());
            Espresso.onView(withId(R.id.newGameSubmit)).perform(ViewActions.click());

            assertEquals(1, 1);

            Intents.release();

        } catch (InterruptedException e) {
            e.printStackTrace();
            Intents.release();
        }
    }

    @Test
    public void CreateGameSendsYouToLobby() {
        try (ActivityScenario<MenuActivity> scenario = ActivityScenario.launch(getStartIntent())) {
            Intents.init();

            onView(ViewMatchers.withId(R.id.new_game)).perform(ViewActions.click());

            Thread.sleep(1000);

            final String game_name = "CreateGameTest";
            final String max_player_count = String.valueOf(3);
            Espresso.onView(withId(R.id.nameGameField)).perform(typeText(game_name), closeSoftKeyboard());
            Espresso.onView(withId(R.id.maxPlayerCountField)).perform(typeText(max_player_count), closeSoftKeyboard());
            Espresso.onView(withId(R.id.newGameSubmit)).perform(ViewActions.click());
            Thread.sleep(2000);
            intended(hasComponent(GameLobbyActivity.class.getName()));
            Intents.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Intents.release();
        }
    }
}