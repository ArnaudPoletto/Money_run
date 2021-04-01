package sdp.moneyrun;

import android.content.Context;

import androidx.lifecycle.Lifecycle.State;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class MenuActivityTest {

    @Rule
    public ActivityScenarioRule<MenuActivity> testRule = new ActivityScenarioRule<>(MenuActivity.class);

    @Test
    public void activityStartsProperly() {
        assertEquals(State.RESUMED, testRule.getScenario().getState());
    }

    @Test
    public void joinGamePopupIsDisplayed() {
        Intents.init();
        onView(ViewMatchers.withId(R.id.join_game)).perform(ViewActions.click());
        onView(ViewMatchers.withId(R.id.join_popup)).check(matches(isDisplayed()));
        Intents.release();
    }
    
    @Test
    public void leaderboardButtonWorks() {
        try (ActivityScenario<MenuActivity> scenario = ActivityScenario.launch(MenuActivity.class)) {
            Intents.init();
            Espresso.onView(withId(R.id.menu_leaderboardButton)).perform(ViewActions.click());
            Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
            intended(hasComponent(LeaderboardActivity.class.getName()));
            Intents.release();
        }
    }
    
    @Test
    public void askQuestionPopupIsDisplayed() {
        Intents.init();
        onView(ViewMatchers.withId(R.id.ask_question)).perform(ViewActions.click());
        onView(ViewMatchers.withId(R.id.ask_question_popup)).check(matches(isDisplayed()));
        Intents.release();
    }

    @Test
    public void askQuestionPopupClosesWhenCorrectAnswerClicked() {
        Intents.init();
        String correctAnswer = "Oman";
        onView(ViewMatchers.withId(R.id.ask_question)).perform(ViewActions.click());
        onView(ViewMatchers.withText(correctAnswer)).perform(ViewActions.click());
        onView(ViewMatchers.withId(R.id.ask_question_popup)).check(doesNotExist());
        Intents.release();

    }

    @Test
    public void logOutButtonWorks() throws InterruptedException {
            Espresso.onView(withId(R.id.log_out_button)).perform(ViewActions.click());
            Thread.sleep(1000);
            assertEquals(State.DESTROYED, testRule.getScenario().getState());
    }

    @Test
    public void logOutButtonLogsOut() throws InterruptedException {
        assertNotNull(FirebaseAuth.getInstance().getCurrentUser());
        Espresso.onView(withId(R.id.log_out_button)).perform(ViewActions.click());
        Thread.sleep(1000);
        assertEquals(FirebaseAuth.getInstance().getCurrentUser(), null);
    }



}