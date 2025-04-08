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
 * It validates user inputs, creates a new account using FirebaseAuth,
 * stores additional user details in Firestore, and navigates to HomeActivity upon success.
 */
public class SignupActivity extends AppCompatActivity implements View.OnClickListener {

    // UI Elements
    private EditText etSignupEmail, etSignupPassword, etSignupFname, etSignupLname, etSignupPhone, etSignupYOB;
    private Button btnSignup, btnBack;
    private ProgressBar progressBar;
    private FirebaseAuth fbAuth;

    /**
     * Called when the activity is first created.
     * Sets up the edge-to-edge UI, initializes views, applies animations, and sets click listeners.
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        // Apply system window insets to adjust layout padding for system bars.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components.
        findViews();

        // Set click listeners for buttons.
        btnSignup.setOnClickListener(this);
        btnBack.setOnClickListener(this);

        // Load animations from the res/anim folder.
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);

        // Apply fade-in animation to the main layout.
        View mainLayout = findViewById(R.id.main);
        mainLayout.startAnimation(fadeIn);

        // Apply slide-in animation to interactive buttons.
        btnSignup.startAnimation(slideIn);
        btnBack.startAnimation(slideIn);

        // Initialize FirebaseAuth instance.
        fbAuth = FirebaseAuth.getInstance();
    }

    /**
     * Initializes view components by finding them from the layout.
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
     * Hides the soft keyboard, applies a button press animation, and then performs the corresponding action.
     *
     * @param view The view that was clicked.
     */
    @Override
    public void onClick(View view) {
        // Dismiss the soft keyboard.
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        // Load and start the button press animation.
        Animation buttonPress = AnimationUtils.loadAnimation(this, R.anim.button_press);
        view.startAnimation(buttonPress);

        // Determine action based on which view was clicked.
        if (view == btnSignup) {
            // Validate fields; if validation fails, exit early.
            if (!validateFields()) return;

            // Show progress indicator.
            progressBar.setVisibility(View.VISIBLE);

            // Sanitize email input and get password.
            String email = sanitizeInput(etSignupEmail.getText().toString());
            String pass = etSignupPassword.getText().toString();

            // Create a new user account using FirebaseAuth.
            fbAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            // Hide progress indicator once operation completes.
                            progressBar.setVisibility(View.GONE);
                            if (task.isSuccessful()) {
                                // Send email verification.
                                fbAuth.getCurrentUser().sendEmailVerification();

                                // Retrieve and sanitize additional user details.
                                String firstName = sanitizeInput(etSignupFname.getText().toString());
                                String lastName = sanitizeInput(etSignupLname.getText().toString());
                                String phone = sanitizeInput(etSignupPhone.getText().toString());
                                int yob = Integer.parseInt(etSignupYOB.getText().toString());

                                // Create a MyUser object with additional details.
                                MyUser user = new MyUser(firstName, lastName, phone, yob);
                                FirebaseFirestore store = FirebaseFirestore.getInstance();
                                // Save user details in Firestore.
                                store.collection("users").document(fbAuth.getUid()).set(user)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(SignupActivity.this, "User created. Please verify your email.", Toast.LENGTH_LONG).show();

                                                    // Store the user's first name in shared preferences.
                                                    getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                                                            .edit()
                                                            .putString("firstName", firstName)
                                                            .apply();

                                                    // Navigate to HomeActivity, passing the first name for personalization.
                                                    Intent intent = new Intent(SignupActivity.this, HomeActivity.class);
                                                    intent.putExtra("firstName", firstName);
                                                    startActivity(intent);
                                                } else {
                                                    showFieldError(etSignupEmail, "Error: Unable to save user details");
                                                }
                                            }
                                        });
                            } else {
                                // Handle failure during account creation.
                                String errorMessage = task.getException().getMessage();
                                if (errorMessage != null && errorMessage.contains("email")) {
                                    showFieldError(etSignupEmail, "Email already in use");
                                } else {
                                    showErrorToast("Error: " + errorMessage);
                                }
                            }
                        }
                    });
        } else if (view == btnBack) {
            // Navigate back to MainActivity.
            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Validates the signup input fields.
     *
     * @return True if all fields are valid; otherwise, false.
     */
    private boolean validateFields() {
        boolean isValid = true;

        // Validate email.
        if (etSignupEmail.getText().toString().isEmpty() || !isValidEmail(etSignupEmail.getText().toString())) {
            showFieldError(etSignupEmail, "Enter a valid email address (e.g., user@example.com)");
            isValid = false;
        }
        // Validate password.
        if (etSignupPassword.getText().toString().isEmpty() || !isValidPassword(etSignupPassword.getText().toString())) {
            showFieldError(etSignupPassword, "Password must be at least 6 characters, contain a number, an uppercase letter, and a lowercase letter");
            isValid = false;
        }
        // Validate first name.
        if (etSignupFname.getText().toString().isEmpty()) {
            showFieldError(etSignupFname, "First name is required");
            isValid = false;
        }
        // Validate last name.
        if (etSignupLname.getText().toString().isEmpty()) {
            showFieldError(etSignupLname, "Last name is required");
            isValid = false;
        }
        // Validate phone number.
        if (etSignupPhone.getText().toString().isEmpty() || !isValidPhone(etSignupPhone.getText().toString())) {
            showFieldError(etSignupPhone, "Enter a valid phone number (e.g., 123-456-7890)");
            isValid = false;
        }
        // Validate year of birth.
        if (etSignupYOB.getText().toString().isEmpty() || !isValidYOB(etSignupYOB.getText().toString())) {
            showFieldError(etSignupYOB, "Enter a valid year of birth");
            isValid = false;
        }

        return isValid;
    }

    /**
     * Displays an error on the given input field.
     *
     * @param field   The EditText field to display the error on.
     * @param message The error message.
     */
    private void showFieldError(EditText field, String message) {
        field.setError(message);
        field.requestFocus();
    }

    /**
     * Validates email format using a regex pattern.
     *
     * @param email The email address to validate.
     * @return True if the email format is valid; otherwise, false.
     */
    private boolean isValidEmail(String email) {
        return Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$").matcher(email).matches();
    }

    /**
     * Validates phone number format using a regex pattern.
     *
     * @param phone The phone number to validate.
     * @return True if the phone format is valid; otherwise, false.
     */
    private boolean isValidPhone(String phone) {
        return Pattern.compile("^(\\+\\d{1,2}\\s?)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}$").matcher(phone).matches();
    }

    /**
     * Validates password ensuring it meets the required criteria.
     *
     * @param password The password to validate.
     * @return True if the password is valid; otherwise, false.
     */
    private boolean isValidPassword(String password) {
        return password.length() >= 6 &&
                Pattern.compile(".*[a-z].*").matcher(password).matches() &&
                Pattern.compile(".*[A-Z].*").matcher(password).matches() &&
                Pattern.compile(".*\\d.*").matcher(password).matches();
    }

    /**
     * Validates the year of birth.
     *
     * @param yob The year of birth as a string.
     * @return True if the year is between 1900 and 2025; otherwise, false.
     */
    private boolean isValidYOB(String yob) {
        try {
            int year = Integer.parseInt(yob);
            return year > 1900 && year <= 2025;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Sanitizes user input by removing potentially harmful characters.
     *
     * @param input The raw user input.
     * @return The sanitized input.
     */
    private String sanitizeInput(String input) {
        return input.replaceAll("[<>\"'/]", "");
    }

    /**
     * Displays a toast message with an error.
     *
     * @param message The error message to display.
     */
    private void showErrorToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Inflates the options menu.
     *
     * @param menu The menu to inflate.
     * @return True if the menu is successfully created.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.before_login_menu, menu);
        return true;
    }

    /**
     * Handles options menu item selection and navigates accordingly.
     *
     * @param item The selected menu item.
     * @return True if the item selection was handled.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        if (item.getItemId() == R.id.menu_main) {
            intent = new Intent(this, MainActivity.class);
        } else if (item.getItemId() == R.id.menu_Login) {
            intent = new Intent(this, LoginActivity.class);
        } else if (item.getItemId() == R.id.menu_SignUp) {
            intent = new Intent(this, SignupActivity.class);
        } else if (item.getItemId() == R.id.menu_ForgotPassword) {
            intent = new Intent(this, ForgotPasswordActivity.class);
        }
        if (intent != null) {
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
