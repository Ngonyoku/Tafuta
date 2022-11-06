package ke.co.kbanda.tafuta;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

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
    private Intent fetchLocationInfoService;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressDialog = new ProgressDialog(MainActivity.this);
        membersList = new ArrayList<>();

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
        databaseReference.keepSynced(true);

        fetchLocationInfoService = new Intent(this, LocationTrackerService.class);

        fab = findViewById(R.id.fabAddMember);
        fab
                .setOnClickListener(v -> {
                    startActivity(new Intent(this, AddMemberActivity.class));
                })
        ;

//        continueLoadingViews();
        checkLocationPermissions();

//        if (currentUser == null) {
//            stopService(fetchLocationInfoService);
//            goBackToSplashscreen();
//        } else {
//            identifyUserRole();
//        }
    }

    public static final int REQUEST_CODE_MAPS_PERMISSIONS = 524;

    @AfterPermissionGranted(REQUEST_CODE_MAPS_PERMISSIONS)
    private void checkLocationPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        if (EasyPermissions.hasPermissions(this, permissions)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!isForegroundServiceRunning()) {
                    if (currentUser != null) {
                        startForegroundService(fetchLocationInfoService);
                    } else {
                        Log.d(TAG, "onCreate: User is NOT logged in!");
                    }
                }
            }
            return;
        } else {
            EasyPermissions
                    .requestPermissions(
                            this,
                            getString(R.string.app_name) + " requires this permissions in order to use this feature",
                            REQUEST_CODE_MAPS_PERMISSIONS,
                            permissions)
            ;
        }
    }

    @Override
    protected void onStart() {
        if (!isNetworkAvailable()) {
            new AlertDialog.Builder(this)
                    .setTitle("No Internet")
                    .setMessage("Please ensure that your device has been connected to the internet.")
                    .setPositiveButton("connect", (dialog, which) -> {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    })
                    .setNegativeButton("cancel", ((dialogInterface, i) -> dialogInterface.dismiss()))
                    .create()
                    .show()
            ;
//            if (currentUser == null) {
//                stopService(fetchLocationInfoService);
//            }
        }
        if (currentUser != null) {
            Log.d(TAG, "onCreate: Current User Id -> " + currentUser.getUid());
            Log.d(TAG, "onCreate: Current User Email -> " + currentUser.getEmail());
            identifyUserRole();
        } else {
            stopLocationService();
            goBackToSplashscreen();
        }
        super.onStart();
    }

    private void identifyUserRole() {
        if (isNetworkAvailable()) {
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
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("No Internet")
                    .setMessage("Please ensure that your device has been connected to the internet.")
                    .setPositiveButton("connect", (dialog, which) -> {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    })
                    .setNegativeButton("cancel", ((dialogInterface, i) -> dialogInterface.dismiss()))
                    .create()
                    .show();
        }
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
                    return;
                }
                default: {
                    goBackToMemberScreen();
                }
            }
        }
    }

    private void continueLoadingViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.app_name));

        recyclerView = findViewById(R.id.recyclerViewMembersList);
        recyclerViewAdapter = new MembersListAdapter(membersList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(recyclerViewAdapter);

        fab.setVisibility(View.VISIBLE);


        recyclerViewAdapter
                .setOnMemberClickedListener(position -> {
                    Member member = membersList.get(position);

                    Intent intent = new Intent(MainActivity.this, TrackingActivity.class);
                    intent.putExtra("member", member);
                    startActivity(intent);
                })
        ;

        fetchDataFromDatabase();
    }

    public boolean isForegroundServiceRunning() {
        ActivityManager systemService = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : systemService.getRunningServices(Integer.MAX_VALUE)) {
            if (LocationTrackerService.class.getName().equals(runningServiceInfo.service.getClassName())) {
                return true;
            }
        }
        return false;
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
            case R.id.actionMyProfile: {
                Dialog dialog = new Dialog(this);
                dialog.setContentView(R.layout.user_dialog_layout);
                TextView nameTV = dialog.findViewById(R.id.nameOfUser);
                TextView emailTV = dialog.findViewById(R.id.emailOfUser);
                ImageView profileImage = dialog.findViewById(R.id.profileImageOfUser);

                if (currentUser != null) {
                    databaseReference
                            .child("Members")
                            .child(currentUser.getUid())
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Member member = task.getResult().getValue(Member.class);
                                    if (member != null) {
                                        String name = member.getFirstName() + " " +
                                                member.getLastName();
                                        String memberMail = member.getEmail();
                                        String imageUrl = member.getImageUrl();
                                        if (!imageUrl.isEmpty() && imageUrl != null) {
                                            Glide
                                                    .with(this)
                                                    .load(imageUrl)
                                                    .centerCrop()
                                                    .placeholder(getDrawable(R.drawable.ic_user_placeholder))
                                                    .into(profileImage)
                                            ;
                                        }
                                        nameTV.setText(name);
                                        emailTV.setText(memberMail);
                                        Log.d(TAG, "onOptionsItemSelected: Value -> " + member);
                                    }
                                }
                            })
                    ;
                }

                dialog.show();
                return true;

            }
//            case R.id.actionMapView: {
//                startActivity(new Intent(this, MapsViewActivity.class));
//            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void stopLocationService() {
        if (isForegroundServiceRunning() && fetchLocationInfoService != null) {
            Log.d(TAG, "stopLocationService: Stopping Location service");
            stopService(fetchLocationInfoService);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            // Network is present and connected
            isAvailable = true;
        }
        return isAvailable;
    }

    private void logout() {
        firebaseAuth.signOut();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopLocationService();
        }
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
