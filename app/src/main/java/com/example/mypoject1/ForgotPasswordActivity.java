package com.example.mypoject1;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

/**
 * ForgotPasswordActivity
 * <p>
 * This activity allows the user to reset their password. It provides a user interface for entering an email
 * address and sends a password reset email via Firebase Authentication.
 * </p>
 * <p>
 * Key Features:
 * <ul>
 *   <li>Initializes UI components and applies animations.</li>
 *   <li>Handles button clicks with animated feedback before invoking actions.</li>
 *   <li>Validates email input and checks network connectivity before attempting a password reset.</li>
 *   <li>Uses Firebase Authentication to send password reset emails.</li>
 *   <li>Provides menu options for navigation to main, login, sign-up, or forgot password screens.</li>
 * </ul>
 * </p>
 */
public class ForgotPasswordActivity extends AppCompatActivity implements View.OnClickListener {

    // UI Elements
    private EditText etEmail;
    private Button btnResetPassword, btnBack;
    private ProgressBar progressBar;

    /**
     * onCreate
     * <p>
     * Called when the activity is first created. This method sets the layout, initializes UI components,
     * loads animations, and applies them to key elements.
     * </p>
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the UI layout for the activity
        setContentView(R.layout.activity_forgot_password);

        // Find and initialize all view elements from the layout
        findViews();

        // Load animations from resources for enhanced UI effects
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);

        // Apply fade in animation to the main layout container
        View mainLayout = findViewById(R.id.main);
        mainLayout.startAnimation(fadeIn);

        // Apply slide in animation to the key buttons
        btnResetPassword.startAnimation(slideIn);
        btnBack.startAnimation(slideIn);

        //making sure that pressing on this button does nothing in this page
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });
    }

    /**
     * onClick
     * <p>
     * Handles click events for the activity's buttons. Plays a button press animation before executing the action.
     * Based on which button is clicked, it triggers either a password reset or a navigation back to MainActivity.
     * </p>
     *
     * @param view The view that was clicked.
     */
    @Override
    public void onClick(View view) {
        // Load the button press animation
        Animation buttonPress = AnimationUtils.loadAnimation(this, R.anim.button_press);

        // Set up an animation listener to trigger actions after the animation ends
        buttonPress.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // No additional action on animation start
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Determine which button was clicked based on view reference
                if (view == btnResetPassword) {
                    // If reset password button, execute password reset
                    resetPassword();
                } else if (view == btnBack) {
                    // If back button, navigate to MainActivity
                    startActivity(new Intent(ForgotPasswordActivity.this, MainActivity.class));
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // No action needed for animation repeats
            }
        });

        // Start the button press animation on the clicked view
        view.startAnimation(buttonPress);
    }

    /**
     * findViews
     * <p>
     * Initializes all view elements by finding them in the layout and setting click listeners.
     * </p>
     */
    private void findViews() {
        // Locate UI elements by their IDs
        etEmail = findViewById(R.id.etEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);

        // Set click listeners for interactive buttons
        btnResetPassword.setOnClickListener(this);
        btnBack.setOnClickListener(this);
    }

    /**
     * resetPassword
     * <p>
     * Validates the entered email, checks for network connectivity, and if all conditions are met,
     * initiates sending a password reset email via Firebase Authentication.
     * </p>
     */
    private void resetPassword() {
        // Retrieve and sanitize the email input from the EditText
        String email = sanitizeInput(etEmail.getText().toString().trim());

        // Verify that the email is not empty
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            return;
        }

        // Validate the email format
        if (!isValidEmail(email)) {
            etEmail.setError("Enter a valid email address");
            return;
        }

        // Check if the device is connected to the internet before proceeding
        if (!isNetworkConnected()) {
            // Notify the user if there is no network connection via a Snackbar
            showSnackbar("No network connection. Please check your internet settings.");
            return;
        }

        // Show a progress indicator to inform the user of the ongoing process
        progressBar.setVisibility(View.VISIBLE);
        // Proceed to send the password reset email
        sendPasswordResetEmail(email);
    }

    /**
     * sendPasswordResetEmail
     * <p>
     * Uses Firebase Authentication to send a password reset email to the provided address.
     * Updates the UI upon success or failure.
     * </p>
     *
     * @param email The email address to which the password reset email should be sent.
     */
    private void sendPasswordResetEmail(String email) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                // Handle success by hiding progress, notifying the user, and finishing the activity
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    showSnackbar("Password reset email sent!");
                    finish();
                })
                // Handle failure by hiding progress and showing an error message
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    showSnackbar("Error sending reset email: " + e.getMessage());
                });
    }

    /**
     * isNetworkConnected
     * <p>
     * Checks whether the device currently has an active network connection.
     * </p>
     *
     * @return true if the device is connected to the internet, false otherwise.
     */
    private boolean isNetworkConnected() {
        // Obtain the ConnectivityManager to access network status
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        // Retrieve information about the active network
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        // Return true if there is an active, connected network
        return activeNetwork != null && activeNetwork.isConnected();
    }

    /**
     * showSnackbar
     * <p>
     * Displays a Snackbar message at the bottom of the screen using the main layout as an anchor.
     * </p>
     *
     * @param message The message text to display.
     */
    private void showSnackbar(String message) {
        Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG).show();
    }

    /**
     * sanitizeInput
     * <p>
     * Cleanses the provided input string by removing potentially harmful or unwanted characters.
     * </p>
     *
     * @param input The raw string input.
     * @return A sanitized version of the input string.
     */
    private String sanitizeInput(String input) {
        // Remove characters that might cause issues in further processing
        return input.replaceAll("[<>\"'/]", "");
    }

    /**
     * isValidEmail
     * <p>
     * Validates if the provided email address has a proper format.
     * </p>
     *
     * @param email The email address to validate.
     * @return true if the email matches standard patterns, false otherwise.
     */
    private boolean isValidEmail(String email) {
        // Use Android's built-in Patterns utility to verify email format
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * onCreateOptionsMenu
     * <p>
     * Inflates the options menu for this activity from the specified XML resource.
     * </p>
     *
     * @param menu The Menu object to populate.
     * @return true if the menu is successfully created.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the before-login menu from the XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.before_login_menu, menu);
        return true;
    }

    /**
     * onOptionsItemSelected
     * <p>
     * Handles selections from the options menu by navigating to different activities based on the selected item.
     * </p>
     *
     * @param item The selected menu item.
     * @return true if the action is handled, else calls the superclass implementation.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Create an intent variable for navigating between activities
        Intent intent;
        // Determine action based on the item selected from the menu
        if (item.getItemId() == R.id.menu_main) {
            intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_Login) {
            intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_SignUp) {
            intent = new Intent(this, SignupActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_ForgotPassword) {
            intent = new Intent(this, ForgotPasswordActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    //making sure that pressing on this button does nothing in this page
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
