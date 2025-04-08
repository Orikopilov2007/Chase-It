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

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etEmail;
    private Button btnResetPassword, btnBack;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        findViews();

        // Load animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);

        View mainLayout = findViewById(R.id.main);
        mainLayout.startAnimation(fadeIn);

        btnResetPassword.startAnimation(slideIn);
        btnBack.startAnimation(slideIn);
    }

    @Override
    public void onClick(View view) {
        Animation buttonPress = AnimationUtils.loadAnimation(this, R.anim.button_press);

        buttonPress.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (view == btnResetPassword) {
                    resetPassword();
                } else if (view == btnBack) {
                    startActivity(new Intent(ForgotPasswordActivity.this, MainActivity.class));
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        view.startAnimation(buttonPress);
    }

    /**
     * Initializes views and sets click listeners.
     */
    private void findViews() {
        etEmail = findViewById(R.id.etEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);

        btnResetPassword.setOnClickListener(this);
        btnBack.setOnClickListener(this);
    }

    /**
     * Validates the email input, checks for network connectivity, and sends a password reset email.
     */
    private void resetPassword() {
        String email = sanitizeInput(etEmail.getText().toString().trim());

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            return;
        }

        if (!isValidEmail(email)) {
            etEmail.setError("Enter a valid email address");
            return;
        }

        // Check network connectivity before proceeding
        if (!isNetworkConnected()) {
            showSnackbar("No network connection. Please check your internet settings.");
            return;
        }

        // Show the progress indicator
        progressBar.setVisibility(View.VISIBLE);
        sendPasswordResetEmail(email);
    }

    /**
     * Sends a password reset email using Firebase Authentication.
     *
     * @param email the email address to send the reset request to
     */
    private void sendPasswordResetEmail(String email) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    showSnackbar("Password reset email sent!");
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    showSnackbar("Error sending reset email: " + e.getMessage());
                });
    }

    /**
     * Checks if there is an active network connection.
     *
     * @return true if connected, false otherwise.
     */
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    /**
     * Displays a Snackbar with the given message.
     *
     * @param message the message to display.
     */
    private void showSnackbar(String message) {
        Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG).show();
    }

    /**
     * Sanitizes the input by removing any potentially harmful characters.
     *
     * @param input the string to sanitize
     * @return sanitized string
     */
    private String sanitizeInput(String input) {
        return input.replaceAll("[<>\"'/]", "");
    }

    /**
     * Validates if the provided email is in a valid format.
     *
     * @param email the email address to validate
     * @return true if the email is valid, false otherwise
     */
    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Inflates the menu for this activity.
     *
     * @param menu the menu to inflate
     * @return true if the menu was successfully inflated
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.before_login_menu, menu);
        return true;
    }

    /**
     * Handles selections from the options menu.
     *
     * @param item the selected menu item.
     * @return true if the selection was handled.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
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
}
