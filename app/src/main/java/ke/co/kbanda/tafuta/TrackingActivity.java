package ke.co.kbanda.tafuta;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import ke.co.kbanda.tafuta.adapters.LocationHistoryRecyclerViewAdapter;
import ke.co.kbanda.tafuta.databinding.ActivityTrackingBinding;
import ke.co.kbanda.tafuta.models.LocationHistory;
import ke.co.kbanda.tafuta.models.Member;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class TrackingActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "TrackingActivity";
    public static final int REQUEST_CODE_MAPS_PERMISSIONS = 524;
    private GoogleMap map;
    private ActivityTrackingBinding binding;
    private Member member = null;
    private List<LocationHistory> locationHistories;

    private FusedLocationProviderClient locationProviderClient;
    private Toolbar toolbar;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityTrackingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        locationHistories = new ArrayList<>();

        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        databaseReference = firebaseDatabase.getReference();

        CircleImageView profileImage = binding.profileImage;

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (getIntent().getExtras() != null) {
            member = (Member) getIntent().getSerializableExtra("member");
            if (member != null) {
                String name = member.getFirstName() + " " + member.getLastName();
                getSupportActionBar().setTitle(name);
                if (member.getImageUrl() != null && !member.getImageUrl().isEmpty()) {
                    Glide
                            .with(this)
                            .load(member.getImageUrl())
                            .centerCrop()
                            .into(profileImage)
                    ;
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tracking_activity_toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionAbout: {
                displayUserInformation();
                return true;
            }
            case R.id.actionGetCurrentLocation: {
                getMostRecentLocation();
                return true;
            }
            case R.id.actionRefreshMap: {
                displayLocationHistory();
                return true;
            }
            case R.id.actionMakeAdmin: {
                promoteToAdmin();
            }
            case R.id.actionRemoveMember: {
                new AlertDialog.Builder(this)
                        .setTitle("Delete Member")
                        .setMessage("Are you sure you want to delete this member?")
                        .setPositiveButton("Yes", ((dialogInterface, i) -> {
                            deleteMemberFromDatabase();
                        }))
                        .setNegativeButton("cancel", ((dialogInterface, i) -> {
                            dialogInterface.dismiss();
                        }))
                        .create()
                        .show()
                ;
            }
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void deleteMemberFromDatabase() {
        if (currentUser != null) {
            firebaseDatabase
                    .getReference()
                    .child("LocationHistory")
                    .child(member.getUserId())
                    .removeValue()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Location History wiped!", Toast.LENGTH_SHORT).show();
                            firebaseDatabase
                                    .getReference()
                                    .child("Members")
                                    .child(member.getUserId())
                                    .removeValue()
                                    .addOnCompleteListener(t -> {
                                        if (t.isSuccessful()) {
                                            Toast.makeText(this, "Member Deleted successfully", Toast.LENGTH_SHORT).show();
                                            finish();
                                        } else {
                                            new AlertDialog.Builder(this)
                                                    .setTitle("Failed")
                                                    .setMessage("Failed to delete user.\n" + t.getException().getMessage())
                                                    .create()
                                                    .show();
                                        }
                                    })
                            ;
                        } else {
                            new AlertDialog.Builder(this)
                                    .setTitle("Failed")
                                    .setMessage("Failed to delete user.\n" + task.getException().getMessage())
                                    .create()
                                    .show();
                        }
                    })
            ;
        }
    }

    private void promoteToAdmin() {
        if (member != null) {
            if (member.getUserId() != null && !member.getUserId().isEmpty()) {
                firebaseDatabase
                        .getReference()
                        .child("Members")
                        .child(member.getUserId())
                        .child("role")
                        .setValue("ADMIN")
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "promoteToAdmin: Successfully promoted to admin");
                                Toast.makeText(this, "Successfully promoted", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d(TAG, "promoteToAdmin: Failed to promote to admin -> " + task.getException().getMessage());
                                Toast.makeText(this, "Successfully promoted", Toast.LENGTH_SHORT).show();
                            }
                        })
                ;
            }
        }
    }

    private void getMostRecentLocation() {
        if (locationHistories != null) {
            if (!locationHistories.isEmpty()) {
                LocationHistory history = locationHistories.get(locationHistories.size() - 1);
                Timestamp timestamp = getTimestamp(history);
                LatLng latLng = getLatLng(history);

                map.addMarker(
                        new MarkerOptions()
                                .title("Last known location")
                                .position(latLng)
                                .snippet(timestamp.toString())
                );
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
            }
        }
    }

    private void displayUserInformation() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View bottomSheetView = LayoutInflater
                .from(getApplicationContext())
                .inflate(R.layout.bottom_sheet_user_info,
                        (ConstraintLayout) findViewById(R.id.bottomSheetContainer)
                );
        TextView name = bottomSheetView.findViewById(R.id.displayName);
        TextView email = bottomSheetView.findViewById(R.id.email);
        TextView label = bottomSheetView.findViewById(R.id.label);
        ImageView profileImage = bottomSheetView.findViewById(R.id.profileImageImageView);
        RecyclerView locationHistoryRecyclerView = bottomSheetView.findViewById(R.id.locationHistoryRecyclerView);
        LocationHistoryRecyclerViewAdapter recyclerViewAdapter = new LocationHistoryRecyclerViewAdapter(TrackingActivity.this, locationHistories);
        locationHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true));
        locationHistoryRecyclerView.setHasFixedSize(true);
        locationHistoryRecyclerView.setAdapter(recyclerViewAdapter);

        if (member != null) {
            String memberName = member.getFirstName() + " " + member.getLastName();
            name.setText(memberName);
            email.setText(member.getEmail());
            label.setText(member.getLabel());
            String imageUrl = member.getImageUrl();
            if (imageUrl != null) {
                Glide
                        .with(this)
                        .load(imageUrl)
                        .centerCrop()
                        .into(profileImage)
                ;
            }
        }

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        Log.d(TAG, "onMapReady: Map is ready...");
        checkLocationPermissions();
    }

    @AfterPermissionGranted(REQUEST_CODE_MAPS_PERMISSIONS)
    private void checkLocationPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        if (EasyPermissions.hasPermissions(this, permissions)) {
            displayLocationHistory();
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

    private void displayLocationHistory() {
        Log.d(TAG, "displayLocationHistory: Displaying Location History...");
        if (map != null) {
            if (member != null) {
                String userId = member.getUserId();
                if (userId != null && !userId.isEmpty()) {
                    Log.d(TAG, "displayLocationHistory: Fetching data from database...");
                    databaseReference
                            .child("LocationHistory")
                            .child(userId)
                            .orderByChild("timestamp")
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    Log.d(TAG, "onDataChange: fetching location histories..");
                                    locationHistories.clear();
                                    Log.d(TAG, "onDataChange: Location Histories -> " + locationHistories.toString());
                                    for (DataSnapshot historySnapshot : snapshot.getChildren()) {
                                        Log.d(TAG, "onDataChange: HistorySnapshot -> " + historySnapshot);
                                        LocationHistory locationHistory = historySnapshot.getValue(LocationHistory.class);
                                        locationHistories.add(locationHistory);
                                        Log.d(TAG, "onDataChange: Location Histories -> " + locationHistories.toString());
                                        Log.d(TAG, "onChildAdded: LocationHistory -> " + locationHistory);
                                    }
                                    drawPolylinesOnMap();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.d(TAG, "onCancelled: Error " + error.getMessage());
                                }
                            })
                    ;
                }
            } else {
                Log.d(TAG, "displayLocationHistory: Member is NULL..");
            }
        } else {
            Log.d(TAG, "displayLocationHistory: Map is NULL !...");
        }
    }

    private void drawPolylinesOnMap() {
        Log.d(TAG, "drawPolylinesOnMap: Drawing Polyines");
        if (locationHistories != null) {
            if (!locationHistories.isEmpty()) {
                PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions.clickable(true);

                for (int i = 0; i < locationHistories.size(); i++) {
                    LocationHistory history = locationHistories.get(i);
                    if (history != null) {
                        LatLng latLng = getLatLng(history);
                        polylineOptions.add(latLng);
                        Log.d(TAG, "drawPolylinesOnMap: history -> " + history.toString());

                        if (i + 1 == locationHistories.size()) {
                            final Timestamp timestamp = getTimestamp(history);
                            //Add Marker
                            map.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(timestamp.toString())
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))

                            );
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
                        } else {
                            final Timestamp timestamp = getTimestamp(history);
                            //Add Marker
                            map.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(timestamp.toString())
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))

                            );
                        }
                    }
                }

//                map.addPolyline(
//                        polylineOptions
//                );
            }
        } else {
            Log.d(TAG, "drawPolylinesOnMap: LocationHistories is NULL!");
        }
    }

    @NonNull
    private LatLng getLatLng(LocationHistory history) {
        double latitude = Double.parseDouble(history.getLatitude());
        double longitude = Double.parseDouble(history.getLongitude());
        LatLng latLng = new LatLng(latitude, longitude);
        return latLng;
    }

    @NonNull
    private Timestamp getTimestamp(LocationHistory history) {
        //Time
        final long time = Long.parseLong(history.getTimeInMillis());
        final Timestamp timestamp = new Timestamp(time);
        return timestamp;
    }
}