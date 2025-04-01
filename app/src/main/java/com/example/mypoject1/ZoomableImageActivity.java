package com.example.mypoject1;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

public class ZoomableImageActivity extends AppCompatActivity {

    private PhotoView photoView;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zoomable_image);

        photoView = findViewById(R.id.photoView);
        btnBack = findViewById(R.id.btnBack);

        // Set back button listener to finish this activity
        btnBack.setOnClickListener(v -> finish());

        // Retrieve the image URI passed in the Intent
        String imageUri = getIntent().getStringExtra("imageUri");
        if (imageUri != null) {
            // Use Glide to load the image into the PhotoView
            Glide.with(this)
                    .load(imageUri)
                    .into(photoView);
        }
    }
}
