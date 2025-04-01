package com.example.mypoject1;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {
    Button btnHomeUserDetails, btnCamera, btnTimer, btnLogout;
    TextView welcomeTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        findViews();

        // Get the first name from the Intent
        Intent intent = getIntent();
        String firstName = intent.getStringExtra("firstName");
        if (firstName == null) {
            firstName = "User";
        }
        welcomeTextView.setText("Welcome " + firstName + "!");

        // Start animations
        View mainLayout = findViewById(R.id.main);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        mainLayout.startAnimation(fadeIn);
        Animation zoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
        welcomeTextView.startAnimation(zoomIn);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        btnHomeUserDetails.startAnimation(slideIn);
        btnCamera.startAnimation(slideIn);
        btnTimer.startAnimation(slideIn);
        btnLogout.startAnimation(slideIn);

        // Set click listeners
        btnHomeUserDetails.setOnClickListener(this);
        btnCamera.setOnClickListener(this);
        btnTimer.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
    }

    private void findViews() {
        btnHomeUserDetails = findViewById(R.id.btnHomeUserDetails);
        btnCamera = findViewById(R.id.btnCamera);
        btnTimer = findViewById(R.id.btnTimer);
        btnLogout = findViewById(R.id.btnLogout);
        welcomeTextView = findViewById(R.id.welcomeTextView);
    }

    @Override
    public void onClick(View view) {
        Animation buttonPress = AnimationUtils.loadAnimation(this, R.anim.button_press);
        view.startAnimation(buttonPress);
        buttonPress.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                Intent intent = null;
                if (view == btnHomeUserDetails) {
                    intent = new Intent(HomeActivity.this, UserDetailsActivity.class);
                } else if (view == btnCamera) {
                    intent = new Intent(HomeActivity.this, CameraActivity.class);
                } else if (view == btnTimer) {
                    intent = new Intent(HomeActivity.this, TimerActivity.class);
                } else if (view == btnLogout) {
                    intent = new Intent(HomeActivity.this, MainActivity.class);
                    finish();
                }
                startActivity(intent);
            }

            @Override
            public void onAnimationRepeat(Animation animation) { }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        if (item.getItemId() == R.id.menu_home) {
            intent = new Intent(this, HomeActivity.class);
        } else if (item.getItemId() == R.id.menu_logout) {
            intent = new Intent(this, MainActivity.class);
            finish();
        } else if (item.getItemId() == R.id.menu_camera) {
            intent = new Intent(this, CameraActivity.class);
        } else if (item.getItemId() == R.id.menu_timer) {
            intent = new Intent(this, TimerActivity.class);
        } else if (item.getItemId() == R.id.menu_userdetails) {
            intent = new Intent(this, UserDetailsActivity.class);
        } else if (item.getItemId() == R.id.menu_ForgotPassword) {
            intent = new Intent(this, ForgotPasswordActivity.class);
        }
        startActivity(intent);
        return super.onOptionsItemSelected(item);
    }
}
