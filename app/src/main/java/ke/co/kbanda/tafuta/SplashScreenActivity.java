package ke.co.kbanda.tafuta;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        findViewById(R.id.button)
                .setOnClickListener(v -> {
                    startActivity(new Intent(this, LoginActivity.class));
                });
    }
}