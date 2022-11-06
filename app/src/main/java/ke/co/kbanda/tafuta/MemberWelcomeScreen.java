package ke.co.kbanda.tafuta;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import ke.co.kbanda.tafuta.models.Member;

public class MemberWelcomeScreen extends AppCompatActivity {
    private static final String TAG = "MemberWelcomeScreen";
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private FirebaseUser currentUser;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_welcome_screen);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();

//        if (currentUser == null) {
//            goBackToSplashscreen();
//            finish();
//        } else {
//            identifyUserRole();
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.member_welcome_screen_toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionAdminPanel: {
                new AlertDialog.Builder(this)
                        .setTitle("ALERT")
                        .setMessage("You will NEED to log back in inordwe to access the admin portal.")
                        .setPositiveButton("Proceed", ((dialogInterface, i) -> {
                            firebaseAuth.signOut();
                            Log.d(TAG, "onOptionsItemSelected: Signed out..");
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        }))
                        .setNegativeButton("cancel", ((dialogInterface, i) -> dialogInterface.dismiss()))
                        .create()
                        .show();

            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    //    private void identifyUserRole() {
//        firebaseDatabase
//                .getReference()
//                .child("Members")
//                .child(currentUser.getUid())
//                .get()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        Member member = task.getResult().getValue(Member.class);
//                        final String role = member.getRole();
//                        Log.d(TAG, "identifyUserRole: Member Role -> " + role);
//                        evaluateUserRole(role);
//                        Log.d(TAG, "identifyUserRole: Task Successfull");
//                    }
//                })
//        ;
//    }

//    private void evaluateUserRole(String role) {
//        if (role != null) {
//            switch (role) {
//                case "ADMIN": {
//                    //Do nothing
//                    goBackToMainScreen();
//                    return;
//                }
//                case "MEMBER": {
//                    //Take to member dashboard
//
//                }
//                default: {
//
//                }
//            }
//        }
//    }

    private void goBackToMainScreen() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void goBackToSplashscreen() {
        Intent intent = new Intent(this, SplashScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}