package com.example.mypoject1;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

/**
 * ZoomableImageActivity displays an image that can be viewed in the userDetailsActivity.
 * <p>
 * The activity retrieves an image URI passed via an Intent, loads the image using Glide
 * into a PhotoView for zoom functionality, and provides a back button to return to the userDetailsActivity.
 * </p>
 */
public class ZoomableImageActivity extends AppCompatActivity {

    // PhotoView provides built-in zooming and panning for images.
    private PhotoView photoView;
    // Back button for closing the activity.
    private ImageButton btnBack;

    /**
     * Called when the activity is first created.
     * <p>
     * This method sets the layout, initializes the PhotoView and back button, sets the click listener
     * for the back button, and loads the image specified by the image URI passed in the Intent.
     * </p>
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout for this activity.
        setContentView(R.layout.activity_zoomable_image);

        // Initialize the PhotoView and back button from the layout.
        photoView = findViewById(R.id.photoView);
        btnBack = findViewById(R.id.btnBack);

        // Set a click listener for the back button to finish the activity when pressed.
        btnBack.setOnClickListener(v -> finish());

        // Retrieve the image URI passed via the Intent.
        String imageUri = getIntent().getStringExtra("imageUri");
        if (imageUri != null) {
            // Use Glide to load the image into the PhotoView.
            Glide.with(this)
                    .load(imageUri)
                    .into(photoView);
        }
    }

    /**
     * onCreateOptionsMenu
     * <p>
     * Inflates the options menu from the XML resource file, adding menu items to the toolbar.
     * </p>
     *
     * @param menu The menu to populate with items.
     * @return true if the menu was successfully created.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Obtain a MenuInflater and inflate the menu resource into the provided menu.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * Handles selection of options menu items.
     * <p>
     * Depending on the selected menu item, this method creates an Intent to navigate to a different activity.
     * For example, selecting the home menu item navigates to HomeActivity, while selecting logout navigates
     * to MainActivity and finishes this activity.
     * </p>
     *
     * @param item The selected menu item.
     * @return True if the event was handled.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        // Check the ID of the selected menu item and create the appropriate Intent.
        if (item.getItemId() == R.id.menu_home) {
            intent = new Intent(this, HomeActivity.class);
        } else if (item.getItemId() == R.id.menu_logout) {
            intent = new Intent(this, MainActivity.class);
            // Finish this activity if the user logs out.
            finish();
        } else if (item.getItemId() == R.id.menu_timer) {
            intent = new Intent(this, RunningActivity.class);
        } else if (item.getItemId() == R.id.menu_userdetails) {
            intent = new Intent(this, SettingsActivity.class);
        } else if (item.getItemId() == R.id.menu_ForgotPassword) {
            intent = new Intent(this, ForgotPasswordActivity.class);
        } else if(item.getItemId() == R.id.menu_ChatBot){
            intent = new Intent(this, ChatbotActivity.class);
        }
        // If an Intent was created, start the corresponding activity.
        if (intent != null) {
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
