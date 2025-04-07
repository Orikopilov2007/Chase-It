package com.example.mypoject1;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        if (item.getItemId() == R.id.menu_home) {
            intent = new Intent(this, HomeActivity.class);
        } else if (item.getItemId() == R.id.menu_logout) {
            intent = new Intent(this, MainActivity.class);
            finish();
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
}
