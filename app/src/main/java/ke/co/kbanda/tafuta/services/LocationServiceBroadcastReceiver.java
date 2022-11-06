package ke.co.kbanda.tafuta.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import ke.co.kbanda.tafuta.services.LocationTrackerService;

public class LocationServiceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "LocationServiceBroadcas";
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;

    @Override
    public void onReceive(Context context, Intent intent) {
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            if (currentUser != null) {
                Intent serviceIntent = new Intent(context, LocationTrackerService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                    Log.d(TAG, "onReceive: Foreground Service launched");
                } else {
                    context.stopService(serviceIntent);
                }
            }
        }
    }
}
