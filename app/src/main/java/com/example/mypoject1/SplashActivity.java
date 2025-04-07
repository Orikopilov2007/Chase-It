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
 * SplashActivity displays a splash screen with animation when the app starts.
 * It transitions to the MainActivity after a short delay.
 */
public class SplashActivity extends AppCompatActivity {

    // Duration the splash screen is shown (in milliseconds)
    private static final int SPLASH_DELAY = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize UI elements
        ImageView splashLogo = findViewById(R.id.splash_logo);
        ProgressBar progressBar = findViewById(R.id.progress_bar);

        // Load and apply fade-in animation to the logo
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        splashLogo.startAnimation(fadeIn);

        // Wait for SPLASH_DELAY then start MainActivity
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, SPLASH_DELAY);
    }
}
