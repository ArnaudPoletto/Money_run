package sdp.moneyrun.game;

import androidx.lifecycle.Lifecycle;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.database.FirebaseDatabase;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import sdp.moneyrun.R;
import sdp.moneyrun.ui.MainActivity;
import sdp.moneyrun.ui.game.GameLobbyActivity;
import sdp.moneyrun.ui.menu.MenuActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class GameLobbyActivityInstrumentedTest {

    @BeforeClass
    public static void setPersistence(){
        if(!MainActivity.calledAlready){
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            MainActivity.calledAlready = true;
        }
    }

    @Rule
    public ActivityScenarioRule<GameLobbyActivity> testRule = new ActivityScenarioRule<>(GameLobbyActivity.class);

    @Test
    public void activityStartsProperly() {
        assertEquals(Lifecycle.State.RESUMED, testRule.getScenario().getState());
    }


    @Test
    public void LeaveLobbyWorks() {
        try {
            Intents.init();
            onView(ViewMatchers.withId(R.id.leave_lobby_button)).perform(ViewActions.click());
            Thread.sleep(4000);
            intended(hasComponent(MenuActivity.class.getName()));
            Intents.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Intents.release();
        }
    }
}
