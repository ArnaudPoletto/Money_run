package sdp.moneyrun;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import sdp.moneyrun.permissions.PermissionsRequester;

public class LoginActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), map -> {
        for(String permission : map.keySet()){
            boolean isGranted = map.get(permission);
            if (isGranted) {
                System.out.println("Permission" + permission + " granted.");
            } else {
                System.out.println("Permission" + permission + " denied.");
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        String coarseLocation = Manifest.permission.ACCESS_COARSE_LOCATION;
        String fineLocation = Manifest.permission.ACCESS_FINE_LOCATION;
        PermissionsRequester locationPermissionsRequester = new PermissionsRequester(
                this,
                requestPermissionsLauncher,
                "In order to work properly, this app needs to use location services.",
                true,
                coarseLocation,
                fineLocation);
        locationPermissionsRequester.requestPermission();

        final Button loginButton = (Button) findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                EditText emailView = (EditText) findViewById(R.id.loginEmailAddress);
                EditText passwordView = (EditText)findViewById(R.id.loginPassword);
                String email = emailView.getText().toString().trim();
                String password = passwordView.getText().toString().trim();

                if(email.isEmpty()){
                    emailView.setError("Email is required");
                    emailView.requestFocus();
                    return;
                }

                if(password.isEmpty()){
                    passwordView.setError("Password is required");
                    passwordView.requestFocus();
                    return;
                }
            }
        });


    }

    // link from signup button to signup page
    public void signUp(View view) {
        Intent intent = new Intent(this, placeHolderSignUp.class);
        startActivity(intent);
    }


    private boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }


    public ActivityResultLauncher<String[]> getRequestPermissionsLauncher(){
        return requestPermissionsLauncher;
    }


}