package com.example.mypoject1;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.content.SharedPreferences;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {
    Button btnHomeUserDetails, btnCamera, btnTimer, btnLogout;



    @Override
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
        TextView welcomeTextView = findViewById(R.id.welcomeTextView);
        welcomeTextView.setText("Welcome " + firstName + "!");

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
    }

    @Override
    public void onClick(View view) {
        if (view == btnHomeUserDetails) {
            Intent intent = new Intent(this, com.example.mypoject1.UserDetailsActivity.class);
            startActivity(intent);
        } else if (view == btnCamera) {
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        } else if (view == btnTimer) {
            Intent intent = new Intent(this, TimerActivity.class);
            startActivity(intent);
        } else if (view == btnLogout) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish(); // Close this activity after logout
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_home) {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_logout) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (item.getItemId() == R.id.menu_camera) {
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_timer) {
            Intent intent = new Intent(this, TimerActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_userdetails) {
            Intent intent = new Intent(this, com.example.mypoject1.UserDetailsActivity.class);
            startActivity(intent);
        }
        else if (item.getItemId() == R.id.menu_ForgotPassword) {
            Intent intent = new Intent(this, com.example.mypoject1.ForgotPasswordActivity.class);
            startActivity(intent);
        }
//        else if (item.getItemId() == R.id.menu_ChatBot) {
//            Intent intent = new Intent(this, com.example.mypoject1.ChatbotActivity.class);
//            startActivity(intent);
//        }

        return super.onOptionsItemSelected(item);
    }
}
