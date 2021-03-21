package sdp.moneyrun;
import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static sdp.moneyrun.SignUpActivityTest.withError;

@RunWith(AndroidJUnit4.class)
public class RegisterPlayerInstrumentedTest {
    @Test
    public void checkViewsAreUpdatedWhenCorrectTyping(){
        try(ActivityScenario<RegisterPlayerActivity> scenario = ActivityScenario.launch(RegisterPlayerActivity.class)){
            Intents.init();
            String name = "John";
            String address = "New York";
            String pet = "Dog";
            String color = "Green";
            Espresso.onView(withId(R.id.registerNameText)).perform(typeText(name), closeSoftKeyboard());
            Espresso.onView(withId(R.id.registerAddressText)).perform(typeText(address), closeSoftKeyboard());
            Espresso.onView(withId(R.id.registerAnimalText)).perform(typeText(pet), closeSoftKeyboard());
            Espresso.onView(withId(R.id.registerColorText)).perform(typeText(color), closeSoftKeyboard());
            Espresso.onView(withId(R.id.registerNameText)).check(matches(withText(name)));
            Espresso.onView(withId(R.id.registerAddressText)).check(matches(withText(address)));
            Espresso.onView(withId(R.id.registerAnimalText)).check(matches(withText(pet)));
            Espresso.onView(withId(R.id.registerColorText)).check(matches(withText(color)));
            Espresso.onView(withId(R.id.submitProfileButton)).perform(click());
            Intents.release();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @Test
    public void checkErrorWhenNameIsEmpty() {
        try (ActivityScenario<RegisterPlayerActivity> scenario = ActivityScenario.launch(RegisterPlayerActivity.class)) {
            Intents.init();
            final String expected = "Name field is empty";
            Espresso.onView(withId(R.id.submitProfileButton)).perform(ViewActions.click());
            Espresso.onView(withId(R.id.registerNameText)).check(matches(withError(expected)));
            Intents.release();
        }
    }
    @Test
    public void checkErrorWhenAddressIsEmpty(){
            try(ActivityScenario<RegisterPlayerActivity> scenario = ActivityScenario.launch(RegisterPlayerActivity.class)){
                Intents.init();
                final String expected = "Address field is empty";
                Espresso.onView(withId(R.id.submitProfileButton)).perform(ViewActions.click());
                Espresso.onView(withId(R.id.registerAddressText)).check(matches(withError(expected)));
                Intents.release();
            }
    }
    @Test
    public void checkErrorWhenAnimalIsEmpty(){
        try(ActivityScenario<RegisterPlayerActivity> scenario = ActivityScenario.launch(RegisterPlayerActivity.class)){
            Intents.init();
            final String expected = "Animal field is empty";
            Espresso.onView(withId(R.id.submitProfileButton)).perform(ViewActions.click());
            Espresso.onView(withId(R.id.registerAnimalText)).check(matches(withError(expected)));
            Intents.release();
        }
    }
    @Test
    public void checkErrorWhenColorIsEmpty(){
        try(ActivityScenario<RegisterPlayerActivity> scenario = ActivityScenario.launch(RegisterPlayerActivity.class)){
            Intents.init();
            final String expected = "Color field is empty";
            Espresso.onView(withId(R.id.submitProfileButton)).perform(ViewActions.click());
            Espresso.onView(withId(R.id.registerColorText)).check(matches(withError(expected)));
            Intents.release();
        }
    }
    @Test
    public void checkProfileHasCorrectAttributesWhenPlayerRegisters(){
        try(ActivityScenario<RegisterPlayerActivity> scenario = ActivityScenario.launch(RegisterPlayerActivity.class)) {
            Intents.init();
            String name = "John";
            String address = "New York";
            String pet = "Dog";
            String color = "Green";
            Espresso.onView(withId(R.id.registerNameText)).perform(typeText(name), closeSoftKeyboard());
            Espresso.onView(withId(R.id.registerAddressText)).perform(typeText(address), closeSoftKeyboard());
            Espresso.onView(withId(R.id.registerAnimalText)).perform(typeText(pet), closeSoftKeyboard());
            Espresso.onView(withId(R.id.registerColorText)).perform(typeText(color), closeSoftKeyboard());
            Espresso.onView(withId(R.id.submitProfileButton)).perform(click());
            Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
            Thread.sleep(1000);
            intended(hasComponent(MenuActivity.class.getName()));
            Espresso.onView(withId(R.id.go_to_profile_button)).perform(click());
            Context appContext2 = InstrumentationRegistry.getInstrumentation().getTargetContext();
            Thread.sleep(1000);
            intended(hasComponent(PlayerProfileActivity.class.getName()));
            Espresso.onView(withId(R.id.playerDiedGames))
                    .check(matches(withText("Player has died Green many times")));
            Espresso.onView(withId(R.id.playerPlayedGames))
                    .check(matches(withText("Player has played Dog many games")));
            Espresso.onView(withId(R.id.playerAddress))
                    .check(matches(withText("Player address : New York")));
            Espresso.onView(withId(R.id.playerName))
                    .check(matches(withText("Player name : John")));
            Intents.release();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
