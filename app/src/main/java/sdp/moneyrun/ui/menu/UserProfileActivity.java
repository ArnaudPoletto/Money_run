package sdp.moneyrun.ui.menu;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;
import java.util.Objects;

import sdp.moneyrun.R;
import sdp.moneyrun.database.DatabaseProxy;
import sdp.moneyrun.user.User;

public class UserProfileActivity extends AppCompatActivity {
    private final String TAG = UserProfileActivity.class.getSimpleName();
    public TextView playerName;
    public TextView playerDiedGames;
    public TextView playerPlayedGames;
    public Button goBackToMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_user_profile);
        playerName = findViewById(R.id.playerName);
        playerDiedGames = findViewById(R.id.playerDiedGames);
        playerPlayedGames = findViewById(R.id.playerPlayedGames);
        goBackToMain = findViewById(R.id.goBackToMainMenu);

        Intent playerIntent = getIntent();
        User user = (User) playerIntent.getSerializableExtra("user");

        goBackToMain.setOnClickListener(v -> {
            MediaPlayer.create(this, R.raw.button_press).start();
            Intent mainMenuIntent = new Intent(UserProfileActivity.this, MenuActivity.class);
            mainMenuIntent.putExtra("user", user);
            startActivity(mainMenuIntent);
            finish();
        });

        setDisplayedTexts(user);
        DatabaseProxy.addOfflineListener(this, TAG);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DatabaseProxy.removeOfflineListener();
    }

    public void setDisplayedTexts(@Nullable User user) {
        if (user == null) {
            playerName.setText("");
            playerDiedGames.setText("");
            playerPlayedGames.setText(R.string.profile_never_created);
        } else {
            playerName.setAllCaps(true);
            playerName.setText(user.getName());
            playerDiedGames.setText(String.format(Locale.getDefault(), "Times you died in a game \n %d", user.getNumberOfDiedGames()));
            playerPlayedGames.setText(String.format(Locale.getDefault(), "Games played \n %d", user.getNumberOfPlayedGames()));
        }
    }

    @Override
    public void onBackPressed() {
    }
}
