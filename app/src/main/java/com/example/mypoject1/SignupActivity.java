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
    private Button btnSignup;
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
    }

    @Override
    public void onClick(View view) {
        if (view == btnSignup) {
            if (!validateFields()) return;

            String email = sanitizeInput(etSignupEmail.getText().toString());
            String pass = etSignupPassword.getText().toString();

            fbAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // User creation succeeded, proceed with Firestore
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
                                                    Toast.makeText(SignupActivity.this, "User created", Toast.LENGTH_LONG).show();
                                                    // Inside the onComplete of Firebase user creation
                                                    Intent intent = new Intent(SignupActivity.this, HomeActivity.class);
                                                    intent.putExtra("firstName", firstName); // Pass the first name to HomeActivity
                                                    startActivity(intent);
                                                } else {
                                                    showFieldError(etSignupEmail, "Error: Unable to save user details");
                                                }
                                            }
                                        });
                            } else {
                                // Handle errors during user creation
                                String errorMessage = task.getException().getMessage();
                                if (errorMessage != null && errorMessage.contains("email")) {
                                    showFieldError(etSignupEmail, "Email already in use");
                                } else {
                                    showErrorToast("Error: " + errorMessage);
                                }
                            }
                        }
                    });
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
        // Modify regex for more accurate phone number matching (example: US number format)
        return Pattern.compile("^(\\+\\d{1,2}\\s?)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}$").matcher(phone).matches();
    }

    private boolean isValidPassword(String password) {
        // Password must contain at least one lowercase letter, one uppercase letter, one number, and be at least 6 characters
        return password.length() >= 6 &&
                Pattern.compile(".*[a-z].*").matcher(password).matches() && // Contains lowercase
                Pattern.compile(".*[A-Z].*").matcher(password).matches() && // Contains uppercase
                Pattern.compile(".*\\d.*").matcher(password).matches();   // Contains digit
    }

    private boolean isValidYOB(String yob) {
        try {
            int year = Integer.parseInt(yob);
            return year > 1900 && year <= 2025; // Adjust as per requirement
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String sanitizeInput(String input) {
        return input.replaceAll("[<>\"'/]", ""); // Removes dangerous characters
    }

    private void showErrorToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // Back button click method
    public void onBackButtonClick(View view) {
        Intent intent = new Intent(SignupActivity.this, MainActivity.class); // Go to the main activity
        startActivity(intent);
        finish(); // Optional, to close the current activity
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.before_login_menu, menu);
        return true;
    }

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
