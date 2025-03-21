package com.example.mypoject1;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.github.chrisbanes.photoview.PhotoView;

public class ZoomableImageActivity extends AppCompatActivity {

    PhotoView photoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zoomable_image);

        photoView = findViewById(R.id.photoView);

        Intent intent = getIntent();
        String imageUri = intent.getStringExtra("imageUri");

        if (imageUri != null) {
            photoView.setImageDrawable(Drawable.createFromPath(Uri.parse(imageUri).getPath()));

        }
    }
}
