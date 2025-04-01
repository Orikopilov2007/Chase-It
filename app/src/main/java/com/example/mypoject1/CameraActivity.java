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

import java.io.OutputStream;

public class CameraActivity extends Activity {

    private static final int CAMERA_REQUEST_CODE = 200;
    private Uri imageUri;
    private ImageView ivCapturedPhoto;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 100);
        }


        ivCapturedPhoto = findViewById(R.id.ivCapturedPhoto);

        // Prepare to capture the photo
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        ImageButton btnBackCamera = findViewById(R.id.btnBack);
        btnBackCamera.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish(); // Close the activity on back button press
        });


        if (imageUri != null) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri); // Save photo to the URI
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        } else {
            Toast.makeText(this, "Unable to access storage", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            try {
                // Display the captured photo in the ImageView
                Bitmap photo = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                ivCapturedPhoto.setImageBitmap(photo);

                // Pass the photo URI back to the calling activity
                Intent resultIntent = new Intent();
                resultIntent.setData(imageUri); // Set the photo URI
                setResult(RESULT_OK, resultIntent);
            } catch (Exception e) {
                //Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
            }
        } else {
            Toast.makeText(this, "Photo capture canceled", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
        }
        finish(); // Close CameraActivity
    }
}
