package com.example.mypoject1;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    // Duration the splash screen is shown (in milliseconds)
    private static final int SPLASH_DELAY = 3000;
    // How often to update the progress bar (in milliseconds)
    private static final int UPDATE_INTERVAL = 100;

    private ProgressBar progressBar;
    private int progressStatus = 0;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize UI elements
        ImageView splashLogo = findViewById(R.id.splash_logo);
        progressBar = findViewById(R.id.progress_bar);

        // Load and apply fade-in animation to the logo
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        splashLogo.startAnimation(fadeIn);

        // Set maximum progress value
        progressBar.setMax(100);
        progressStatus = 0;

        // Create a background thread to update the progress bar
        new Thread(() -> {
            // Calculate the number of updates needed based on the SPLASH_DELAY and UPDATE_INTERVAL
            int updatesCount = SPLASH_DELAY / UPDATE_INTERVAL;
            // Increment value for each update (100 / updatesCount)
            int increment = 100 / updatesCount;

            while (progressStatus < 100) {
                progressStatus += increment;
                // Update the progress bar on the UI thread
                handler.post(() -> progressBar.setProgress(progressStatus));
                try {
                    Thread.sleep(UPDATE_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // Wait for SPLASH_DELAY then start MainActivity
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, SPLASH_DELAY);
    }
}
