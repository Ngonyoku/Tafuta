package ke.co.kbanda.tafuta;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import ke.co.kbanda.tafuta.adapters.MembersListAdapter;
import ke.co.kbanda.tafuta.models.Member;
import ke.co.kbanda.tafuta.services.LocationTrackerService;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private RecyclerView recyclerView;
    private MembersListAdapter recyclerViewAdapter;
    private List<Member> membersList;
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private ProgressDialog progressDialog;
    private Toolbar toolbar;

    private FirebaseUser currentUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressDialog = new ProgressDialog(MainActivity.this);
        membersList = new ArrayList<>();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.app_name));
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();

        if (currentUser == null) {
            goBackToSplashscreen();
            finish();
        } else {
            identifyUserRole();
        }
    }

    private void continueLoadingViews() {
        recyclerView = findViewById(R.id.recyclerViewMembersList);
        recyclerViewAdapter = new MembersListAdapter(membersList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(recyclerViewAdapter);

        recyclerViewAdapter
                .setOnMemberClickedListener(position -> {
                    Member member = membersList.get(position);

                    Intent intent = new Intent(MainActivity.this, TrackingActivity.class);
                    intent.putExtra("member", member);
                    startActivity(intent);
                })
        ;

        findViewById(R.id.fabAddMember)
                .setOnClickListener(v -> {
                    startActivity(new Intent(this, AddMemberActivity.class));
                })
        ;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!foregroundServiceRunning()) {
                startForegroundService(new Intent(this, LocationTrackerService.class));
            }

        }
        fetchDataFromDatabase();
    }

    public boolean foregroundServiceRunning() {
        ActivityManager systemService = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : systemService.getRunningServices(Integer.MAX_VALUE)) {
            if (LocationTrackerService.class.getName().equals(runningServiceInfo.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    private void identifyUserRole() {
        firebaseDatabase
                .getReference()
                .child("Members")
                .child(currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Member member = task.getResult().getValue(Member.class);
                        final String role = member.getRole();
                        Log.d(TAG, "identifyUserRole: Member Role -> " + role);
                        evaluateUserRole(role);
                        Log.d(TAG, "identifyUserRole: Task Successfull");
                    }
                })
        ;
    }

    private void evaluateUserRole(String role) {
        if (role != null) {
            switch (role) {
                case "ADMIN": {
                    //Do nothing
                    continueLoadingViews();
                    return;
                }
                case "MEMBER": {
                    //Take to member dashboard
                    goBackToMemberScreen();
                }
                default: {

                }
            }
        }
    }

    @Override
    protected void onResume() {
        if (currentUser == null) {
            goBackToSplashscreen();
            finish();
        } else {
            identifyUserRole();
        }
        super.onResume();
    }

    private void goBackToMemberScreen() {
        Intent intent = new Intent(this, MemberWelcomeScreen.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        progressDialog.dismiss();
        finish();
    }

    private void goBackToSplashscreen() {
        Intent intent = new Intent(this, SplashScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        progressDialog.dismiss();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionLogout: {
                new AlertDialog.Builder(this)
                        .setTitle("Log out")
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Yes", ((dialogInterface, i) -> {
                            logout();
                        }))
                        .setNegativeButton("No", ((dialogInterface, i) -> {
                            dialogInterface.dismiss();
                        }))
                        .create()
                        .show();
            }
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void logout() {
        firebaseAuth.signOut();
        goBackToSplashscreen();
    }

    private void fetchDataFromDatabase() {
        progressDialog.setMessage("Loading...");
        progressDialog.show();
        databaseReference
                .child("Members")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressDialog.dismiss();
                        membersList.clear();
                        for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                            Member member = memberSnapshot.getValue(Member.class);
                            Log.d(TAG, "onDataChange: New Member -> " + member);
                            membersList.add(member);
                            recyclerViewAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onCancelled: Fetch cancelled -> " + error.getMessage());
                    }
                })
        ;
    }
}
