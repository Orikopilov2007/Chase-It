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
 * LoginActivity
 * <p>
 * This activity handles user login functionality. It validates user input, authenticates the user using FirebaseAuth,
 * and retrieves additional user details from Firestore upon successful authentication. The activity also applies animations
 * for a smoother user experience and handles navigation to other related activities through button click events and options menu.
 * </p>
 * <p>
 * Key Flow:
 * <ul>
 *   <li>Applies system window insets for proper layout padding.</li>
 *   <li>Finds and initializes all UI components.</li>
 *   <li>Applies fade in and slide in animations to the main layout and action buttons.</li>
 *   <li>Handles login by validating email and password, authenticating via FirebaseAuth, and fetching user details from Firestore.</li>
 *   <li>Handles navigation to ForgotPasswordActivity, MainActivity, or SignupActivity through both buttons and the options menu.</li>
 * </ul>
 * </p>
 */
public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    // Tag for logging purposes
    private static final String TAG = "LoginActivity";

    // UI Elements: EditTexts for login and password, Buttons for actions, and a ProgressBar
    private EditText etLoginEmail, etLoginPassword;
    private Button btnLogin, btnForgotPassword, btnBack;
    private ProgressBar progressBar;

    /**
     * onCreate
     * <p>
     * Called when the activity is created. Sets up the UI, applies window insets for proper padding,
     * initializes view components with findViews(), loads animations, and registers click listeners.
     * </p>
     *
     * @param savedInstanceState Bundle with any saved state from previous instances.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content view to activity_login.xml layout
        setContentView(R.layout.activity_login);

        // Apply system window insets to ensure proper layout padding (e.g., for status and navigation bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI elements using the findViews() method
        findViews();

        // Load UI animations for a better user experience
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);

        // Start fade in animation on the main layout container
        View mainLayout = findViewById(R.id.main);
        mainLayout.startAnimation(fadeIn);

        // Apply slide in animation to the action buttons
        animateView(btnLogin, slideIn);
        animateView(btnForgotPassword, slideIn);
        animateView(btnBack, slideIn);

        // Set click listeners for the buttons (login, forgot password, back)
        btnLogin.setOnClickListener(this);
        btnForgotPassword.setOnClickListener(this);
        btnBack.setOnClickListener(this);
    }

    /**
     * findViews
     * <p>
     * Initializes all the UI elements by finding them from the layout resource file.
     * </p>
     */
    private void findViews() {
        // Locate the EditText fields for email and password
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        // Locate the action buttons for login, forgot password, and back navigation
        btnLogin = findViewById(R.id.btnLogin);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);
        btnBack = findViewById(R.id.btnBack);
        // Locate the ProgressBar for showing progress during login attempts
        progressBar = findViewById(R.id.progressBar);
    }

    /**
     * animateView
     * <p>
     * Helper method to apply a given animation to a view.
     * </p>
     *
     * @param view      The view to animate.
     * @param animation The animation to be applied.
     */
    private void animateView(View view, Animation animation) {
        // Start the provided animation on the specified view
        view.startAnimation(animation);
    }

    /**
     * onClick
     * <p>
     * Handles click events for the login, forgot password, and back buttons.
     * Plays a button press animation before executing the appropriate action based on which view was clicked.
     * </p>
     *
     * @param view The view that was clicked.
     */
    @Override
    public void onClick(View view) {
        // Load the button press animation from resources
        Animation buttonPress = AnimationUtils.loadAnimation(this, R.anim.button_press);
        // Set an animation listener to execute actions after the animation completes
        buttonPress.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // No action required at animation start
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Perform the action corresponding to the clicked button
                if (view == btnLogin) {
                    // If login button clicked, initiate the login process
                    handleLogin();
                } else if (view == btnForgotPassword) {
                    // If forgot password button clicked, navigate to ForgotPasswordActivity
                    goToForgotPassword();
                } else if (view == btnBack) {
                    // If back button clicked, navigate to MainActivity and finish this activity
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // No action needed for repeated animation events
            }
        });
        // Start the button press animation on the clicked view
        view.startAnimation(buttonPress);
    }

    /**
     * handleLogin
     * <p>
     * Processes the login by clearing previous errors, validating user input,
     * showing a progress indicator, and authenticating using FirebaseAuth.
     * On successful authentication, user details are fetched from Firestore.
     * </p>
     */
    private void handleLogin() {
        // Clear any previous error messages
        clearErrors();
        // Show the progress bar to indicate a login attempt is in progress
        progressBar.setVisibility(View.VISIBLE);

        // Retrieve and sanitize the email input and obtain password input
        String email = sanitizeInput(etLoginEmail.getText().toString());
        String pass = etLoginPassword.getText().toString();

        // Validate the email and password fields; if invalid, hide progress and return early
        if (!validateFields(email, pass)) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Attempt to sign in using FirebaseAuth with the provided credentials
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        fbAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        // Log success and notify user on successful login
                        Log.d(TAG, "Login successful");
                        Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                        // Fetch additional user details from Firestore
                        fetchUserDetails();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Log error, notify the user, and handle authentication failure scenarios
                        Log.e(TAG, "Login failed: " + e.getMessage());
                        Toast.makeText(LoginActivity.this, "Login Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        handleAuthenticationFailure(e.getMessage());
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    /**
     * fetchUserDetails
     * <p>
     * Retrieves the authenticated user's details from Firestore.
     * On success, stores the user's first name in SharedPreferences and navigates to HomeActivity.
     * </p>
     */
    private void fetchUserDetails() {
        // Get the user ID from FirebaseAuth
        String userId = FirebaseAuth.getInstance().getUid();
        // Obtain an instance of FirebaseFirestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Retrieve the user document using the user ID
        db.collection("users").document(userId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        // Hide the progress indicator
                        progressBar.setVisibility(View.GONE);
                        // If user details exist, retrieve the first name and store it in SharedPreferences
                        if (documentSnapshot.exists()) {
                            String firstName = documentSnapshot.getString("firstName");
                            getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                                    .edit()
                                    .putString("firstName", firstName)
                                    .apply();

                            // Create an intent to navigate to HomeActivity and pass the first name as extra data
                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            intent.putExtra("firstName", firstName);
                            startActivity(intent);
                            finish();
                        } else {
                            // If document does not exist, show a toast indicating missing user details
                            Toast.makeText(LoginActivity.this, "User details not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Hide the progress indicator on failure and notify the user
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Error fetching user details", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error fetching user details: " + e.getMessage());
                    }
                });
    }

    /**
     * goToForgotPassword
     * <p>
     * Navigates the user to the ForgotPasswordActivity.
     * </p>
     */
    private void goToForgotPassword() {
        // Create an intent to open ForgotPasswordActivity and start it
        startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
    }

    /**
     * validateFields
     * <p>
     * Validates the email and password input fields.
     * Checks if the email is non-empty and in a valid format and if the password meets
     * certain criteria. Displays error messages on invalid fields.
     * </p>
     *
     * @param email The user's email input.
     * @param pass  The user's password input.
     * @return true if both fields are valid; otherwise, false.
     */
    private boolean validateFields(String email, String pass) {
        boolean isValid = true;
        // Check if email is empty or invalid
        if (email.isEmpty()) {
            etLoginEmail.setError("Email is required");
            isValid = false;
        } else if (!isValidEmail(email)) {
            etLoginEmail.setError("Enter a valid email address");
            isValid = false;
        }
        // Check if password is empty or does not meet complexity criteria
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
     * handleAuthenticationFailure
     * <p>
     * Processes the error message from a failed authentication attempt by setting appropriate error messages
     * on the input fields.
     * </p>
     *
     * @param errorMessage The error message returned by Firebase.
     */
    private void handleAuthenticationFailure(String errorMessage) {
        // Set specific error messages based on keywords in the error message
        if (errorMessage.toLowerCase().contains("password")) {
            etLoginPassword.setError("Incorrect password");
        } else if (errorMessage.toLowerCase().contains("no user")) {
            etLoginEmail.setError("No account found with this email");
        } else {
            etLoginEmail.setError("Invalid email or password");
        }
    }

    /**
     * clearErrors
     * <p>
     * Clears any error messages from the email and password input fields.
     * </p>
     */
    private void clearErrors() {
        etLoginEmail.setError(null);
        etLoginPassword.setError(null);
    }

    /**
     * sanitizeInput
     * <p>
     * Cleanses the provided user input by removing potentially dangerous characters.
     * </p>
     *
     * @param input The raw user input string.
     * @return The sanitized input string.
     */
    private String sanitizeInput(String input) {
        return input.replaceAll("[<>\"'/]", "").trim();
    }

    /**
     * isValidEmail
     * <p>
     * Validates the email format using Android's built-in Patterns.
     * </p>
     *
     * @param email The email address to validate.
     * @return true if the email is valid; otherwise, false.
     */
    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * isValidPassword
     * <p>
     * Validates that the password meets the minimum criteria: at least 6 characters,
     * contains an uppercase letter, a lowercase letter, and a number.
     * </p>
     *
     * @param password The password to validate.
     * @return true if the password meets the criteria; otherwise, false.
     */
    private boolean isValidPassword(String password) {
        return password.length() >= 6 &&
                Pattern.compile(".*[a-z].*").matcher(password).matches() &&
                Pattern.compile(".*[A-Z].*").matcher(password).matches() &&
                Pattern.compile(".*\\d.*").matcher(password).matches();
    }

    /**
     * onCreateOptionsMenu
     * <p>
     * Inflates the options menu from an XML resource file. This menu is available on the toolbar.
     * </p>
     *
     * @param menu The Menu object to populate.
     * @return true if the menu is successfully created.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.before_login_menu, menu);
        return true;
    }

    /**
     * onOptionsItemSelected
     * <p>
     * Handles click events for the options menu items. Depending on which menu item is selected,
     * navigates to the corresponding activity.
     * </p>
     *
     * @param item The selected MenuItem.
     * @return true if the selection is handled; otherwise, passes the event to the superclass.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Process selection based on the menu item ID
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
