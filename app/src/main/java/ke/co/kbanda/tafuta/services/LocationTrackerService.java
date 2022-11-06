package ke.co.kbanda.tafuta.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import ke.co.kbanda.tafuta.R;
import ke.co.kbanda.tafuta.models.LocationHistory;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class LocationTrackerService extends Service {
    private static final String TAG = "LocationTrackerService";
    public static final int REQUEST_CODE_MAPS_PERMISSIONS = 823;
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private FusedLocationProviderClient locationProviderClient;
    private DatabaseReference databaseReference;

    @Override
    public void onCreate() {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
        databaseReference.keepSynced(true); //Offline capabilities
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        locationProviderClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());

        new Thread(() -> {
            while (true) {
                getUserCurrentLocation();
                try {
                    Thread.sleep(15000); //15 seconds
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        })
                .start()
        ;

        String CHANNEL_ID = "Tracking User Location";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class)
                    .createNotificationChannel(notificationChannel);
            Notification.Builder notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentText("Updating location information...")
                    .setContentTitle("Location")
                    .setSmallIcon(R.drawable.ic_gps);

            startForeground(234, notification.build());
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @AfterPermissionGranted(REQUEST_CODE_MAPS_PERMISSIONS)
    private boolean checkLocationPermissions() {
        Log.d(TAG, "checkLocationPermissions: Checking Location permissions");
        String[] permissions = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        //Continue
        if (EasyPermissions.hasPermissions(this, permissions)) {
            Log.d(TAG, "checkLocationPermissions: Has Permission!");
            return true;
        } else {
            Log.d(TAG, "checkLocationPermissions: Permission Denied");
            return false;
        }
    }

    //Updates the location to the database
    private void updateLocationHistory(LocationHistory history) {
        Log.d(TAG, "updateLocationHistory: Updating location history");
        if (isNetworkAvailable()) {
            if (firebaseAuth.getCurrentUser() != null) {

                databaseReference
                        .child("LocationHistory")
                        .child(firebaseAuth.getCurrentUser().getUid())
                        .child(String.valueOf(System.currentTimeMillis()))
                        .setValue(history)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "updateLocationHistory: History added to firebase -> " + history);
                            } else {
                                Log.d(TAG, "updateLocationHistory: Failed -> " + history);
                            }
                        })
                ;
            } else {
                stopForeground(true); //Stop the foreground Service
                stopSelf();

                Toast.makeText(this, getString(R.string.app_name) + " needs Location permission in order to function properly.", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "updateLocationHistory: User is NULL");
            }
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

    /*
     * Check to see the user's current location
     * */
    @SuppressLint("MissingPermission")
    private void getUserCurrentLocation() {
        Log.d(TAG, "getUserCurrentLocation: Getting Current Location");
        if (checkLocationPermissions()) {
            LocationManager locationManager = (LocationManager)
                    getSystemService(Context.LOCATION_SERVICE);
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                //If/When the location service in enabled, get the last location
                locationProviderClient
                        .getLastLocation()
                        .addOnCompleteListener(
                                new OnCompleteListener<Location>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Location> task) {
                                        //Initialize location
                                        Location location = task.getResult();
                                        if (location != null) {
                                            //Display the user location on map
                                            LocationHistory history = new LocationHistory(
                                                    String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()),
                                                    String.valueOf(System.currentTimeMillis())
                                            );
                                            Log.d(TAG, "onComplete: New Location History -> " + history);
                                            updateLocationHistory(history);
                                        } else {
                                            //Initialize Location Request if location is Null
                                            LocationRequest locationRequest = new LocationRequest()
                                                    .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                                                    .setInterval(1000)
                                                    .setFastestInterval(10000)
                                                    .setNumUpdates(1);
                                            LocationCallback callback = new LocationCallback() {
                                                @Override
                                                public void onLocationResult(LocationResult locationResult) {
                                                    super.onLocationResult(locationResult);
                                                    Location location1 = locationResult.getLastLocation();
                                                    LocationHistory history = new LocationHistory(
                                                            String.valueOf(location1.getLatitude()), String.valueOf(location1.getLongitude()),
                                                            String.valueOf(System.currentTimeMillis())
                                                    );
                                                    Log.d(TAG, "onComplete: New Location History -> " + history);
                                                    updateLocationHistory(history);
                                                }
                                            };
                                            locationProviderClient
                                                    .requestLocationUpdates(
                                                            locationRequest,
                                                            callback,
                                                            Looper.myLooper()
                                                    )
                                            ;
                                        }
                                    }
                                }
                        );
            }
        } else {
            Log.d(TAG, "getUserCurrentLocation: Stopping foreground...");
            stopForeground(true);
            stopSelf();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
