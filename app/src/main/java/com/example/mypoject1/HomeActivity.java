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


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        findViews();

        // Get the first name from the Intent
        Intent intent = getIntent();
        String firstName = intent.getStringExtra("firstName"); // Retrieve the first name passed from SignupActivity

        if (firstName == null) {
            firstName = "User"; // Default to "User" if no first name is passed
        }
        // Update the welcome text
        welcomeTextView.setText("Welcome " + firstName + "!");

        // Start initial animations for the main layout and welcome text
        View mainLayout = findViewById(R.id.main);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        mainLayout.startAnimation(fadeIn);

        // Additional zoom-in animation on welcome text
        Animation zoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
        welcomeTextView.startAnimation(zoomIn);

        // Animate the buttons with slide_in effect
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
        // Load the button press animation defined in res/anim/button_press.xml
        Animation buttonPress = AnimationUtils.loadAnimation(this, R.anim.button_press);
        view.startAnimation(buttonPress);

        // Set an AnimationListener so that the navigation happens after the animation finishes
        buttonPress.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Not used
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Intent intent = null;
                if (view == btnHomeUserDetails) {
                    intent = new Intent(HomeActivity.this, com.example.mypoject1.UserDetailsActivity.class);
                } else if (view == btnCamera) {
                    intent = new Intent(HomeActivity.this, CameraActivity.class);
                } else if (view == btnTimer) {
                    intent = new Intent(HomeActivity.this, TimerActivity.class);
                } else if (view == btnLogout) {
                    intent = new Intent(HomeActivity.this, MainActivity.class);
                    finish(); // Optionally finish current activity after logout
                }
                startActivity(intent);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Not used
            }
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
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_logout) {
            intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (item.getItemId() == R.id.menu_camera) {
            intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_timer) {
            intent = new Intent(this, TimerActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_userdetails) {
            intent = new Intent(this, com.example.mypoject1.UserDetailsActivity.class);
            startActivity(intent);
        }
        else if (item.getItemId() == R.id.menu_ForgotPassword) {
            intent = new Intent(this, com.example.mypoject1.ForgotPasswordActivity.class);
            startActivity(intent);
        }

        /*
       else if (item.getItemId() == R.id.menu_ChatBot) {
           Intent intent = new Intent(this, com.example.mypoject1.ChatbotActivity.class);
           startActivity(intent);
       }
       */

        startActivity(intent);

        return super.onOptionsItemSelected(item);
    }
}
