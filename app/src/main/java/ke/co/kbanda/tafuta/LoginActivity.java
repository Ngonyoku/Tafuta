package ke.co.kbanda.tafuta;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private Toolbar toolbar;
    private TextInputLayout emailTIL;
    private TextInputLayout passwordlTIL;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        progressDialog = new ProgressDialog(this);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Login");
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        emailTIL = findViewById(R.id.email);
        passwordlTIL = findViewById(R.id.password);

        findViewById(R.id.btnLogin)
                .setOnClickListener(v -> {
                    validateDetails();
                });
    }

    private void validateDetails() {
        String email = emailTIL.getEditText().getText().toString();
        String password = passwordlTIL.getEditText().getText().toString();
        progressDialog.setTitle("Authenticating...");
        progressDialog.setMessage("Processing information");
        progressDialog.create();
        progressDialog.show();
        if (!email.trim().isEmpty()) {
            emailTIL.setError(null);
            if (!password.trim().isEmpty()) {
                passwordlTIL.setError(null);
                login(email, password);
            } else {
                progressDialog.dismiss();
                passwordlTIL.requestFocus();
                passwordlTIL.setError("Field cannot be empty");
            }
        } else {
            progressDialog.dismiss();
            emailTIL.requestFocus();
            emailTIL.setError("Email is required!");
        }
    }

    private void login(String email, String password) {
        firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        Log.d(TAG, "login: Successfully logged in");
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                    } else {
                        Log.d(TAG, "login: Failed to log in");
                        Toast.makeText(this, task.getException().getMessage() + "", Toast.LENGTH_LONG).show();
                    }
                })
        ;
    }
}