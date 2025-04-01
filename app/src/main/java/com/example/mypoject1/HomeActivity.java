package com.example.mypoject1;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mypoject1.databinding.ActivityHomeBinding;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Using view binding to inflate the layout
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Retrieve firstName from the Intent and set welcome message
        String firstName = getIntent().getStringExtra("firstName");
        if (firstName == null) {
            firstName = "User";
        }
        binding.welcomeTextView.setText("Welcome " + firstName + "!");

        // Start animations for a more dynamic UI
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        binding.main.startAnimation(fadeIn);

        Animation zoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
        binding.welcomeTextView.startAnimation(zoomIn);

        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        binding.btnHomeUserDetails.startAnimation(slideIn);
        binding.btnCamera.startAnimation(slideIn);
        binding.btnTimer.startAnimation(slideIn);
        binding.btnLogout.startAnimation(slideIn);

        // Set click listeners using view binding
        binding.btnHomeUserDetails.setOnClickListener(this);
        binding.btnCamera.setOnClickListener(this);
        binding.btnTimer.setOnClickListener(this);
        binding.btnLogout.setOnClickListener(this);
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
                if (view == binding.btnHomeUserDetails) {
                    intent = new Intent(HomeActivity.this, UserDetailsActivity.class);
                } else if (view == binding.btnCamera) {
                    intent = new Intent(HomeActivity.this, CameraActivity.class);
                } else if (view == binding.btnTimer) {
                    intent = new Intent(HomeActivity.this, TimerActivity.class);
                } else if (view == binding.btnLogout) {
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
        int id = item.getItemId();
        if (id == R.id.menu_home) {
            intent = new Intent(this, HomeActivity.class);
        } else if (id == R.id.menu_logout) {
            intent = new Intent(this, MainActivity.class);
            finish();
        } else if (id == R.id.menu_camera) {
            intent = new Intent(this, CameraActivity.class);
        } else if (id == R.id.menu_timer) {
            intent = new Intent(this, TimerActivity.class);
        } else if (id == R.id.menu_userdetails) {
            intent = new Intent(this, UserDetailsActivity.class);
        } else if (id == R.id.menu_ForgotPassword) {
            intent = new Intent(this, ForgotPasswordActivity.class);
        }
        startActivity(intent);
        return super.onOptionsItemSelected(item);
    }
}
