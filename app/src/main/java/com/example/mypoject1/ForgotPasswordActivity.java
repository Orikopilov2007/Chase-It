package com.example.mypoject1;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnResetPassword, btnBack;

    /**
     * Called when the activity is first created. This method sets the content view and initializes views.
     * It also sets listeners for the reset password button and back button.
     *
     * @param savedInstanceState Bundle containing saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        findViews();
        btnResetPassword.setOnClickListener(v -> resetPassword());
        btnBack.setOnClickListener(v -> goToHomeActivity());  // Back button functionality

        Toast.makeText(this, "Forgot Password Activity Loaded", Toast.LENGTH_SHORT).show();
    }

    /**
     * Initializes the views by finding the respective views using their IDs.
     * This method is called during onCreate to set up the UI components.
     */
    private void findViews() {
        etEmail = findViewById(R.id.etEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        btnBack = findViewById(R.id.btnBack);  // Add back button
    }

    /**
     * Validates the email input, checks for empty or invalid email, and sends a password reset email.
     * If the email is valid, it triggers the reset process.
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

        // Directly send a password reset email
        sendPasswordResetEmail(email);  // No need to check for password fields
    }

    /**
     * Sends a password reset email using Firebase Authentication.
     * Displays a success or failure message based on the result.
     *
     * @param email the email address to send the reset request to
     */
    private void sendPasswordResetEmail(String email) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ForgotPasswordActivity.this, "Password reset email sent!", Toast.LENGTH_SHORT).show();
                    finish();  // Close activity after successful password reset email is sent
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ForgotPasswordActivity.this, "Error sending reset email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Navigates the user back to the login screen.
     * This method is triggered when the back button is pressed.
     */
    private void goToHomeActivity() {
        Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();  // Close this activity and return to LoginActivity
    }

    /**
     * Sanitizes the input by removing any potentially harmful characters.
     * This helps to prevent XSS and other security issues.
     *
     * @param input the string to sanitize
     * @return sanitized string
     */
    private String sanitizeInput(String input) {
        return input.replaceAll("[<>\"'/]", "");  // Removes dangerous characters
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
     * The menu contains options like Main, Login, SignUp, and ForgotPassword.
     *
     * @param menu the menu to inflate
     * @return true if the menu was successfully inflated
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.before_login_menu, menu);
        return true;
    }

    /**
     * Handles item selections from the options menu.
     * Depending on the selected menu item, it navigates to the appropriate activity.
     *
     * @param item the menu item that was selected
     * @return true if the item selection was handled
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_main) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_Login) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_SignUp) {
            Intent intent = new Intent(this, SignupActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_ForgotPassword) {
            Intent intent = new Intent(this, com.example.mypoject1.ForgotPasswordActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }
}
