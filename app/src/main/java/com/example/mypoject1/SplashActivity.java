package com.example.mypoject1;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

/**
 * SplashActivity displays a splash screen with a fade-in animation for the app logo
 * and a progress bar that fills over a fixed duration. Once the splash screen duration
 * is complete, it navigates to the MainActivity.
 */
public class SplashActivity extends AppCompatActivity {

    // Duration for which the splash screen is shown (in milliseconds)
    private static final int SPLASH_DELAY = 3000;
    // Interval at which the progress bar is updated (in milliseconds)
    private static final int UPDATE_INTERVAL = 100;

    // UI element to display the progress
    private ProgressBar progressBar;
    // Variable to hold the current progress status (0 to 100)
    private int progressStatus = 0;
    // Handler to post updates to the UI thread
    private Handler handler = new Handler();

    /**
     * Called when the activity is first created.
     * <p>
     * This method sets up the splash screen layout, starts the fade-in animation on the logo,
     * and initiates a background thread that updates the progress bar periodically. Once the
     * splash screen delay is reached, the app navigates to MainActivity.
     * </p>
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the splash screen layout
        setContentView(R.layout.activity_splash);

        // Initialize UI elements: splash logo and progress bar.
        ImageView splashLogo = findViewById(R.id.splash_logo);
        progressBar = findViewById(R.id.progress_bar);

        // Load the fade-in animation from the resources and apply it to the splash logo.
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        splashLogo.startAnimation(fadeIn);

        // Set the maximum value for the progress bar and reset the progress status.
        progressBar.setMax(100);
        progressStatus = 0;

        // Start a new background thread to update the progress bar over time.
        new Thread(() -> {
            // Calculate the total number of progress updates based on SPLASH_DELAY and UPDATE_INTERVAL.
            int updatesCount = SPLASH_DELAY / UPDATE_INTERVAL;
            // Calculate the amount by which to increment the progress bar for each update.
            int increment = 100 / updatesCount;

            // Loop until the progress status reaches or exceeds 100.
            while (progressStatus < 100) {
                // Increase the progress status by the calculated increment.
                progressStatus += increment;

                // Post the progress update to the UI thread.
                handler.post(() -> progressBar.setProgress(progressStatus));

                try {
                    // Pause the thread for the update interval.
                    Thread.sleep(UPDATE_INTERVAL);
                } catch (InterruptedException e) {
                    // Print the stack trace if the thread is interrupted.
                    e.printStackTrace();
                }
            }
        }).start();

        // Schedule a delayed task to transition to MainActivity after SPLASH_DELAY milliseconds.
        new Handler().postDelayed(() -> {
            // Start MainActivity.
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            // Close the SplashActivity so that the user cannot return to it.
            finish();
        }, SPLASH_DELAY);
    }
}
