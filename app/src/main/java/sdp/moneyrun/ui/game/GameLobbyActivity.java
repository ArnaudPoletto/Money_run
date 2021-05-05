package sdp.moneyrun.ui.game;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import sdp.moneyrun.R;
import sdp.moneyrun.database.GameDatabaseProxy;
import sdp.moneyrun.database.PlayerDatabaseProxy;
import sdp.moneyrun.game.Game;
import sdp.moneyrun.player.Player;
import sdp.moneyrun.ui.menu.MenuActivity;


public class GameLobbyActivity extends AppCompatActivity {
    private final String TAG = GameLobbyActivity.class.getSimpleName();
    private final String DB_HOST = "host";
    private final String DB_IS_DELETED = "isDeleted";
    private final String DB_PLAYERS = "players";

    private String gameId;
    private Player user;
    private DatabaseReference thisGame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_lobby);
        gameId = (String) getIntent().getStringExtra (getResources().getString(R.string.join_game_lobby_intent_extra_id));
        user = (Player) getIntent().getSerializableExtra(getResources().getString(R.string.join_game_lobby_intent_extra_user));
        this.thisGame = FirebaseDatabase.getInstance().getReference()
                        .child(this.getString(R.string.database_games)).child(gameId);
        getGameFromDb();
    }

    private void getGameFromDb(){
        GameDatabaseProxy proxyG = new GameDatabaseProxy();
        proxyG.getGameDataSnapshot(gameId).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Game game = proxyG.getGameFromTaskSnapshot(task);
                setAllFieldsAccordingToGame(game);
                listenToIsDeleted(game);
                createDeleteOrLeaveButton(game);
            }else{
                Log.e(TAG, task.getException().getMessage());
            }
        });
    }


    private void listenToIsDeleted(Game g){
        if (!user.equals(g.getHost())) {
            thisGame.child(DB_IS_DELETED).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if ((boolean) snapshot.getValue()) {
                        g.removePlayer(user, false);
                        Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
                        intent.putExtra("user", user);
                        startActivity(intent);
                        finish();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, error.getMessage());
                }
            });
        }
    }

    private void createDeleteOrLeaveButton(Game g){
        if(user.equals(g.getHost())){
           Button leaveButton = (Button) findViewById(R.id.leave_lobby_button);
           leaveButton.setText("Delete");
           leaveButton.setOnClickListener(getDeleteClickListener(g));
        }else{
            findViewById(R.id.leave_lobby_button).setOnClickListener(getLeaveClickListener(g));
        }
    }

    private void setAllFieldsAccordingToGame(Game g){
        //Find all the views and assign them values
        TextView name = (TextView) findViewById(R.id.lobby_title);
        name.setText(g.getName());

        //Player List is dynamic with DB
        TextView playerList = (TextView) findViewById(R.id.player_list_textView);
        TextView playersMissing = (TextView) findViewById(R.id.players_missing_TextView);
        thisGame.child(DB_PLAYERS).addValueEventListener( new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GenericTypeIndicator<List<Player>> t = new GenericTypeIndicator<List<Player>>(){};
                List<Player> newPlayers = snapshot.getValue(t);
                StringBuilder str = new StringBuilder();
                String prefix = "";
                for(Player p: newPlayers){
                    str.append(prefix);
                    prefix = "\n";
                    str.append(p.getName());
                }
                playerList.setText(str.toString());
                String newPlayersMissing = "Players missing: " + Integer.toString(g.getMaxPlayerCount() - newPlayers.size());
                playersMissing.setText(newPlayersMissing);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        });
    }

    private View.OnClickListener getDeleteClickListener(Game g){
       return v -> {
            g.setIsDeleted(true, false);
            thisGame.child(DB_PLAYERS).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    GenericTypeIndicator<List<Player>> t = new GenericTypeIndicator<List<Player>>(){};
                    List<Player> players = snapshot.getValue(t);
                    if(players.size() == 1){
                        Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
                        intent.putExtra("user", user);
                        startActivity(intent);
                        finish();
                        thisGame.removeValue();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, error.getMessage());
                }
            });
       };
    }

    private View.OnClickListener getLeaveClickListener(Game g){
        return v -> {
            g.removePlayer(user,false);
            Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
            intent.putExtra("user", user);
            startActivity(intent);
            finish();
        };
    }
}