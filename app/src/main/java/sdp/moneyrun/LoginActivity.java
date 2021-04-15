package sdp.moneyrun;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import sdp.moneyrun.permissions.PermissionsRequester;

public class LoginActivity extends AppCompatActivity {

    private final String TAG = LoginActivity.class.getSimpleName();
    private FirebaseAuth mAuth;

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), map -> {});
    private final String coarseLocation = Manifest.permission.ACCESS_COARSE_LOCATION;
    private final String fineLocation = Manifest.permission.ACCESS_FINE_LOCATION;

    private Button login;

    private final String ERROR_MISSING_EMAIL = "Email is required";
    private final String ERROR_MISSING_PASSWORD = "Password is required";
    private final String ERROR_INVALID_EMAIL_FORMAT = "Email format is invalid";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        PermissionsRequester locationPermissionsRequester = new PermissionsRequester(
                this,
                requestPermissionsLauncher,
                "In order to work properly, this app needs to use location services.",
                true,
                coarseLocation,
                fineLocation);
        locationPermissionsRequester.requestPermission();
        mAuth = FirebaseAuth.getInstance();
        final Button loginButton = (Button) findViewById(R.id.loginButton);
        setLogIn(loginButton);


    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
           updateUI(currentUser);
        }
    }

    /**
     * This is needed for testing
     * Also, since it's one of the first activity created, it's reasonable to assume that when it's
     * destroyed the user should be signed out
     */
    @Override
    public void onDestroy(){
        super.onDestroy();
        mAuth.signOut();
    }

    // link from signUp button to signUp page
    public void signUp(View view) {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
    }

    private void setLogIn(Button loginButton) {
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                EditText emailView = (EditText) findViewById(R.id.loginEmailAddress);
                EditText passwordView = (EditText) findViewById(R.id.loginPassword);
                String email = emailView.getText().toString().trim();
                String password = passwordView.getText().toString().trim();

                if (email.isEmpty()) {
                    emailView.setError(ERROR_MISSING_EMAIL);
                    emailView.requestFocus();

                } else if (password.isEmpty()) {
                    passwordView.setError(ERROR_MISSING_PASSWORD);
                    passwordView.requestFocus();
                }
                else if (!isEmailValid(email)){
                    emailView.setError(ERROR_INVALID_EMAIL_FORMAT);
                    emailView.requestFocus();
                }
                else{
                    submitLogin(email, password);


                }
            }
        });

    }

    private void submitLogin(String email, String password){
        mAuth.signInWithEmailAndPassword(email, password)
         .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success");
                    FirebaseUser user = mAuth.getCurrentUser();
                    updateUI(user);
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                    Toast.makeText(LoginActivity.this, "Authentication failed.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Intent menuIntent = new Intent(LoginActivity.this, MenuActivity.class);
            getPlayerFromDB(user.getUid().hashCode(),menuIntent);
        }
    }
    private void getPlayerFromDB(int playerID,Intent menuIntent){
        DatabaseProxy db = new DatabaseProxy();
        Task t = db.getPlayerTask(playerID);
        t.addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                if(task.isSuccessful()) {
                    menuIntent.putExtra("playerId", playerID);
                    Player p = db.getPlayerFromTask(task);
                    String[] info = {p.getName(), p.getAddress(), "" + p.getNumberOfDiedGames(), "" + p.getNumberOfPlayedGames()};
                    menuIntent.putExtra("playerId" + playerID, info);
                    System.out.println("DOING ASYNCHROUSNOUS DATA TRANSFER of player with name " + p.getName());
                    startActivity(menuIntent);
                }
            }
        });

    }

    private boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    public ActivityResultLauncher<String[]> getRequestPermissionsLauncher() {
        return requestPermissionsLauncher;
    }


}