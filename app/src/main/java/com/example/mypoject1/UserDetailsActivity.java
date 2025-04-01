package com.example.mypoject1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.UUID;

import android.provider.MediaStore;

public class UserDetailsActivity extends AppCompatActivity implements View.OnClickListener {

    // UI Elements
    TextView tvDetailsEmail, tvDetailsFirstName, tvDetailsLastName, tvDetailsPhone, tvDetailsYOB;
    ImageView ivProfilePhoto;
    Button btnLogout, btnDeleteAccount, btnBack, btnForgotPassword;
    private static final String TAG = "UserDetailsActivity"; // Tag for logging

    // Variable to store profile image URI
    Uri profileImageUri;

    // Constants for Camera and Gallery Request Codes
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        // Initialize UI elements and load user details
        findViews();
        loadDetails();

        // Load animations from res/anim folder
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);

        // Apply fade-in animation to the entire layout
        View mainLayout = findViewById(R.id.main);
        mainLayout.startAnimation(fadeIn);

        // Set click listeners for buttons and image view
        ivProfilePhoto.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
        btnDeleteAccount.setOnClickListener(this);
        btnBack.setOnClickListener(this);
        btnForgotPassword.setOnClickListener(this); // Added listener for Forgot Password button

        ivProfilePhoto.startAnimation(slideIn);
        btnLogout.startAnimation(slideIn);
        btnDeleteAccount.startAnimation(slideIn);
        btnBack.startAnimation(slideIn);
        btnForgotPassword.startAnimation(slideIn);
    }

    // Initialize the UI elements
    private void findViews() {
        tvDetailsEmail = findViewById(R.id.tvDetailsEmail);
        tvDetailsFirstName = findViewById(R.id.tvDetailsFname);
        tvDetailsLastName = findViewById(R.id.tvDetailsLname);
        tvDetailsPhone = findViewById(R.id.tvDetailsPhone);
        tvDetailsYOB = findViewById(R.id.tvDetailsYOB);
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        btnBack = findViewById(R.id.btnBack);
        btnForgotPassword = findViewById(R.id.btnForgotPassword); // Assign Forgot Password button
    }

    // Load user details from Firebase Firestore and populate the UI elements
    private void loadDetails() {
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        String uid = fbAuth.getUid();
        FirebaseFirestore store = FirebaseFirestore.getInstance();

        // Fetch user data from Firestore
        store.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            MyUser user = documentSnapshot.toObject(MyUser.class);
            tvDetailsEmail.setText(fbAuth.getCurrentUser().getEmail());
            tvDetailsFirstName.setText(user.getFirstName());
            tvDetailsLastName.setText(user.getLastName());
            tvDetailsPhone.setText(user.getPhone());
            tvDetailsYOB.setText(String.valueOf(user.getYob()));

            // Set profile image if available
            if (user.getProfileImageUri() != null) {
                profileImageUri = Uri.parse(user.getProfileImageUri()); // Store the URI in the variable
                ivProfilePhoto.setImageURI(profileImageUri);
            }
        });
    }

    // Handle button click events
    @Override
    public void onClick(View view) {
        // Load the press animation
        Animation buttonPress = AnimationUtils.loadAnimation(this, R.anim.button_press);
        buttonPress.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Not needed, required to implement AnimationListener
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Intent intent = null;
                if (view == ivProfilePhoto) {
                    showImageOptions(); // Show image options when profile photo is clicked
                } else if (view == btnLogout) {
                    // Log out the user and redirect to MainActivity
                    FirebaseAuth.getInstance().signOut();
                     intent = new Intent(UserDetailsActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else if (view == btnDeleteAccount) {
                    // Show confirmation dialog for deleting account
                    new AlertDialog.Builder(UserDetailsActivity.this)
                            .setTitle("Delete Account")
                            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                // Proceed with account deletion
                                FirebaseAuth.getInstance().getCurrentUser().delete()
                                        .addOnSuccessListener(aVoid -> {
                                            FirebaseFirestore.getInstance().collection("users").document(FirebaseAuth.getInstance().getUid()).delete()
                                                    .addOnSuccessListener(aVoid1 -> {
                                                        Toast.makeText(UserDetailsActivity.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(UserDetailsActivity.this, MainActivity.class));
                                                        finish();

                                                    })
                                                    .addOnFailureListener(e -> Toast.makeText(UserDetailsActivity.this, "Failed to delete account", Toast.LENGTH_SHORT).show());
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(UserDetailsActivity.this, "Failed to delete account", Toast.LENGTH_SHORT).show());
                            })
                            .setNegativeButton("No", (dialog, which) -> dialog.dismiss()) // Dismiss the dialog if No is clicked
                            .setCancelable(false) // Prevent dismissal by clicking outside
                            .show();
                } else if (view == btnBack) {
                    finish();
                } else if (view == btnForgotPassword) {
                    goToForgotPassword(); // Navigate to Forgot Password activity
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Not needed, required to implement AnimationListener
            }
        });

        view.startAnimation(buttonPress);
    }

    // Show options for viewing or changing profile photo
    private void showImageOptions() {
        String[] options = {"View Photo", "Change Photo"};
        new AlertDialog.Builder(this)
                .setTitle("Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        viewPhoto(); // View the current profile photo
                    } else if (which == 1) {
                        pickImage(); // Allow the user to pick a new photo
                    }
                })
                .show();
    }

    // View the profile photo in a new activity
    private void viewPhoto() {
        if (profileImageUri != null) {
            Intent intent = new Intent(this, ZoomableImageActivity.class);
            intent.putExtra("imageUri", profileImageUri.toString());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Still on development", Toast.LENGTH_SHORT).show();
            //Toast.makeText(this, "No profile picture available", Toast.LENGTH_SHORT).show();
        }
    }

    // Open options to choose between camera or gallery
    private void pickImage() {
        String[] options = {"Camera", "Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Select Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera(); // Open camera to take a new photo
                    } else if (which == 1) {
                        pickImageFromGallery(); // Open gallery to pick an image
                    }
                })
                .show();
    }

    // Open camera to capture a new profile picture
    private void openCamera() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    // Pick an image from the gallery
    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    // Handle the result from the camera or gallery
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE || requestCode == GALLERY_REQUEST_CODE) {
                Uri imageUri = data.getData();
                ivProfilePhoto.setImageURI(imageUri);
                uploadProfileImage(imageUri); // Upload the selected image
            }
        }
    }

    // Upload profile image to Firebase Storage
    public void uploadProfileImage(Uri imageUri) {
        // Get Firebase authentication instance
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        String uid = fbAuth.getUid(); // Get user ID
        FirebaseStorage storage = FirebaseStorage.getInstance(); // Get Firebase storage instance
        StorageReference storageRef = storage.getReference(); // Reference to Firebase Storage root

        // Create a unique reference for the profile image in Firebase Storage
        StorageReference storageReference = storageRef.child("profile_pictures/" + UUID.randomUUID().toString());

        Log.d(TAG, "Uploading image URI: " + imageUri.toString());

        // Upload the image to Firebase Storage
        storageReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Image uploaded successfully");

                    // Retrieve the download URL of the uploaded image
                    storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                                // Update the profileImageUri variable
                                profileImageUri = uri;

                                // Update Firestore with the new profile picture URL
                                FirebaseFirestore.getInstance().collection("users").document(uid)
                                        .update("profileImageUri", uri.toString())
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "Profile picture updated in Firestore");
                                            Toast.makeText(UserDetailsActivity.this, "Profile picture updated", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to update profile picture in Firestore: " + e.getMessage());
                                            Toast.makeText(UserDetailsActivity.this, "Failed to update profile picture", Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to get download URL: " + e.getMessage());
                                Toast.makeText(UserDetailsActivity.this, "Failed to retrieve image URL", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload profile picture: " + e.getMessage());
                    Toast.makeText(UserDetailsActivity.this, "Failed to upload profile picture", Toast.LENGTH_SHORT).show();
                });
    }

    // Inflate the menu for options
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    // Handle options menu item clicks
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_home) {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_logout) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (item.getItemId() == R.id.menu_camera) {
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_timer) {
            Intent intent = new Intent(this, TimerActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_userdetails) {
            Intent intent = new Intent(this, com.example.mypoject1.UserDetailsActivity.class);
            startActivity(intent);
        }
        else if (item.getItemId() == R.id.menu_ForgotPassword) {
            Intent intent = new Intent(this, com.example.mypoject1.ForgotPasswordActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    // Go to Forgot Password screen
    private void goToForgotPassword() {
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        startActivity(intent);
    }
}
