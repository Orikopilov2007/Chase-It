package com.example.mypoject1;

import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Button btnMainSignUp, btnMainLogIn;
    private static final int PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request permissions for Android 13/14 if needed
        checkAndRequestPermissions();


        // Start MyService for notifications
        startService(new Intent(this, MyService.class));

        // Initialize views
        btnMainSignUp = findViewById(R.id.btnMainSignUp);
        btnMainLogIn = findViewById(R.id.btnMainLogIn);

        // Set click listeners
        btnMainSignUp.setOnClickListener(this);
        btnMainLogIn.setOnClickListener(this);

        // Load animations from the res/anim folder
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);


        // Apply fade in animation to the entire layout (LinearLayout with id "main")
        View mainLayout = findViewById(R.id.main);
        mainLayout.startAnimation(fadeIn);

        // Apply slide in animation to the buttons
        btnMainSignUp.startAnimation(slideIn);
        btnMainLogIn.startAnimation(slideIn);

        Toast.makeText(this, "Main Activity Loaded", Toast.LENGTH_SHORT).show();

    }

    // Example: Refactored method for checking permissions
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }


    @Override
    public void onClick(View view) {
        Animation buttonPress = AnimationUtils.loadAnimation(this, R.anim.button_press);

        buttonPress.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Not used, I have to use it because of the build of AnimationListener that needs it
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (view == btnMainSignUp) {
                    startActivity(new Intent(MainActivity.this, SignupActivity.class));
                } else if (view == btnMainLogIn) {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Not used, I have to use it because of the build of AnimationListener that needs it
            }
        });

        view.startAnimation(buttonPress);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.before_login_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_main) {
            startActivity(new Intent(this, MainActivity.class));
        } else if (item.getItemId() == R.id.menu_Login) {
            startActivity(new Intent(this, LoginActivity.class));
        } else if (item.getItemId() == R.id.menu_SignUp) {
            startActivity(new Intent(this, SignupActivity.class));
        } else if (item.getItemId() == R.id.menu_ForgotPassword) {
            startActivity(new Intent(this, com.example.mypoject1.ForgotPasswordActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}
