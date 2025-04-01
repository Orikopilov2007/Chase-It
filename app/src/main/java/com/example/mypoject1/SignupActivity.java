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

public class SignupActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText etSignupEmail, etSignupPassword, etSignupFname, etSignupLname, etSignupPhone, etSignupYOB;
    private Button btnSignup, btnBack;
    private ProgressBar progressBar;
    private FirebaseAuth fbAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViews();
        btnSignup.setOnClickListener(this);
        btnBack.setOnClickListener(this);

        // Load animations from the res/anim folder
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);

        // Apply fade in animation to the entire layout
        View mainLayout = findViewById(R.id.main);
        mainLayout.startAnimation(fadeIn);

        // Apply slide in animation to the buttons
        btnSignup.startAnimation(slideIn);
        btnBack.startAnimation(slideIn);

        fbAuth = FirebaseAuth.getInstance();
    }

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

    @Override
    public void onClick(View view) {
        // Dismiss keyboard on button click
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        Animation buttonPress = AnimationUtils.loadAnimation(this, R.anim.button_press);
        view.startAnimation(buttonPress);

        if (view == btnSignup) {
            if (!validateFields()) return;

            // Show progress indicator
            progressBar.setVisibility(View.VISIBLE);

            String email = sanitizeInput(etSignupEmail.getText().toString());
            String pass = etSignupPassword.getText().toString();

            fbAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            // Hide progress indicator once operation is complete
                            progressBar.setVisibility(View.GONE);
                            if (task.isSuccessful()) {
                                // Send verification email
                                fbAuth.getCurrentUser().sendEmailVerification();

                                String firstName = sanitizeInput(etSignupFname.getText().toString());
                                String lastName = sanitizeInput(etSignupLname.getText().toString());
                                String phone = sanitizeInput(etSignupPhone.getText().toString());
                                int yob = Integer.parseInt(etSignupYOB.getText().toString());

                                MyUser user = new MyUser(firstName, lastName, phone, yob);
                                FirebaseFirestore store = FirebaseFirestore.getInstance();
                                store.collection("users").document(fbAuth.getUid()).set(user)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(SignupActivity.this, "User created. Please verify your email.", Toast.LENGTH_LONG).show();
                                                    // Navigate to HomeActivity, passing the first name for personalization
                                                    Intent intent = new Intent(SignupActivity.this, HomeActivity.class);
                                                    intent.putExtra("firstName", firstName);
                                                    startActivity(intent);
                                                } else {
                                                    showFieldError(etSignupEmail, "Error: Unable to save user details");
                                                }
                                            }
                                        });
                            } else {
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
            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private boolean validateFields() {
        boolean isValid = true;

        if (etSignupEmail.getText().toString().isEmpty() || !isValidEmail(etSignupEmail.getText().toString())) {
            showFieldError(etSignupEmail, "Enter a valid email address (e.g., user@example.com)");
            isValid = false;
        }
        if (etSignupPassword.getText().toString().isEmpty() || !isValidPassword(etSignupPassword.getText().toString())) {
            showFieldError(etSignupPassword, "Password must be at least 6 characters, contain a number, an uppercase letter, and a lowercase letter");
            isValid = false;
        }
        if (etSignupFname.getText().toString().isEmpty()) {
            showFieldError(etSignupFname, "First name is required");
            isValid = false;
        }
        if (etSignupLname.getText().toString().isEmpty()) {
            showFieldError(etSignupLname, "Last name is required");
            isValid = false;
        }
        if (etSignupPhone.getText().toString().isEmpty() || !isValidPhone(etSignupPhone.getText().toString())) {
            showFieldError(etSignupPhone, "Enter a valid phone number (e.g., 123-456-7890)");
            isValid = false;
        }
        if (etSignupYOB.getText().toString().isEmpty() || !isValidYOB(etSignupYOB.getText().toString())) {
            showFieldError(etSignupYOB, "Enter a valid year of birth");
            isValid = false;
        }

        return isValid;
    }

    private void showFieldError(EditText field, String message) {
        field.setError(message);
        field.requestFocus();
    }

    private boolean isValidEmail(String email) {
        return Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$").matcher(email).matches();
    }

    private boolean isValidPhone(String phone) {
        return Pattern.compile("^(\\+\\d{1,2}\\s?)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}$").matcher(phone).matches();
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 6 &&
                Pattern.compile(".*[a-z].*").matcher(password).matches() &&
                Pattern.compile(".*[A-Z].*").matcher(password).matches() &&
                Pattern.compile(".*\\d.*").matcher(password).matches();
    }

    private boolean isValidYOB(String yob) {
        try {
            int year = Integer.parseInt(yob);
            return year > 1900 && year <= 2025;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String sanitizeInput(String input) {
        return input.replaceAll("[<>\"'/]", "");
    }

    private void showErrorToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.before_login_menu, menu);
        return true;
    }

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
            intent = new Intent(this, com.example.mypoject1.ForgotPasswordActivity.class);
        }
        if(intent != null) {
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
