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

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etLoginEmail, etLoginPassword;
    private Button btnLogin, btnForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViews();
        btnLogin.setOnClickListener(this);
        btnForgotPassword.setOnClickListener(this); // Added listener for Forgot Password button

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Toast to verify that the page has loaded
        Toast.makeText(this, "Login Activity Loaded", Toast.LENGTH_SHORT).show();
    }

    private void findViews() {
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnForgotPassword = findViewById(R.id.btnForgotPassword); // Assign Forgot Password button
    }

    @Override
    public void onClick(View view) {
        if (view == btnLogin) {
            handleLogin();
        } else if (view == btnForgotPassword) {
            goToForgotPassword();
        }
    }

    private void handleLogin() {
        clearErrors(); // Clear previous errors

        String email = sanitizeInput(etLoginEmail.getText().toString());
        String pass = etLoginPassword.getText().toString();

        if (!validateFields(email, pass)) return;

        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        fbAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();

                        String userId = FirebaseAuth.getInstance().getUid();
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("users").document(userId).get()
                                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                    @Override
                                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                                        if (documentSnapshot.exists()) {
                                            String firstName = documentSnapshot.getString("firstName");
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
                                        Toast.makeText(LoginActivity.this, "Error fetching user details", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(LoginActivity.this, "Login Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        handleAuthenticationFailure(e.getMessage());
                    }
                });
    }

    private void goToForgotPassword() {
        Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
        startActivity(intent);
        // Removed finish() here to ensure the LoginActivity is not closed prematurely
    }

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

    private void handleAuthenticationFailure(String errorMessage) {
        if (errorMessage.contains("password")) {
            etLoginPassword.setError("Incorrect password");
        } else if (errorMessage.contains("no user")) {
            etLoginEmail.setError("No account found with this email");
        } else {
            etLoginEmail.setError("Invalid email or password");
        }
    }

    private void clearErrors() {
        etLoginEmail.setError(null);
        etLoginPassword.setError(null);
    }

    private String sanitizeInput(String input) {
        return input.replaceAll("[<>\"'/]", ""); // Removes dangerous characters
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 6 &&
                Pattern.compile(".*[a-z].*").matcher(password).matches() &&
                Pattern.compile(".*[A-Z].*").matcher(password).matches() &&
                Pattern.compile(".*\\d.*").matcher(password).matches();
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
