package com.example.mypoject1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.GridView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;

public class UserDetailsActivity extends AppCompatActivity implements View.OnClickListener {

    // UI Elements
    TextView tvDetailsEmail, tvDetailsFirstName, tvDetailsLastName, tvDetailsPhone, tvDetailsYOB;
    ImageView ivProfilePhoto;
    Button btnLogout, btnDeleteAccount, btnBack, btnForgotPassword;
    private static final String TAG = "UserDetailsActivity";

    // Variable to store profile image URI
    Uri profileImageUri;

    // Constants for Request Codes
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 101;
    private static final int STORAGE_GALLERY_REQUEST_CODE = 102;
    private static final int UCROP_REQUEST_CODE = UCrop.REQUEST_CROP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        findViews();
        loadDetails();

        // Load animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        View mainLayout = findViewById(R.id.main);
        mainLayout.startAnimation(fadeIn);

        // Set click listeners
        ivProfilePhoto.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
        btnDeleteAccount.setOnClickListener(this);
        btnBack.setOnClickListener(this);
        btnForgotPassword.setOnClickListener(this);

        ivProfilePhoto.startAnimation(slideIn);
        btnLogout.startAnimation(slideIn);
        btnDeleteAccount.startAnimation(slideIn);
        btnBack.startAnimation(slideIn);
        btnForgotPassword.startAnimation(slideIn);
    }

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
        btnForgotPassword = findViewById(R.id.btnForgotPassword);
    }

    private void loadDetails() {
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        String uid = fbAuth.getUid();
        FirebaseFirestore store = FirebaseFirestore.getInstance();

        store.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            MyUser user = documentSnapshot.toObject(MyUser.class);
            tvDetailsEmail.setText(fbAuth.getCurrentUser().getEmail());
            tvDetailsFirstName.setText(user.getFirstName());
            tvDetailsLastName.setText(user.getLastName());
            tvDetailsPhone.setText(user.getPhone());
            tvDetailsYOB.setText(String.valueOf(user.getYob()));

            // Check if profileImageUri is not null or empty
            if (user.getProfileImageUri() != null && !user.getProfileImageUri().isEmpty()) {
                profileImageUri = Uri.parse(user.getProfileImageUri());
                // Use Glide to load the remote image
                Glide.with(this)
                        .load(profileImageUri)
                        .placeholder(R.drawable.default_user_photo)
                        .transform(new CircleCrop())
                        .into(ivProfilePhoto);
            } else {
                // Optionally set the default image if no URI is available
                ivProfilePhoto.setImageResource(R.drawable.default_user_photo);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load user details: " + e.getMessage());
            Toast.makeText(this, "Failed to load user details", Toast.LENGTH_SHORT).show();
        });
    }


    @Override
    public void onClick(View view) {
        Animation buttonPress = AnimationUtils.loadAnimation(this, R.anim.button_press);
        buttonPress.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                Intent intent = null;
                if (view == ivProfilePhoto) {
                    showImageOptions();
                } else if (view == btnLogout) {
                    FirebaseAuth.getInstance().signOut();
                    intent = new Intent(UserDetailsActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else if (view == btnDeleteAccount) {
                    new AlertDialog.Builder(UserDetailsActivity.this)
                            .setTitle("Delete Account")
                            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                String uid = FirebaseAuth.getInstance().getUid();  // Store UID before deletion
                                if(uid == null) {
                                    // Handle the case where UID is null (user not logged in)
                                    return;
                                }

                                FirebaseAuth.getInstance().getCurrentUser().delete()
                                        .addOnSuccessListener(aVoid -> {
                                            // Delete the user document from Firestore using the stored uid
                                            FirebaseFirestore.getInstance().collection("users")
                                                    .document(uid)
                                                    .delete()
                                                    .addOnSuccessListener(aVoid1 -> {
                                                        // Clear SharedPreferences and navigate back
                                                        getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                                                                .edit()
                                                                .clear()
                                                                .apply();
                                                        Toast.makeText(UserDetailsActivity.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(UserDetailsActivity.this, MainActivity.class));
                                                        finish();
                                                    })
                                                    .addOnFailureListener(e ->
                                                            Toast.makeText(UserDetailsActivity.this, "Failed to delete account data from DB: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(UserDetailsActivity.this, "Failed to delete account: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                            })
                            .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                            .setCancelable(false)
                            .show();
                } else if (view == btnBack) {
                    finish();
                } else if (view == btnForgotPassword) {
                    goToForgotPassword();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) { }
        });

        view.startAnimation(buttonPress);
    }

    private void showImageOptions() {
        // Options: View, Change, Delete, or choose from Firebase Gallery
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

    private void viewPhoto() {
        if (profileImageUri != null) {
            Intent intent = new Intent(this, ZoomableImageActivity.class);
            intent.putExtra("imageUri", profileImageUri.toString());
            startActivity(intent);
        } else {
            Toast.makeText(this, "No profile picture available", Toast.LENGTH_SHORT).show();
        }
    }

    // Delete current photo (clears stored URL and resets default image)
    private void deleteCurrentPhoto() {
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        String uid = fbAuth.getUid();
        FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .update("profileImageUri", "")
                .addOnSuccessListener(aVoid -> {
                    profileImageUri = null;
                    ivProfilePhoto.setImageResource(R.drawable.default_user_photo);
                    Toast.makeText(UserDetailsActivity.this, "Profile picture deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserDetailsActivity.this, "Failed to delete profile picture", Toast.LENGTH_SHORT).show();
                });
    }

    private void openCamera() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    // Show Firebase Gallery in a custom dialog with thumbnails
    private void showFirebaseGalleryDialog() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child("profile_pictures");
        storageRef.listAll().addOnSuccessListener(listResult -> {
            List<StorageReference> items = listResult.getItems();
            if (items.isEmpty()) {
                Toast.makeText(UserDetailsActivity.this, "No images available in App Gallery", Toast.LENGTH_SHORT).show();
                return;
            }
            // Create and show custom dialog
            Dialog dialog = new Dialog(UserDetailsActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_firebase_gallery);

            GridView gridView = dialog.findViewById(R.id.gridViewGallery);
            GalleryAdapter adapter = new GalleryAdapter(this, items);
            gridView.setAdapter(adapter);

            gridView.setOnItemClickListener((parent, view, position, id) -> {
                StorageReference selectedRef = items.get(position);
                selectedRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    dialog.dismiss();
                    startCrop(uri);
                });
            });
            dialog.show();
        }).addOnFailureListener(e ->
                Toast.makeText(UserDetailsActivity.this, "Failed to load images from Storage", Toast.LENGTH_SHORT).show());
    }

    // Adapter for displaying image thumbnails in the Firebase Gallery dialog
    private static class GalleryAdapter extends BaseAdapter {
        private Context context;
        private List<StorageReference> items;

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

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_gallery_image, parent, false);
                imageView = convertView.findViewById(R.id.imageViewItem);
                convertView.setTag(imageView);
            } else {
                imageView = (ImageView) convertView.getTag();
            }
            // Load thumbnail using Glide with circleCrop transformation for consistency
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

    // תהליך חיתוך באמצעות UCrop
    private void startCrop(Uri sourceUri) {
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "cropped_" + UUID.randomUUID().toString() + ".jpg"));
        UCrop.Options options = new UCrop.Options();
        UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(500, 500)
                .withOptions(options)
                .start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == CAMERA_REQUEST_CODE || requestCode == GALLERY_REQUEST_CODE) {
                Uri imageUri = data.getData();
                startCrop(imageUri);
            } else if (requestCode == UCROP_REQUEST_CODE) {
                final Uri croppedUri = UCrop.getOutput(data);
                if (croppedUri != null) {
                    ivProfilePhoto.setImageURI(croppedUri);
                    uploadProfileImage(croppedUri);
                }
            }
        }
    }

    // העלאת תמונת פרופיל לתיקיית "profile_pictures" ועדכון Firestore
    public void uploadProfileImage(Uri imageUri) {
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        String uid = fbAuth.getUid();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        // העלאה לתיקייה profile_pictures עם שם קובץ ייחודי
        StorageReference imageRef = storageRef.child("profile_pictures/" + UUID.randomUUID().toString());

        Log.d(TAG, "Uploading image URI: " + imageUri.toString());
        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        profileImageUri = uri;
                        FirebaseFirestore.getInstance().collection("users")
                                .document(uid)
                                .update("profileImageUri", uri.toString())
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Profile picture updated in Firestore");
                                    Toast.makeText(UserDetailsActivity.this, "Profile picture updated", Toast.LENGTH_SHORT).show();
                                    loadDetails();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to update profile picture in Firestore: " + e.getMessage());
                                    Toast.makeText(UserDetailsActivity.this, "Failed to update profile picture", Toast.LENGTH_SHORT).show();
                                });
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get download URL: " + e.getMessage());
                        Toast.makeText(UserDetailsActivity.this, "Failed to retrieve image URL", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload profile picture: " + e.getMessage());
                    Toast.makeText(UserDetailsActivity.this, "Failed to upload profile picture", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        if (item.getItemId() == R.id.menu_home) {
            intent = new Intent(this, HomeActivity.class);
        } else if (item.getItemId() == R.id.menu_logout) {
            intent = new Intent(this, MainActivity.class);
            finish();
        } else if (item.getItemId() == R.id.menu_camera) {
            intent = new Intent(this, CameraActivity.class);
        } else if (item.getItemId() == R.id.menu_timer) {
            intent = new Intent(this, TimerActivity.class);
        } else if (item.getItemId() == R.id.menu_userdetails) {
            intent = new Intent(this, UserDetailsActivity.class);
        } else if (item.getItemId() == R.id.menu_ForgotPassword) {
            intent = new Intent(this, ForgotPasswordActivity.class);
        } else if(item.getItemId() == R.id.menu_ChatBot){
            intent = new Intent(this, ChatbotActivity.class);
        }
        startActivity(intent);
        return super.onOptionsItemSelected(item);
    }

    private void goToForgotPassword() {
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        startActivity(intent);
    }
}
