package com.example.mypoject1;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * CameraActivity handles capturing a photo using the device camera.
 * It manages camera permissions, photo capture, and passing back the captured image URI.
 */
public class CameraActivity extends Activity {

    private static final int CAMERA_REQUEST_CODE = 200;
    private Uri imageUri;
    private ImageView ivCapturedPhoto;

    /**
     * Called when the activity is starting.
     * Sets up the camera capture process and checks for camera permissions.
     *
     * @param savedInstanceState Bundle with saved state, if any.
     */
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Check for camera permission; request if not granted.
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 100);
        }

        // Initialize the ImageView for displaying the captured photo.
        ivCapturedPhoto = findViewById(R.id.ivCapturedPhoto);

        // Prepare a ContentValues object to store image metadata.
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");

        // Insert into MediaStore to get a URI for the captured image.
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        // Set up the back button to cancel and exit the activity.
        ImageButton btnBackCamera = findViewById(R.id.btnBack);
        btnBackCamera.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        // Start the camera intent if a valid URI is obtained.
        if (imageUri != null) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        } else {
            Toast.makeText(this, "Unable to access storage", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Receives the result from the camera capture intent.
     * Displays the captured photo and returns the photo URI to the calling activity.
     *
     * @param requestCode The request code originally supplied to startActivityForResult().
     * @param resultCode  The result code returned by the child activity.
     * @param data        An Intent containing any additional data.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            try {
                // Retrieve the captured photo from the given URI and display it.
                Bitmap photo = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                ivCapturedPhoto.setImageBitmap(photo);

                // Create an intent to pass the photo URI back to the calling activity.
                Intent resultIntent = new Intent();
                resultIntent.setData(imageUri);
                setResult(RESULT_OK, resultIntent);
            } catch (Exception e) {
                setResult(RESULT_CANCELED);
            }
        } else {
            Toast.makeText(this, "Photo capture canceled", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
        }
        finish();
    }
}
