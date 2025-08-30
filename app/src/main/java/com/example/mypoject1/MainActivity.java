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

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;

/**
 * MainActivity
 * <p>
 * This is the launcher activity of the application. It acts as the entry point for users
 * and handles essential app initialization, such as permission requests and background service startup.
 * It also serves as a navigation hub for login and signup screens and enhances user experience using animations.
 * </p>
 *
 * <p>
 * Key Features and Flow:
 * <ul>
 *   <li>Checks for and requests runtime permissions (like notifications and alarms).</li>
 *   <li>Starts background services (e.g., MyService).</li>
 *   <li>Initializes and animates UI components.</li>
 *   <li>Handles user navigation to LoginActivity and SignupActivity.</li>
 * </ul>
 * </p>
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // UI elements
    private Button btnMainSignUp, btnMainLogIn;
    private static final int PERMISSION_REQUEST_CODE = 1001;

    /**
     * Called when the activity is first created.
     * Sets up UI, requests permissions, starts services, and applies animations.
     *
     * @param savedInstanceState Bundle containing saved state (if any).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check and request required permissions
        checkAndRequestPermissions();

        // Start MyService for notifications or background tasks
        startService(new Intent(this, MyService.class));

        // Initialize UI components
        btnMainSignUp = findViewById(R.id.btnMainSignUp);
        btnMainLogIn = findViewById(R.id.btnMainLogIn);

        // Set click listeners for buttons
        btnMainSignUp.setOnClickListener(this);
        btnMainLogIn.setOnClickListener(this);

        // Load animations from resources
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);

        View mainLayout = findViewById(R.id.main);
        mainLayout.startAnimation(fadeIn);

        btnMainSignUp.startAnimation(slideIn);
        btnMainLogIn.startAnimation(slideIn);

        //making sure that pressing on this button does nothing in this page
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });
    }

    /**
     * Checks for required permissions and requests them if not already granted.
     * Also handles scheduling exact alarms for Android S and above.
     */
    private void checkAndRequestPermissions() {
        // Request POST_NOTIFICATIONS permission on Android 13 (TIRAMISU) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            }
        }
        // Request permission to schedule exact alarms on Android S and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    /**
     * Handles click events for the buttons.
     * Applies a button press animation before navigating to the appropriate screen.
     *
     * @param view The view that was clicked.
     */
    @Override
    public void onClick(View view) {
        // Load the button press animation
        Animation buttonPress = AnimationUtils.loadAnimation(this, R.anim.button_press);
        buttonPress.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Navigate based on which button was clicked
                if (view == btnMainSignUp) {
                    startActivity(new Intent(MainActivity.this, SignupActivity.class));
                } else if (view == btnMainLogIn) {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        view.startAnimation(buttonPress);
    }

    /**
     * Inflates the options menu from the resource file.
     *
     * @param menu The menu to be inflated.
     * @return True if the menu is successfully created.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.before_login_menu, menu);
        return true;
    }

    /**
     * Handles menu item selection and navigates to the corresponding activity.
     *
     * @param item The selected menu item.
     * @return True if the item selection is handled.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_main) {
            startActivity(new Intent(this, MainActivity.class));
        } else if (itemId == R.id.menu_Login) {
            startActivity(new Intent(this, LoginActivity.class));
        } else if (itemId == R.id.menu_SignUp) {
            startActivity(new Intent(this, SignupActivity.class));
        } else if (itemId == R.id.menu_ForgotPassword) {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    //making sure that pressing on this button does nothing in this page
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
