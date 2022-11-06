package ke.co.kbanda.tafuta;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

public class Tafuta extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
