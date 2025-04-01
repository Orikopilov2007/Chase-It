package com.example.mypoject1;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
    private Button btnLogin, btnForgotPassword, btnBack;

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
        // Load animations from res/anim folder
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);

        // Apply fade-in animation to the entire layout
        View mainLayout = findViewById(R.id.main);
        mainLayout.startAnimation(fadeIn);

        // Apply slide-in animation to key interactive elements, e.g., buttons
        btnLogin.startAnimation(slideIn);
        btnForgotPassword.startAnimation(slideIn);
        btnBack.startAnimation(slideIn);

        // Set click listeners
        btnLogin.setOnClickListener(this);
        btnForgotPassword.setOnClickListener(this);
        btnBack.setOnClickListener(this);

    }

    private void findViews() {
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);
        btnBack = findViewById(R.id.btnBack);
    }

    @Override
    public void onClick(View view) {
        // Load the press animation
        Animation buttonPress = AnimationUtils.loadAnimation(this, R.anim.button_press);

        // Set an animation listener to perform actions after animation ends
        buttonPress.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Not needed, required to implement AnimationListener
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Intent intent = null;
                if (view == btnLogin) {
                    handleLogin();
                } else if (view == btnForgotPassword) {
                    goToForgotPassword();
                } else if (view == btnBack) {
                    intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Not needed, required to implement AnimationListener
            }
        });

        // Start the animation on the clicked button
        view.startAnimation(buttonPress);
    }


    private void handleLogin() {
        clearErrors();

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
        return input.replaceAll("[<>\"'/]", "");
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.before_login_menu, menu);
        return true;
    }

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
