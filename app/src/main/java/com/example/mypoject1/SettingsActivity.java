package com.example.mypoject1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.appcompat.app.AlertDialog;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;

import java.util.List;
import java.util.UUID;
import android.app.Dialog;
import android.view.animation.Animation.AnimationListener;
import android.widget.BaseAdapter;

/**
 * UserDetailsActivity displays the user's details, allows editing of specific fields,
 * and supports updating the profile picture and other details in Firebase.
 */
public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    // UI Elements for displaying and editing user details.
    private EditText etDetailsFirstName, etDetailsLastName, etDetailsPhone, etDetailsYOB;
    private EditText tvDetailsEmail;
    private ImageView ivProfilePhoto;
    private Button btnLogout, btnDeleteAccount, btnBack, btnForgotPassword, btnSave;

    // Tag used for logging purposes.
    private static final String TAG = "UserDetailsActivity";
    // Uri to hold the user's profile image.
    private Uri profileImageUri;

    // Request codes for handling activity results.
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 101;
    private static final int UCROP_REQUEST_CODE = UCrop.REQUEST_CROP;

    /**
     * Called when the activity is created.
     * <p>
     * This method sets the layout, initializes views, loads user details from Firestore,
     * applies animations to UI elements, and sets click listeners for interactive components.
     * </p>
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout for the activity.
        setContentView(R.layout.activity_settings);

        // Find and initialize UI elements.
        findViews();
        // Load user details from Firestore and display them.
        loadDetails();

        // Load animations from resources.
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        // Apply fade-in animation to the main layout.
        View mainLayout = findViewById(R.id.main);
        mainLayout.startAnimation(fadeIn);

        // Set click listeners for interactive UI elements.
        ivProfilePhoto.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
        btnDeleteAccount.setOnClickListener(this);
        btnBack.setOnClickListener(this);
        btnForgotPassword.setOnClickListener(this);
        btnSave.setOnClickListener(this);

        // Apply slide-in animation to various interactive elements.
        ivProfilePhoto.startAnimation(slideIn);
        btnLogout.startAnimation(slideIn);
        btnDeleteAccount.startAnimation(slideIn);
        btnBack.startAnimation(slideIn);
        btnForgotPassword.startAnimation(slideIn);
        btnSave.startAnimation(slideIn);
    }

    /**
     * Finds and initializes all views from the layout.
     * <p>
     * This method maps the UI elements defined in the XML layout to their corresponding Java objects.
     * </p>
     */
    private void findViews() {
        tvDetailsEmail = findViewById(R.id.tvDetailsEmail);
        etDetailsFirstName = findViewById(R.id.etDetailsFname);
        etDetailsLastName = findViewById(R.id.etDetailsLname);
        etDetailsPhone = findViewById(R.id.etDetailsPhone);
        etDetailsYOB = findViewById(R.id.tvDetailsYOB);
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        btnBack = findViewById(R.id.btnBack);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);
        btnSave = findViewById(R.id.btnSave);
    }

    /**
     * Loads user details from Firebase Firestore and displays them in the UI.
     * <p>
     * The method retrieves the current user's document from the "users" collection using their UID.
     * It then populates the email, first name, last name, phone number, and year of birth fields.
     * If a profile image URL exists, it uses Glide to load and display the image.
     * </p>
     */
    private void loadDetails() {
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        String uid = fbAuth.getUid();
        FirebaseFirestore store = FirebaseFirestore.getInstance();

        // Fetch the user's document from the "users" collection using their UID.
        store.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            // Convert the document into a MyUser object.
            MyUser user = documentSnapshot.toObject(MyUser.class);

            // Display the user's email from FirebaseAuth.
            tvDetailsEmail.setText(fbAuth.getCurrentUser().getEmail());
            // Populate the user details in their respective EditTexts.
            etDetailsFirstName.setText(user.getFirstName());
            etDetailsLastName.setText(user.getLastName());
            etDetailsPhone.setText(user.getPhone());
            etDetailsYOB.setText(String.valueOf(user.getYob()));

            // Load the user's profile image using Glide if the URL is available.
            if (user.getProfileImageUri() != null && !user.getProfileImageUri().isEmpty()) {
                profileImageUri = Uri.parse(user.getProfileImageUri());
                Glide.with(this)
                        .load(profileImageUri) // Load image from URI.
                        .placeholder(R.drawable.default_user_photo)
                        .transform(new CircleCrop()) // Apply circular crop transformation.
                        .into(ivProfilePhoto);
            } else {
                // If no profile image is set, display the default user photo.
                ivProfilePhoto.setImageResource(R.drawable.default_user_photo);
            }
        }).addOnFailureListener(e -> {
            // Log error and show a toast message if user details fail to load.
            Log.e(TAG, "Failed to load user details: " + e.getMessage());
            Toast.makeText(this, "Failed to load user details", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Handles click events for various UI elements.
     * <p>
     * The method first applies a button press animation, then based on which view was clicked,
     * it performs actions such as showing image options, logging out, deleting the account,
     * navigating back, going to the forgot password screen, or saving changes.
     * </p>
     *
     * @param view The clicked view.
     */
    @Override
    public void onClick(View view) {
        // Load a button press animation.
        Animation buttonPress = AnimationUtils.loadAnimation(this, R.anim.button_press);
        buttonPress.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // No action needed at the start of the animation.
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Perform actions based on which view was clicked.
                Intent intent = null;
                if (view == ivProfilePhoto) {
                    // If the profile photo is clicked, show image options.
                    showImageOptions();
                } else if (view == btnLogout) {
                    // If logout button is clicked, perform logout.
                    logout();
                } else if (view == btnDeleteAccount) {
                    // Confirm and delete the account.
                    confirmAndDeleteAccount();
                } else if (view == btnBack) {
                    // Finish the activity and go back.
                    finish();
                } else if (view == btnForgotPassword) {
                    // Navigate to the Forgot Password activity.
                    goToForgotPassword();
                } else if (view == btnSave) {
                    // Save the updated user details.
                    saveChanges();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // No action needed during animation repetition.
            }
        });
        // Start the button press animation.
        view.startAnimation(buttonPress);
    }

    /**
     * Displays an AlertDialog to confirm and proceed with account deletion.
     * <p>
     * If the user confirms, it deletes the user's account from FirebaseAuth,
     * removes their document from Firestore, clears SharedPreferences, and
     * navigates to MainActivity.
     * </p>
     */
    private void confirmAndDeleteAccount() {
        new AlertDialog.Builder(SettingsActivity.this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    String uid = FirebaseAuth.getInstance().getUid();
                    if (uid == null) {
                        // If UID is null, the user is not logged in; abort deletion.
                        return;
                    }
                    // Delete the user from FirebaseAuth.
                    FirebaseAuth.getInstance().getCurrentUser().delete()
                            .addOnSuccessListener(aVoid -> {
                                // After deleting from Auth, delete user details from Firestore.
                                FirebaseFirestore.getInstance().collection("users")
                                        .document(uid)
                                        .delete()
                                        .addOnSuccessListener(aVoid1 -> {
                                            // Clear SharedPreferences.
                                            getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                                                    .edit()
                                                    .clear()
                                                    .apply();
                                            Toast.makeText(SettingsActivity.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                            // Navigate to MainActivity.
                                            startActivity(new Intent(SettingsActivity.this, MainActivity.class));
                                            finish();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(SettingsActivity.this, "Failed to delete account data from DB: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(SettingsActivity.this, "Failed to delete account: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    /**
     * Validates the input details based on specific criteria.
     * <p>
     * Validations include:
     * - First Name and Last Name: Must contain only letters, spaces, or hyphens.
     * - Phone Number: Must be exactly 10 digits.
     * - Year of Birth: Must be an integer between 1900 and 2025.
     * </p>
     *
     * @param firstName The input first name.
     * @param lastName  The input last name.
     * @param phone     The input phone number.
     * @param yobStr    The input year of birth as a String.
     * @return An error message if any validation fails; otherwise, null.
     */
    private String validateInputs(String firstName, String lastName, String phone, String yobStr) {
        // Validate first name (allows letters, spaces, and hyphens).
        if (firstName.isEmpty() || !firstName.matches("[a-zA-Z\\s\\-]+")) {
            return "Please enter a valid first name.";
        }
        // Validate last name.
        if (lastName.isEmpty() || !lastName.matches("[a-zA-Z\\s\\-]+")) {
            return "Please enter a valid last name.";
        }
        // Validate phone number (must be exactly 10 digits).
        if (!phone.matches("\\d{10}")) {
            return "Please enter a valid 10-digit phone number.";
        }
        // Validate year of birth.
        int yob;
        try {
            yob = Integer.parseInt(yobStr);
        } catch (NumberFormatException e) {
            return "Year of birth must be a number.";
        }
        if (yob < 1900 || yob > 2025) {
            return "Year of birth must be between 1900 and 2025.";
        }
        return null;
    }

    /**
     * Saves updated user details (first name, last name, phone, and year of birth) to Firestore.
     * <p>
     * The method first validates the inputs. If valid, it updates the user document in Firestore
     * and saves the new details in SharedPreferences. Upon success, it reloads the details.
     * </p>
     */
    private void saveChanges() {
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        String uid = fbAuth.getUid();
        FirebaseFirestore store = FirebaseFirestore.getInstance();

        // Extract updated details from the EditText fields.
        String updatedFirstName = etDetailsFirstName.getText().toString().trim();
        String updatedLastName = etDetailsLastName.getText().toString().trim();
        String updatedPhone = etDetailsPhone.getText().toString().trim();
        String updatedYob = etDetailsYOB.getText().toString().trim();

        // Validate the input details.
        String validationError = validateInputs(updatedFirstName, updatedLastName, updatedPhone, updatedYob);
        if (validationError != null) {
            Toast.makeText(this, validationError, Toast.LENGTH_SHORT).show();
            return;
        }

        // Update the user details in Firestore.
        store.collection("users").document(uid)
                .update("firstName", updatedFirstName,
                        "lastName", updatedLastName,
                        "phone", updatedPhone,
                        "yob", Integer.parseInt(updatedYob))
                .addOnSuccessListener(aVoid -> {
                    // On success, update SharedPreferences with the new details.
                    SharedPreferences.Editor editor = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE).edit();
                    editor.putString("firstName", updatedFirstName);
                    editor.putString("lastName", updatedLastName);
                    editor.putString("phone", updatedPhone);
                    editor.putInt("yob", Integer.parseInt(updatedYob));
                    editor.apply();

                    Toast.makeText(SettingsActivity.this, "Details updated successfully", Toast.LENGTH_SHORT).show();
                    // Reload the user details to reflect the updates.
                    loadDetails();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update user details: " + e.getMessage());
                    Toast.makeText(SettingsActivity.this, "Failed to update details", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Shows the options to view, change, or delete the current profile photo.
     * <p>
     * Displays an AlertDialog with options:
     * - View Photo
     * - Camera (to capture a new image)
     * - Delete Photo
     * - Select from App Gallery
     * </p>
     */
    private void showImageOptions() {
        String[] options = {"View Photo", "Camera", "Delete Photo", "Select from App Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Profile Picture")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            viewPhoto();
                            break;
                        case 1:
                            openCamera();
                            break;
                        case 2:
                            deleteCurrentPhoto();
                            break;
                        case 3:
                            showFirebaseGalleryDialog();
                            break;
                    }
                })
                .show();
    }

    /**
     * Launches an activity to view the profile photo in a zoomable view.
     * <p>
     * If a profile image URI exists, it starts the ZoomableImageActivity and passes the image URI.
     * Otherwise, it shows a toast message indicating no profile picture is available.
     * </p>
     */
    private void viewPhoto() {
        if (profileImageUri != null) {
            Intent intent = new Intent(this, ZoomableImageActivity.class);
            intent.putExtra("imageUri", profileImageUri.toString());
            startActivity(intent);
        } else {
            Toast.makeText(this, "No profile picture available", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Deletes the current profile photo from Firebase Storage and Firestore.
     * <p>
     * This method removes the image file from Storage, clears the URI in Firestore,
     * and resets the UI to show the default user photo.
     * </p>
     */
    private void deleteCurrentPhoto() {
        if (profileImageUri != null) {
            // Delete from Firebase Storage
            StorageReference photoRef = FirebaseStorage.getInstance().getReferenceFromUrl(profileImageUri.toString());
            photoRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        // After deleting the file, clear the Firestore entry
                        String uid = FirebaseAuth.getInstance().getUid();
                        FirebaseFirestore.getInstance().collection("users")
                                .document(uid)
                                .update("profileImageUri", "")
                                .addOnSuccessListener(aVoid1 -> {
                                    profileImageUri = null;
                                    ivProfilePhoto.setImageResource(R.drawable.default_user_photo);
                                    Toast.makeText(SettingsActivity.this, "Profile picture deleted", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(SettingsActivity.this, "Failed to clear profile picture URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(SettingsActivity.this, "Failed to delete image file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "No profile picture to delete", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Opens the camera activity to capture a new profile image.
     * <p>
     * Starts the CameraActivity and expects a result identified by CAMERA_REQUEST_CODE.
     * </p>
     */
    private void openCamera() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    /**
     * Displays a custom dialog showing images stored in Firebase Storage.
     * <p>
     * The method lists all images under "profile_pictures", displays them in a GridView using a custom adapter,
     * and allows the user to select an image for cropping.
     * </p>
     */
    private void showFirebaseGalleryDialog() {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("profile_pictures");
        storageRef.listAll().addOnSuccessListener(listResult -> {
            List<StorageReference> items = listResult.getItems();
            Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_firebase_gallery);
            GridView gridView = dialog.findViewById(R.id.gridViewGallery);
            GalleryAdapter adapter = new GalleryAdapter(this, items);
            gridView.setAdapter(adapter);
            gridView.setOnItemClickListener((parent, view, position, id) -> {
                StorageReference selectedRef = items.get(position);
                selectedRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    dialog.dismiss();
                    profileImageUri = uri;
                    Glide.with(this)
                            .load(uri)
                            .transform(new CircleCrop())
                            .into(ivProfilePhoto);
                    updateProfileImageUriInFirestore(uri.toString());
                });
            });
            dialog.show();
        });
    }

    private void updateProfileImageUriInFirestore(String uriString) {
        String uid = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("profileImageUri", uriString)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update profile picture", Toast.LENGTH_SHORT).show()
                );
    }


    /**
     * Adapter class for displaying image thumbnails in the Firebase Gallery dialog.
     * <p>
     * This adapter loads images from Firebase Storage references into a GridView using Glide.
     * </p>
     */
    private static class GalleryAdapter extends BaseAdapter {
        private Context context;
        private List<StorageReference> items;

        /**
         * Constructor for GalleryAdapter.
         *
         * @param context The context.
         * @param items   The list of StorageReference objects representing images.
         */
        GalleryAdapter(Context context, List<StorageReference> items) {
            this.context = context;
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * Provides the view for an item in the GridView.
         *
         * @param position    The position of the item.
         * @param convertView The recycled view.
         * @param parent      The parent ViewGroup.
         * @return The view for the current item.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                // Inflate the view if not already recycled.
                convertView = LayoutInflater.from(context).inflate(R.layout.item_gallery_image, parent, false);
                imageView = convertView.findViewById(R.id.imageViewItem);
                convertView.setTag(imageView);
            } else {
                imageView = (ImageView) convertView.getTag();
            }
            // Load the image thumbnail using Glide.
            items.get(position).getDownloadUrl().addOnSuccessListener(uri -> {
                Glide.with(context)
                        .load(uri)
                        .placeholder(R.drawable.default_user_photo)
                        .transform(new CircleCrop())
                        .into(imageView);
            });
            return convertView;
        }
    }

    /**
     * Handles the results from activities such as Camera, Gallery, and UCrop.
     * <p>
     * Depending on the request code, this method starts the crop activity for new images
     * or updates the profile photo with the cropped image and uploads it to Firebase Storage.
     * </p>
     *
     * @param requestCode The request code identifying the source of the result.
     * @param resultCode  The result code returned by the activity.
     * @param data        The intent data containing the result.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE
                && resultCode == RESULT_OK
                && data != null) {

            String uriString = data.getStringExtra(CameraActivity.EXTRA_IMAGE_URI);
            if (uriString != null) {
                Uri newUri = Uri.parse(uriString);

                // Glide + CircleCrop
                Glide.with(this)
                        .load(newUri)
                        .transform(new CircleCrop())
                        .into(ivProfilePhoto);

                uploadProfileImage(newUri);

            } else {
                Toast.makeText(this, "Failed to retrieve image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Uploads the profile image to Firebase Storage and updates its URL in Firestore.
     * <p>
     * The method uploads the provided image URI, retrieves the download URL, and updates
     * the corresponding user's document in Firestore. It also provides user feedback based on success or failure.
     * </p>
     *
     * @param imageUri The URI of the image to be uploaded.
     */
    private void uploadProfileImage(Uri imageUri) {
        String uid = FirebaseAuth.getInstance().getUid();
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("profile_pictures/" + uid + "_" + UUID.randomUUID() + ".jpg");

        ref.putFile(imageUri)
                .addOnSuccessListener(task -> ref.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> {
                            // update Firestore
                            FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(uid)
                                    .update("profileImageUri", downloadUri.toString())
                                    .addOnSuccessListener(a -> {
                                        profileImageUri = downloadUri;
                                        Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Firestore update failed: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show()
                                    );
                        })
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }


    /**
     * Inflates the options menu.
     * <p>
     * Loads the menu items from the main_menu XML resource.
     * </p>
     *
     * @param menu The menu to be inflated.
     * @return True if the menu is successfully created.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * Handles menu item selections and navigates to the appropriate activity.
     * <p>
     * Depending on the selected menu item, the method navigates to HomeActivity,
     * logs the user out, navigates to TimerActivity, or opens the ChatbotActivity.
     * </p>
     *
     * @param item The selected menu item.
     * @return True if the event was handled.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        if (item.getItemId() == R.id.menu_home) {
            intent = new Intent(this, HomeActivity.class);
        } else if (item.getItemId() == R.id.menu_logout) {
            logout();
        } else if (item.getItemId() == R.id.menu_timer) {
            intent = new Intent(this, RunningActivity.class);
        } else if (item.getItemId() == R.id.menu_userdetails) {
            intent = new Intent(this, SettingsActivity.class);
        } else if(item.getItemId() == R.id.menu_ChatBot){
            intent = new Intent(this, ChatbotActivity.class);
        }
        if (intent != null) {
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Navigates to the Forgot Password activity.
     * <p>
     * This method starts the ForgotPasswordActivity.
     * </p>
     */
    private void goToForgotPassword() {
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        startActivity(intent);
    }

    /**
     * Logs out the current user.
     * <p>
     * Clears SharedPreferences, signs out from FirebaseAuth, and redirects the user to MainActivity.
     * </p>
     */
    private void logout() {
        // Clear SharedPreferences.
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        // Sign out from FirebaseAuth.
        FirebaseAuth.getInstance().signOut();

        // Redirect to the MainActivity (login screen).
        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
