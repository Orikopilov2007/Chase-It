package com.example.mypoject1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.regex.Pattern;

/**
 * LoginActivity handles user login functionality.
 * It validates user input, authenticates via FirebaseAuth, and fetches user details from Firestore.
 */
public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "LoginActivity";

    // UI Elements
    private EditText etLoginEmail, etLoginPassword;
    private Button btnLogin, btnForgotPassword, btnBack;
    private ProgressBar progressBar;

    /**
     * Called when the activity is created. Sets up UI, animations, and click listeners.
     *
     * @param savedInstanceState Bundle with saved state, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Apply system window insets for proper layout padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI elements
        findViews();

        // Load animations from resources
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);

        // Apply fade-in animation to the main layout
        View mainLayout = findViewById(R.id.main);
        mainLayout.startAnimation(fadeIn);

        // Animate interactive elements with a slide-in effect
        animateView(btnLogin, slideIn);
        animateView(btnForgotPassword, slideIn);
        animateView(btnBack, slideIn);

        // Set click listeners for buttons
        btnLogin.setOnClickListener(this);
        btnForgotPassword.setOnClickListener(this);
        btnBack.setOnClickListener(this);
    }

    /**
     * Initializes the UI elements by finding them from the layout.
     */
    private void findViews() {
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);
    }

    /**
     * Helper method to apply an animation to a view.
     *
     * @param view      The view to animate.
     * @param animation The animation to apply.
     */
    private void animateView(View view, Animation animation) {
        view.startAnimation(animation);
    }

    /**
     * Handles click events for the buttons.
     * Applies a button press animation before executing the appropriate action.
     *
     * @param view The clicked view.
     */
    @Override
    public void onClick(View view) {
        // Load and set up the button press animation
        Animation buttonPress = AnimationUtils.loadAnimation(this, R.anim.button_press);
        buttonPress.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Not needed
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Determine action based on which button was clicked
                if (view == btnLogin) {
                    handleLogin();
                } else if (view == btnForgotPassword) {
                    goToForgotPassword();
                } else if (view == btnBack) {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Not needed
            }
        });
        view.startAnimation(buttonPress);
    }

    /**
     * Handles the login process.
     * Validates input, performs authentication, and fetches user details on success.
     */
    private void handleLogin() {
        clearErrors();
        progressBar.setVisibility(View.VISIBLE); // Show progress indicator

        String email = sanitizeInput(etLoginEmail.getText().toString());
        String pass = etLoginPassword.getText().toString();

        // Validate input fields; if invalid, hide progress and return early.
        if (!validateFields(email, pass)) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Attempt to sign in using FirebaseAuth
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        fbAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        Log.d(TAG, "Login successful");
                        Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                        fetchUserDetails();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Login failed: " + e.getMessage());
                        Toast.makeText(LoginActivity.this, "Login Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        handleAuthenticationFailure(e.getMessage());
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    /**
     * Fetches the authenticated user's details from Firestore.
     * On success, stores the user's first name in shared preferences and navigates to HomeActivity.
     */
    private void fetchUserDetails() {
        String userId = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        progressBar.setVisibility(View.GONE);
                        if (documentSnapshot.exists()) {
                            String firstName = documentSnapshot.getString("firstName");
                            getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                                    .edit()
                                    .putString("firstName", firstName)
                                    .apply();

                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            intent.putExtra("firstName", firstName);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "User details not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Error fetching user details", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error fetching user details: " + e.getMessage());
                    }
                });
    }

    /**
     * Navigates to the ForgotPasswordActivity.
     */
    private void goToForgotPassword() {
        startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
    }

    /**
     * Validates the email and password fields.
     *
     * @param email The user's email input.
     * @param pass  The user's password input.
     * @return True if both fields are valid; otherwise, false.
     */
    private boolean validateFields(String email, String pass) {
        boolean isValid = true;
        if (email.isEmpty()) {
            etLoginEmail.setError("Email is required");
            isValid = false;
        } else if (!isValidEmail(email)) {
            etLoginEmail.setError("Enter a valid email address");
            isValid = false;
        }
        if (pass.isEmpty()) {
            etLoginPassword.setError("Password is required");
            isValid = false;
        } else if (!isValidPassword(pass)) {
            etLoginPassword.setError("Password must be at least 6 characters long, with an uppercase letter, lowercase letter, and a number");
            isValid = false;
        }
        return isValid;
    }

    /**
     * Handles authentication failure by setting error messages on input fields.
     *
     * @param errorMessage The error message received from Firebase.
     */
    private void handleAuthenticationFailure(String errorMessage) {
        if (errorMessage.toLowerCase().contains("password")) {
            etLoginPassword.setError("Incorrect password");
        } else if (errorMessage.toLowerCase().contains("no user")) {
            etLoginEmail.setError("No account found with this email");
        } else {
            etLoginEmail.setError("Invalid email or password");
        }
    }

    /**
     * Clears any error messages on the email and password input fields.
     */
    private void clearErrors() {
        etLoginEmail.setError(null);
        etLoginPassword.setError(null);
    }

    /**
     * Sanitizes user input by removing potentially dangerous characters.
     *
     * @param input The raw user input.
     * @return The sanitized input.
     */
    private String sanitizeInput(String input) {
        return input.replaceAll("[<>\"'/]", "");
    }

    /**
     * Validates the email format using Android's built-in Patterns.
     *
     * @param email The email to validate.
     * @return True if the email is valid; otherwise, false.
     */
    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Validates the password ensuring it meets the minimum criteria:
     * at least 6 characters, one uppercase letter, one lowercase letter, and one digit.
     *
     * @param password The password to validate.
     * @return True if the password meets the criteria; otherwise, false.
     */
    private boolean isValidPassword(String password) {
        return password.length() >= 6 &&
                Pattern.compile(".*[a-z].*").matcher(password).matches() &&
                Pattern.compile(".*[A-Z].*").matcher(password).matches() &&
                Pattern.compile(".*\\d.*").matcher(password).matches();
    }

    /**
     * Inflates the options menu.
     *
     * @param menu The menu to be inflated.
     * @return True if the menu is created successfully.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.before_login_menu, menu);
        return true;
    }

    /**
     * Handles selection of menu items.
     *
     * @param item The selected menu item.
     * @return True if the item selection is handled.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_main) {
            startActivity(new Intent(this, MainActivity.class));
        } else if (item.getItemId() == R.id.menu_Login) {
            startActivity(new Intent(this, LoginActivity.class));
        } else if (item.getItemId() == R.id.menu_SignUp) {
            startActivity(new Intent(this, SignupActivity.class));
        } else if (item.getItemId() == R.id.menu_ForgotPassword) {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}
