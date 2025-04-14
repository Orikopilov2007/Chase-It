package com.example.mypoject1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.regex.Pattern;

/**
 * SignupActivity handles user registration.
 * <p>
 * This activity is responsible for collecting user input, validating the input fields,
 * creating a new user account via FirebaseAuth, storing additional user details in Firestore,
 * and navigating to HomeActivity upon a successful signup.
 * </p>
 */
public class SignupActivity extends AppCompatActivity implements View.OnClickListener {

    // UI Elements for user input and interaction.
    private EditText etSignupEmail, etSignupPassword, etSignupFname, etSignupLname, etSignupPhone, etSignupYOB;
    private Button btnSignup, btnBack;
    private ProgressBar progressBar;
    // Firebase authentication instance for creating new users.
    private FirebaseAuth fbAuth;

    /**
     * Called when the activity is first created.
     * <p>
     * This method sets up the edge-to-edge layout, adjusts layout insets for system bars,
     * initializes the UI components, applies animations to the views, and sets click listeners.
     * It also initializes the FirebaseAuth instance for later use.
     * </p>
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Enable an edge-to-edge UI experience.
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        // Apply system window insets to ensure proper layout padding for system bars (status/navigation).
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize all UI components (EditTexts, Buttons, ProgressBar).
        findViews();

        // Set click listeners for the Signup and Back buttons.
        btnSignup.setOnClickListener(this);
        btnBack.setOnClickListener(this);

        // Load animations from the resources.
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);

        // Apply fade-in animation to the main layout for a smooth appearance.
        View mainLayout = findViewById(R.id.main);
        mainLayout.startAnimation(fadeIn);

        // Apply slide-in animations to interactive buttons.
        btnSignup.startAnimation(slideIn);
        btnBack.startAnimation(slideIn);

        // Initialize Firebase Authentication instance.
        fbAuth = FirebaseAuth.getInstance();
    }

    /**
     * Initializes view components by finding them in the layout.
     * <p>
     * This method maps the UI elements from the XML layout file to their corresponding Java objects.
     * </p>
     */
    private void findViews() {
        etSignupEmail = findViewById(R.id.etSignupEmail);
        etSignupPassword = findViewById(R.id.etSignupPassword);
        etSignupFname = findViewById(R.id.etSignupFname);
        etSignupLname = findViewById(R.id.etSignupLname);
        etSignupPhone = findViewById(R.id.etSignupPhone);
        etSignupYOB = findViewById(R.id.etSignupYOB);
        btnSignup = findViewById(R.id.btnSignup);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);
    }

    /**
     * Handles click events for the signup and back buttons.
     * <p>
     * When a button is clicked, the method dismisses the soft keyboard, starts a button press animation,
     * and then proceeds with the appropriate action. For signup, it validates inputs and creates a new account.
     * For back, it navigates to the MainActivity.
     * </p>
     *
     * @param view The view that was clicked.
     */
    @Override
    public void onClick(View view) {
        // Dismiss the soft keyboard to enhance user experience.
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        // Load and start a button press animation for visual feedback.
        Animation buttonPress = AnimationUtils.loadAnimation(this, R.anim.button_press);
        view.startAnimation(buttonPress);

        // Determine which button was pressed and perform the corresponding action.
        if (view == btnSignup) {
            // Validate all input fields. If validation fails, stop further processing.
            if (!validateFields()) return;

            // Display the progress indicator during the account creation process.
            progressBar.setVisibility(View.VISIBLE);

            // Retrieve and sanitize the email and password inputs.
            String email = sanitizeInput(etSignupEmail.getText().toString());
            String pass = etSignupPassword.getText().toString();

            // Use FirebaseAuth to create a new user account with the provided credentials.
            fbAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            // Hide the progress indicator when the operation is complete.
                            progressBar.setVisibility(View.GONE);
                            if (task.isSuccessful()) {
                                // Send an email verification to the newly created user.
                                fbAuth.getCurrentUser().sendEmailVerification();

                                // Retrieve and sanitize additional user details.
                                String firstName = sanitizeInput(etSignupFname.getText().toString());
                                String lastName = sanitizeInput(etSignupLname.getText().toString());
                                String phone = sanitizeInput(etSignupPhone.getText().toString());
                                int yob = Integer.parseInt(etSignupYOB.getText().toString());

                                // Create a new MyUser object to hold the additional user details.
                                MyUser user = new MyUser(firstName, lastName, phone, yob);
                                FirebaseFirestore store = FirebaseFirestore.getInstance();
                                // Save the additional user details in the Firestore database.
                                store.collection("users").document(fbAuth.getUid()).set(user)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    // Notify the user to verify their email.
                                                    Toast.makeText(SignupActivity.this, "User created. Please verify your email.", Toast.LENGTH_LONG).show();

                                                    // Store the user's first name in SharedPreferences for personalization.
                                                    getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                                                            .edit()
                                                            .putString("firstName", firstName)
                                                            .apply();

                                                    // Navigate to HomeActivity, passing the user's first name.
                                                    Intent intent = new Intent(SignupActivity.this, HomeActivity.class);
                                                    intent.putExtra("firstName", firstName);
                                                    startActivity(intent);
                                                } else {
                                                    // Display an error on the email field if saving user details fails.
                                                    showFieldError(etSignupEmail, "Error: Unable to save user details");
                                                }
                                            }
                                        });
                            } else {
                                // If account creation fails, extract and handle the error message.
                                String errorMessage = task.getException().getMessage();
                                if (errorMessage != null && errorMessage.contains("email")) {
                                    // Display a specific error if the email is already in use.
                                    showFieldError(etSignupEmail, "Email already in use");
                                } else {
                                    // Display a general error message.
                                    Toast.makeText(SignupActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    });
        } else if (view == btnBack) {
            // Navigate back to MainActivity when the back button is pressed.
            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Validates the signup input fields.
     * <p>
     * This method checks the email, password, first name, last name, phone number,
     * and year of birth to ensure they meet the required format and criteria.
     * </p>
     *
     * @return True if all fields are valid; otherwise, false.
     */
    private boolean validateFields() {
        boolean isValid = true;

        // Validate email field: must not be empty and match a proper email format.
        if (etSignupEmail.getText().toString().isEmpty() || !isValidEmail(etSignupEmail.getText().toString())) {
            showFieldError(etSignupEmail, "Enter a valid email address (e.g., user@example.com)");
            isValid = false;
        }
        // Validate password field: must not be empty and follow password strength requirements.
        if (etSignupPassword.getText().toString().isEmpty() || !isValidPassword(etSignupPassword.getText().toString())) {
            showFieldError(etSignupPassword, "Password must be at least 6 characters, contain a number, an uppercase letter, and a lowercase letter");
            isValid = false;
        }
        // Validate first name: cannot be empty.
        if (etSignupFname.getText().toString().isEmpty()) {
            showFieldError(etSignupFname, "First name is required");
            isValid = false;
        }
        // Validate last name: cannot be empty.
        if (etSignupLname.getText().toString().isEmpty()) {
            showFieldError(etSignupLname, "Last name is required");
            isValid = false;
        }
        // Validate phone number: must not be empty and match the phone format.
        if (etSignupPhone.getText().toString().isEmpty() || !isValidPhone(etSignupPhone.getText().toString())) {
            showFieldError(etSignupPhone, "Enter a valid phone number (e.g., 123-456-7890)");
            isValid = false;
        }
        // Validate year of birth: must not be empty and fall within a valid range.
        if (etSignupYOB.getText().toString().isEmpty() || !isValidYOB(etSignupYOB.getText().toString())) {
            showFieldError(etSignupYOB, "Enter a valid year of birth");
            isValid = false;
        }

        return isValid;
    }

    /**
     * Displays an error on the specified input field and sets focus to it.
     *
     * @param field   The EditText field to display the error message.
     * @param message The error message to show.
     */
    private void showFieldError(EditText field, String message) {
        field.setError(message);
        field.requestFocus();
    }

    /**
     * Validates the email address using a regular expression pattern.
     *
     * @param email The email address to validate.
     * @return True if the email matches the expected format; otherwise, false.
     */
    private boolean isValidEmail(String email) {
        return Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$").matcher(email).matches();
    }

    /**
     * Validates the phone number using a regular expression pattern.
     *
     * @param phone The phone number to validate.
     * @return True if the phone number matches the expected format; otherwise, false.
     */
    private boolean isValidPhone(String phone) {
        return Pattern.compile("^(\\+\\d{1,2}\\s?)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}$").matcher(phone).matches();
    }

    /**
     * Validates the password to ensure it meets the required criteria.
     * <p>
     * The password must be at least 6 characters long and contain at least one lowercase letter,
     * one uppercase letter, and one digit.
     * </p>
     *
     * @param password The password string to validate.
     * @return True if the password is valid; otherwise, false.
     */
    private boolean isValidPassword(String password) {
        return password.length() >= 6 &&
                Pattern.compile(".*[a-z].*").matcher(password).matches() &&
                Pattern.compile(".*[A-Z].*").matcher(password).matches() &&
                Pattern.compile(".*\\d.*").matcher(password).matches();
    }

    /**
     * Validates the year of birth to ensure it falls within a reasonable range.
     *
     * @param yob The year of birth as a string.
     * @return True if the year is between 1900 and 2025; otherwise, false.
     */
    private boolean isValidYOB(String yob) {
        try {
            int year = Integer.parseInt(yob);
            return year > 1900 && year <= 2025;
        } catch (NumberFormatException e) {
            // If parsing fails, the year of birth is invalid.
            return false;
        }
    }

    /**
     * Sanitizes user input by removing potentially harmful characters.
     * <p>
     * This method replaces characters like <, >, ", ', and / with an empty string
     * to prevent any malicious input from affecting the application.
     * </p>
     *
     * @param input The raw user input.
     * @return The sanitized version of the input.
     */
    private String sanitizeInput(String input) {
        return input.replaceAll("[<>\"'/]", "");
    }

    /**
     * Inflates the options menu from the resource file.
     *
     * @param menu The menu to be inflated.
     * @return True if the menu is successfully created.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu using the before_login_menu XML resource.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.before_login_menu, menu);
        return true;
    }

    /**
     * Handles options menu item selections and navigates to the corresponding activity.
     *
     * @param item The selected menu item.
     * @return True if the selection was handled.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        // Determine the target activity based on the selected menu item.
        if (item.getItemId() == R.id.menu_main) {
            intent = new Intent(this, MainActivity.class);
        } else if (item.getItemId() == R.id.menu_Login) {
            intent = new Intent(this, LoginActivity.class);
        } else if (item.getItemId() == R.id.menu_SignUp) {
            intent = new Intent(this, SignupActivity.class);
        } else if (item.getItemId() == R.id.menu_ForgotPassword) {
            intent = new Intent(this, ForgotPasswordActivity.class);
        }
        // If an intent was created, start the corresponding activity.
        if (intent != null) {
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
