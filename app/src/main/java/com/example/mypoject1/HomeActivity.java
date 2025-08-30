package com.example.mypoject1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mypoject1.databinding.ActivityHomeBinding;
import com.google.firebase.auth.FirebaseAuth;

/**
 * HomeActivity
 * <p>
 * This activity serves as the main hub after a user logs in. It displays a personalized welcome message and
 * provides navigation to various parts of the application (User Details, AI Chatbot, Timer, and Logout).
 * The activity uses view binding for improved type safety and performance while inflating its layout.
 * </p>
 * <p>
 * Key Features and Flow:
 * <ul>
 *   <li>Retrieves the user's first name from SharedPreferences and shows a welcome message.</li>
 *   <li>If no user data is available, redirects to the MainActivity.</li>
 *   <li>Starts various animations on the UI elements to enhance the user experience.</li>
 *   <li>Handles click events to navigate to other activities or perform logout.</li>
 *   <li>Updates the welcome message on resume if the stored first name changes.</li>
 *   <li>Provides an options menu to navigate to Home, Timer, User Details, Chatbot, or to logout.</li>
 * </ul>
 * </p>
 */
public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    // View binding instance for easy access to layout views
    private ActivityHomeBinding binding;

    /**
     * onCreate
     * <p>
     * Called when the activity is first created. This method initializes the view binding, checks for stored
     * user data, sets up animations for UI elements, and registers click listeners for navigation buttons.
     * </p>
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Using view binding to inflate the layout
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Retrieve firstName from SharedPreferences to personalize the welcome message.
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String firstName = prefs.getString("firstName", null);

        // If no user information is found, redirect to MainActivity (login screen) and finish this activity.
        if (firstName == null || firstName.isEmpty()) {
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Set the welcome message using the retrieved first name.
        binding.welcomeTextView.setText("Welcome " + firstName + "!");

        // Load animations from resources to create a dynamic and smooth user interface.
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        // Apply fade in animation to the main layout.
        binding.main.startAnimation(fadeIn);

        Animation zoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
        // Apply zoom in animation to the welcome text.
        binding.welcomeTextView.startAnimation(zoomIn);

        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        // Apply slide in animation to the navigation buttons.
        binding.btnHomeUserDetails.startAnimation(slideIn);
        binding.btnAI.startAnimation(slideIn);
        binding.btnTimer.startAnimation(slideIn);
        binding.btnLogout.startAnimation(slideIn);

        // Register click listeners for buttons using view binding.
        binding.btnHomeUserDetails.setOnClickListener(this);
        binding.btnAI.setOnClickListener(this);
        binding.btnTimer.setOnClickListener(this);
        binding.btnLogout.setOnClickListener(this);

        //making sure that pressing on this button does nothing in this page
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });
    }

    /**
     * onClick
     * <p>
     * Handles click events for all navigation buttons. When a button is clicked, a button press animation is played.
     * Once the animation ends, the appropriate navigation or action is executed.
     * </p>
     *
     * @param view The view that received the click event.
     */
    @Override
    public void onClick(View view) {
        // Load a button press animation from resources.
        Animation buttonPress = AnimationUtils.loadAnimation(this, R.anim.button_press);
        // Start the button press animation on the clicked view.
        view.startAnimation(buttonPress);

        // Set an animation listener to perform actions after the animation completes.
        buttonPress.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // No operation needed when the animation starts.
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Intent intent = null;
                // Check which button was clicked using view references.
                if (view == binding.btnHomeUserDetails) {
                    // Navigate to UserDetailsActivity if the "User Details" button was clicked.
                    intent = new Intent(HomeActivity.this, SettingsActivity.class);
                } else if (view == binding.btnAI) {
                    // Navigate to ChatbotActivity if the "AI" button was clicked.
                    intent = new Intent(HomeActivity.this, ChatbotActivity.class);
                } else if (view == binding.btnTimer) {
                    // Navigate to TimerActivity if the "Timer" button was clicked.
                    intent = new Intent(HomeActivity.this, RunningActivity.class);
                } else if (view == binding.btnLogout) {
                    // Call the logout() method to clear user data and sign out, then return to avoid navigation.
                    logout();
                    return;
                }
                // If an intent was set, start the corresponding activity.
                if (intent != null) {
                    startActivity(intent);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // No operation needed on animation repeat.
            }
        });
    }

    /**
     * onResume
     * <p>
     * Called when the activity resumes. Retrieves the updated first name from SharedPreferences and updates
     * the welcome message accordingly.
     * </p>
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Retrieve the latest first name from SharedPreferences.
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String firstName = prefs.getString("firstName", "");
        // Update welcome message if a valid first name is present.
        if (!firstName.isEmpty()) {
            binding.welcomeTextView.setText("Welcome " + firstName + "!");
        }
    }

    /**
     * logout
     * <p>
     * Clears stored user data in SharedPreferences and signs the user out from Firebase Authentication.
     * Redirects the user to the MainActivity (login screen) and finishes the current activity.
     * </p>
     */
    private void logout() {
        // Clear SharedPreferences to remove any stored user-specific data.
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        // Sign out from Firebase Authentication.
        FirebaseAuth.getInstance().signOut();

        // Redirect to the MainActivity (login screen) and finish HomeActivity.
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
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
     * onOptionsItemSelected
     * <p>
     * Handles click events on the options menu items. Depending on the selected item,
     * the activity navigates to the corresponding screen or executes logout.
     * </p>
     *
     * @param item The selected MenuItem.
     * @return true if the selection was handled.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        // Determine which menu item was selected.
        if (item.getItemId() == R.id.menu_home) {
            // Re-launch HomeActivity.
            intent = new Intent(this, HomeActivity.class);
        } else if (item.getItemId() == R.id.menu_logout) {
            // Log out if the logout option is selected.
            logout();
            return true;
        } else if (item.getItemId() == R.id.menu_timer) {
            // Navigate to TimerActivity.
            intent = new Intent(this, RunningActivity.class);
        } else if (item.getItemId() == R.id.menu_userdetails) {
            // Navigate to UserDetailsActivity.
            intent = new Intent(this, SettingsActivity.class);
        } else if (item.getItemId() == R.id.menu_ChatBot) {
            // Navigate to ChatbotActivity.
            intent = new Intent(this, ChatbotActivity.class);
        }
        // Start the corresponding activity if an intent was set.
        if (intent != null) {
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    //making sure that pressing on this button does nothing in this page
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }


}
