package com.example.mypoject1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

// API usage: Google Maps SDK and Location Services API are imported for map display and location updates.
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;

// API usage: Firebase Firestore API is imported to store and retrieve workout summary data remotely.
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * TimerActivity
 * <p>
 * This activity acts as a central hub for workout timing, route tracking using maps, and sensor-based activity
 * monitoring (such as step counting and accelerometer readings). It also supports starting/stopping a stopwatch and
 * a countdown timer. Additionally, it integrates with Firebase Firestore for storing workout summaries and with
 * Google Maps API for location updates.
 * </p>
 * <p>
 * Major Functions and Features:
 * <ul>
 *   <li><b>Lifecycle Management:</b> onCreate, onResume, onPause, onDestroy, onLowMemory are overridden to manage
 *       sensors and MapView lifecycle.</li>
 *   <li><b>UI Initialization:</b> findViews(), applying animations, and setting click listeners.</li>
 *   <li><b>Map Integration:</b> onMapReady, requestFreshLocation, and moveToDefaultLocation are used to set the user's
 *       location and update the map camera.</li>
 *   <li><b>Sensor Management:</b> onSensorChanged and onAccuracyChanged handle step counter and accelerometer data,
 *       while updateSteps() and updateDistance() update UI elements.</li>
 *   <li><b>Stopwatch and Countdown Timer:</b> Methods for starting, pausing, resetting, and updating the stopwatch and
 *       timer are provided.</li>
 *   <li><b>Workout Summary and Sharing:</b> Methods to show a workout summary, store it in Firebase, and share it via
 *       implicit intents.</li>
 *   <li><b>Asynchronous Tasks:</b> AverageSpeedAnalysisTask computes average speed in the background using AsyncTask.</li>
 * </ul>
 * </p>
 */
public class RunningActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener, OnMapReadyCallback {

    // Logging tag for debug messages
    private static final String TAG = "TimerActivity";

    // UI Elements
    Button btnBack, btnStartStopwatch, btnResetStopwatch, btnStartTimer, btnResetTimer;
    Button btnFinishWorkout, btnCloseDialog, btnShareSummary;
    EditText etTimeInput;
    TextView tvWorkoutSummary, tvTotalSteps, tvTotalDistance, tvAverageSpeed;
    LinearLayout workoutSummaryDialog;
    TextView tvStopwatch, tvCountDown, tvSteps, tvDistance;

    // Map and Firebase objects
    private MapView mapView;
    private GoogleMap gMap;
    private FirebaseFirestore db;

    // Route tracking fields for mapping the workout route
    private List<LatLng> routePoints = new ArrayList<>();
    private Polyline routePolyline;

    // Stopwatch and Timer Variables
    boolean isStopwatchRunning = false;
    long stopwatchTime = 0, stopwatchStartTime = 0;
    boolean isTimerRunning = false;
    long timeRemain = 0, millisAtPause = 0;
    Handler timerHandler = new Handler();
    Runnable countdownRunnable, stopwatchRunnable;

    // Sensor-related fields
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private Sensor accelerometerSensor;
    private float initialStepCount = -1;
    int stepCount = 0;
    double distanceCovered = 0.0;

    // Storage for accelerometer data (for advanced analysis)
    private List<float[]> accelerometerData = new ArrayList<>();
    long totalElapsedTime = 0;
    private float latestSensorReading = -1;

    /**
     * onCreate
     * <p>
     * Called when the activity is first created. This method initializes the UI components, maps, sensors,
     * Firebase Firestore, permissions, and animations. It also sets up event listeners for button clicks.
     * </p>
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting TimerActivity");
        // Set the layout for this activity
        setContentView(R.layout.activity_running);

        // Initialize Firebase Firestore for storing workout data
        db = FirebaseFirestore.getInstance();

        // Initialize Google Maps SDK
        try {
            MapsInitializer.initialize(this);
            Log.d(TAG, "MapsInitializer.initialize succeeded");
        } catch (Exception e) {
            Log.e(TAG, "MapsInitializer.initialize failed", e);
        }

        // Adjust layout padding to account for system bars (status bar, navigation bar, etc.)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // Set padding based on insets
            v.setPadding(
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        // Find and initialize all UI elements from the layout
        findViews();

        // Load UI animations from resources and apply to key components for enhanced user experience
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        View mainLayout = findViewById(R.id.main);
        mainLayout.startAnimation(fadeIn);
        btnBack.startAnimation(slideIn);
        btnStartStopwatch.startAnimation(slideIn);
        btnResetStopwatch.startAnimation(slideIn);
        btnStartTimer.startAnimation(slideIn);
        btnResetTimer.startAnimation(slideIn);

        // Set click listeners for interactive buttons
        btnBack.setOnClickListener(this);
        btnStartStopwatch.setOnClickListener(this);
        btnResetStopwatch.setOnClickListener(this);
        btnStartTimer.setOnClickListener(this);
        btnResetTimer.setOnClickListener(this);
        btnFinishWorkout.setOnClickListener(this);
        btnCloseDialog.setOnClickListener(this);
        btnShareSummary.setOnClickListener(this);

        // Request necessary runtime permissions for sensor and location data
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting ACTIVITY_RECOGNITION permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting ACCESS_FINE_LOCATION permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
        }

        // Initialize stopwatch and timer displays to default values
        updateStopwatchText(0);
        updateCountText(0);
        updateSteps();
        updateDistance();

        // Initialize sensors: step counter and accelerometer for tracking user activity
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Log.d(TAG, "Step counter sensor: " + (stepCounterSensor != null ? "available" : "not available"));
            Log.d(TAG, "Accelerometer sensor: " + (accelerometerSensor != null ? "available" : "not available"));
        }
        if (stepCounterSensor == null) {
            Log.d(TAG, "Step counter sensor not available.");
        }

        // Initialize MapView for showing location on a map
        if (mapView == null) {
            Log.e(TAG, "mapView is null in onCreate!");
        } else {
            Log.d(TAG, "Initializing MapView");
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }
    }

    /**
     * findViews
     * <p>
     * Finds and initializes all view components defined in the layout using findViewById.
     * </p>
     */
    private void findViews() {
        // Locate all relevant UI elements and assign them to member variables
        btnBack = findViewById(R.id.btnBack);
        btnStartStopwatch = findViewById(R.id.btnStartStopwatch);
        btnResetStopwatch = findViewById(R.id.btnResetStopwatch);
        btnStartTimer = findViewById(R.id.btnStartTimer);
        btnResetTimer = findViewById(R.id.btnResetTimer);
        etTimeInput = findViewById(R.id.etTimeInput);
        tvStopwatch = findViewById(R.id.tvStopwatch);
        tvCountDown = findViewById(R.id.tvCountDown);
        tvSteps = findViewById(R.id.tvSteps);
        tvDistance = findViewById(R.id.tvDistance);
        btnFinishWorkout = findViewById(R.id.btnFinishWorkout);
        workoutSummaryDialog = findViewById(R.id.workoutSummaryDialog);
        tvWorkoutSummary = findViewById(R.id.tvWorkoutSummary);
        tvTotalSteps = findViewById(R.id.tvTotalSteps);
        tvTotalDistance = findViewById(R.id.tvTotalDistance);
        tvAverageSpeed = findViewById(R.id.tvAverageSpeed);
        btnCloseDialog = findViewById(R.id.btnCloseDialog);
        btnShareSummary = findViewById(R.id.btnShareSummary);
        mapView = findViewById(R.id.mapView);
        // Log the success or failure of mapView retrieval
        if (mapView == null) {
            Log.e(TAG, "mapView not found in layout!");
        } else {
            Log.d(TAG, "mapView successfully found in layout");
        }
    }

    /**
     * onResume
     * <p>
     * Called when the activity resumes. Registers sensor listeners and resumes the MapView.
     * </p>
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Register step counter sensor listener with highest possible update rate
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_FASTEST);
            Log.d(TAG, "Sensor listener registered");
        }
        // Register accelerometer sensor listener with default update rate
        if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "Accelerometer sensor listener registered");
        }
        // Resume the MapView when activity becomes visible
        if (mapView != null) {
            Log.d(TAG, "Calling mapView.onResume()");
            mapView.onResume();
        }
    }

    /**
     * onPause
     * <p>
     * Called when the activity is paused. Unregisters sensor listeners and pauses the MapView.
     * </p>
     */
    @Override
    protected void onPause() {
        super.onPause();
        // Unregister all sensor listeners to conserve resources
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            Log.d(TAG, "Sensor listener unregistered");
        }
        // Pause the MapView to avoid unnecessary updates when activity is not visible
        if (mapView != null) {
            Log.d(TAG, "Calling mapView.onPause()");
            mapView.onPause();
        }
    }

    /**
     * onDestroy
     * <p>
     * Called when the activity is being destroyed. Cleans up the MapView resources.
     * </p>
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Properly destroy MapView to avoid memory leaks
        if (mapView != null) {
            Log.d(TAG, "Calling mapView.onDestroy()");
            mapView.onDestroy();
        }
    }

    /**
     * onLowMemory
     * <p>
     * Called when the system is running low on memory.
     * Passes this signal to the MapView for further cleanup.
     * </p>
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            Log.d(TAG, "Calling mapView.onLowMemory()");
            mapView.onLowMemory();
        }
    }

    /**
     * onMapReady
     * <p>
     * Callback for when the MapView is ready. Sets up the GoogleMap, enables MyLocation,
     * and requests a fresh location update using the FusedLocationProviderClient.
     * </p>
     *
     * @param googleMap The GoogleMap instance that is now ready.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: Map is ready");
        // Save the GoogleMap instance
        gMap = googleMap;
        // Get the FusedLocationProviderClient for location updates
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            // Check for location permission before enabling MyLocation on the map
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                gMap.setMyLocationEnabled(true);
                Log.d(TAG, "MyLocation enabled on map");
                // Request a fresh location update to center the map on the current location
                requestFreshLocation(fusedLocationClient);
                // Override the MyLocation button click to always request an updated location
                gMap.setOnMyLocationButtonClickListener(() -> {
                    Log.d(TAG, "MyLocation button clicked. Requesting fresh location.");
                    requestFreshLocation(fusedLocationClient);
                    return true;
                });
            } else {
                Log.d(TAG, "ACCESS_FINE_LOCATION permission not granted");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in onMapReady", e);
        }
    }

    /**
     * requestFreshLocation
     * <p>
     * Requests a single high-accuracy location update from the FusedLocationProviderClient.
     * On receiving an update, moves the map camera to the new location.
     * </p>
     *
     * @param fusedLocationClient The FusedLocationProviderClient used for location updates.
     */
    private void requestFreshLocation(FusedLocationProviderClient fusedLocationClient) {
        // Create a high-accuracy location request with a 500ms interval and only one update
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(500);
        locationRequest.setNumUpdates(1);

        // Create a callback for receiving location updates
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                // Check if the location result is null
                if (locationResult == null) {
                    Log.d(TAG, "Location result is null. Falling back to default location.");
                    moveToDefaultLocation();
                    return;
                }
                // Get the last location from the result
                android.location.Location location = locationResult.getLastLocation();
                if (location != null) {
                    // Create a LatLng object from the location and update the map camera
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    Log.d(TAG, "Fresh location coordinates: " + currentLatLng.toString());
                    gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f));
                    Log.d(TAG, "Camera moved to fresh location: " + currentLatLng.toString());
                } else {
                    Log.d(TAG, "Location is null. Falling back to default location.");
                    moveToDefaultLocation();
                }
            }
        };

        // Ensure location permission is available before requesting updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    /**
     * moveToDefaultLocation
     * <p>
     * Moves the map camera to a predefined default location.
     * Useful when a fresh location update is not available.
     * </p>
     */
    private void moveToDefaultLocation() {
        // Define a default location (latitude and longitude)
        LatLng defaultLocation = new LatLng(32.072550, 34.811370);
        // Move the map camera to the default location with zoom level 18
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 18f));
        Log.d(TAG, "Camera moved to default location: " + defaultLocation.toString());
    }

    /**
     * onSensorChanged
     * <p>
     * Called when sensor values have changed.
     * Handles step counting and accelerometer events.
     * For step counter: updates the step count and distance covered.
     * For accelerometer: collects data for potential later analysis.
     * </p>
     *
     * @param event The SensorEvent containing sensor data.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Process step counter sensor events
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            // Update the latest sensor reading
            latestSensorReading = event.values[0];
            // Set the initial step count on the first reading
            if (initialStepCount == -1) {
                initialStepCount = event.values[0];
            }
            // Compute the current step count
            stepCount = (int) (event.values[0] - initialStepCount);
            // Estimate distance (assuming average step length of 0.8 m)
            distanceCovered = stepCount * 0.8;
            // Update UI elements for steps and distance
            updateSteps();
            updateDistance();
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Add accelerometer readings for later asynchronous analysis (advanced data processing)
            accelerometerData.add(event.values.clone());
        }
    }

    /**
     * onAccuracyChanged
     * <p>
     * Called when the accuracy of a sensor changes.
     * Not used in this activity; thus, no action is taken.
     * </p>
     *
     * @param sensor   The sensor whose accuracy changed.
     * @param accuracy The new accuracy value.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No action needed for this use case.
    }

    /**
     * updateSteps
     * <p>
     * Updates the TextView displaying the current step count.
     * </p>
     */
    private void updateSteps() {
        // Display the current step count on the screen
        tvSteps.setText("Steps: " + stepCount);
    }

    /**
     * updateDistance
     * <p>
     * Updates the TextView displaying the estimated distance covered.
     * </p>
     */
    private void updateDistance() {
        // Display the calculated distance in meters with two decimal precision
        tvDistance.setText("Distance: " + String.format(Locale.getDefault(), "%.2f", distanceCovered) + " m");
    }

    /**
     * startStopwatch
     * <p>
     * Starts the stopwatch by recording the start time, setting the running flag,
     * updating the start button text, and beginning the periodic update routine.
     * </p>
     */
    private void startStopwatch() {
        // Record current system time as the start time
        stopwatchStartTime = System.currentTimeMillis();
        isStopwatchRunning = true;
        // Update UI button to show the option to pause
        btnStartStopwatch.setText("Pause");
        // Begin running the stopwatch update routine
        runStopwatch();
        // Start any additional counters if necessary
        startCounters();
    }

    /**
     * pauseStopwatch
     * <p>
     * Pauses the stopwatch by adding the elapsed time to a cumulative total,
     * updating the UI button text, and removing scheduled runnable tasks.
     * </p>
     */
    private void pauseStopwatch() {
        // Calculate and add elapsed time since start to total stopwatch time
        stopwatchTime += System.currentTimeMillis() - stopwatchStartTime;
        isStopwatchRunning = false;
        // Update UI button to allow resume
        btnStartStopwatch.setText("Resume");
        // Stop the stopwatch update routine
        timerHandler.removeCallbacks(stopwatchRunnable);
        // Stop any active counters
        stopCounters();
    }

    /**
     * resetStopwatch
     * <p>
     * Resets the stopwatch by clearing the stored time, updating the UI display,
     * and stopping any currently running update routines.
     * </p>
     */
    private void resetStopwatch() {
        // Reset stored time and elapsed time counters
        stopwatchTime = 0;
        totalElapsedTime = 0;
        updateStopwatchText(0);
        isStopwatchRunning = false;
        // Reset the UI button text
        btnStartStopwatch.setText("Start");
        // Stop the stopwatch routine if it is running
        stopCounters();
    }

    /**
     * runStopwatch
     * <p>
     * Initiates a periodic task using a Handler to update the stopwatch display.
     * Calculates elapsed time and updates the TextView every 10 milliseconds.
     * </p>
     */
    private void runStopwatch() {
        // Define a runnable that updates the stopwatch display
        stopwatchRunnable = new Runnable() {
            @Override
            public void run() {
                if (isStopwatchRunning) {
                    // Compute elapsed milliseconds including previously elapsed time
                    long elapsedMillis = System.currentTimeMillis() - stopwatchStartTime + stopwatchTime;
                    // Update the stopwatch display with the formatted time
                    updateStopwatchText(elapsedMillis);
                    totalElapsedTime = elapsedMillis;
                    // Schedule the next update in 10 milliseconds
                    timerHandler.postDelayed(this, 10);
                }
            }
        };
        // Start the runnable immediately
        timerHandler.post(stopwatchRunnable);
    }

    /**
     * updateStopwatchText
     * <p>
     * Formats the elapsed time in hours, minutes, seconds, and hundredths of a second,
     * then updates the corresponding TextView.
     * </p>
     *
     * @param elapsedMillis The elapsed time in milliseconds.
     */
    private void updateStopwatchText(long elapsedMillis) {
        // Convert milliseconds into hours, minutes, seconds and fractions
        int hours = (int) (elapsedMillis / 3600000);
        int minutes = (int) (elapsedMillis / 60000) % 60;
        int seconds = (int) (elapsedMillis / 1000) % 60;
        int milliseconds = (int) (elapsedMillis % 1000) / 10;
        // Format the time string using locale-specific formatting
        String timeFormat = String.format(Locale.getDefault(), "%02d:%02d:%02d.%02d", hours, minutes, seconds, milliseconds);
        // Update the stopwatch TextView with the formatted string
        tvStopwatch.setText(timeFormat);
    }

    /**
     * setTimeFromInput
     * <p>
     * Parses the user input from the EditText field to set the remaining time for the countdown timer.
     * </p>
     */
    private void setTimeFromInput() {
        String input = etTimeInput.getText().toString();
        // Ensure that input is provided before parsing to avoid errors
        if (!input.isEmpty()) {
            // Convert input from seconds to milliseconds
            int seconds = Integer.parseInt(input);
            timeRemain = seconds * 1000L;
            // Update the countdown display with the new time
            updateCountText(timeRemain);
        }
    }

    /**
     * resetTimer
     * <p>
     * Resets the countdown timer by stopping any running tasks, zeroing counters,
     * and updating the UI to reflect the reset state.
     * </p>
     */
    private void resetTimer() {
        // Remove any pending countdown runnable tasks
        if (countdownRunnable != null) {
            timerHandler.removeCallbacks(countdownRunnable);
        }
        // Reset timer variables
        timeRemain = 0;
        millisAtPause = 0;
        updateCountText(0);
        isTimerRunning = false;
        // Update the UI button to indicate the timer is ready to start
        btnStartTimer.setText("Start");
        stopCounters();
    }

    /**
     * startTimer
     * <p>
     * Starts the countdown timer using a Handler to decrement the remaining time at a 10ms interval.
     * Displays a Toast when the timer finishes.
     * </p>
     */
    private void startTimer() {
        // Define a runnable task to update the countdown timer
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (timeRemain > 0) {
                    // Reduce the remaining time and update the display
                    timeRemain -= 10;
                    updateCountText(timeRemain);
                    timerHandler.postDelayed(this, 10);
                } else {
                    // Timer has finished; update UI and notify the user
                    isTimerRunning = false;
                    updateCountText(0);
                    btnStartTimer.setText("Start");
                    Toast.makeText(RunningActivity.this, "Time's up! Great job!", Toast.LENGTH_LONG).show();
                    stopCounters();
                }
            }
        };
        // Start the runnable task
        timerHandler.post(countdownRunnable);
        isTimerRunning = true;
        btnStartTimer.setText("Pause");
        startCounters();
    }

    /**
     * pauseTimer
     * <p>
     * Pauses the countdown timer by removing scheduled tasks, saving the remaining time,
     * and updating the UI button to allow resuming.
     * </p>
     */
    private void pauseTimer() {
        if (countdownRunnable != null) {
            timerHandler.removeCallbacks(countdownRunnable);
        }
        // Save the current remaining time for later resumption
        millisAtPause = timeRemain;
        isTimerRunning = false;
        btnStartTimer.setText("Resume");
        stopCounters();
    }

    /**
     * updateCountText
     * <p>
     * Formats the remaining countdown time and displays it in the countdown TextView.
     * </p>
     *
     * @param millisUntilFinished The remaining time in milliseconds.
     */
    private void updateCountText(long millisUntilFinished) {
        // Convert milliseconds into minutes, seconds, and hundredths
        int minutes = (int) (millisUntilFinished / 1000) / 60;
        int seconds = (int) (millisUntilFinished / 1000) % 60;
        int milliseconds = (int) (millisUntilFinished % 1000) / 10;
        // Create a formatted time string
        String timeFormat = String.format(Locale.getDefault(), "%02d:%02d:%02d", minutes, seconds, milliseconds);
        // Update the countdown TextView
        tvCountDown.setText(timeFormat);
    }

    /**
     * startCounters
     * <p>
     * Prepares the counters used during the workout.
     * Currently ensures step count is valid.
     * </p>
     */
    private void startCounters() {
        if (isStopwatchRunning || isTimerRunning) {
            // If no initial step count is set, default it to 0
            if (initialStepCount < 0) {
                initialStepCount = 0;
            }
        }
    }

    /**
     * stopCounters
     * <p>
     * Updates the UI counters (steps and distance) after a workout is paused or finished.
     * </p>
     */
    private void stopCounters() {
        updateSteps();
        updateDistance();
    }

    /**
     * onClick
     * <p>
     * Handles button click events. Determines the clicked view and executes the corresponding method.
     * Options include navigation, stopwatch and timer control, finishing a workout, and sharing the workout summary.
     * </p>
     *
     * @param view The view that was clicked.
     */
    @Override
    public void onClick(View view) {
        // Check which button was clicked and perform the corresponding action
        if (view == btnBack) {
            // Navigate back to HomeActivity
            Intent intent = new Intent(RunningActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        } else if (view == btnStartStopwatch) {
            // Start or pause stopwatch based on its current state (if timer is not running)
            if (!isStopwatchRunning && !isTimerRunning) {
                startStopwatch();
            } else if (isStopwatchRunning) {
                pauseStopwatch();
            }
        } else if (view == btnResetStopwatch) {
            // Reset stopwatch counters and UI display
            resetStopwatch();
        } else if (view == btnStartTimer) {
            // Start or pause timer based on its state
            if (!isTimerRunning && !isStopwatchRunning) {
                if (timeRemain == 0) {
                    // If no time is set, read the input value
                    setTimeFromInput();
                }
                startTimer();
            } else if (isTimerRunning) {
                pauseTimer();
            }
        } else if (view == btnResetTimer) {
            // Reset the timer
            resetTimer();
        } else if (view == btnFinishWorkout) {
            // Display the workout summary dialog, store workout data remotely, and calculate average speed
            showWorkoutSummary();
            storeWorkoutSummary();
            new AverageSpeedAnalysisTask().execute(); // AsyncTask for background average speed calculation
        } else if (view == btnCloseDialog) {
            // Close the summary dialog and reset workout data for a new session
            closeWorkoutSummaryDialog();
        } else if (view == btnShareSummary) {
            // Share the workout summary using an implicit intent
            shareWorkoutSummary();
        }
    }

    /**
     * showWorkoutSummary
     * <p>
     * Displays the workout summary dialog, updates the summary UI elements with current workout data,
     * and disables the main layout to prevent further interaction.
     * </p>
     */
    private void showWorkoutSummary() {
        // Update summary TextViews with the total steps, distance, and elapsed time
        tvTotalSteps.setText("Total Steps: " + stepCount);
        tvTotalDistance.setText("Total Distance: " + String.format(Locale.getDefault(), "%.2f", distanceCovered) + " m");
        tvWorkoutSummary.setText("Elapsed Time: " + tvStopwatch.getText().toString());
        // Show the summary dialog with a fade-in effect
        workoutSummaryDialog.setVisibility(View.VISIBLE);
        workoutSummaryDialog.setAlpha(0f);
        workoutSummaryDialog.animate().alpha(1f).setDuration(300);
        disableMainLayout();
    }

    /**
     * closeWorkoutSummaryDialog
     * <p>
     * Closes the workout summary dialog with an animation. Resets workout counters, clears sensor data,
     * and re-enables the main layout for a new workout.
     * </p>
     */
    private void closeWorkoutSummaryDialog() {
        // Animate the dialog fade-out and then set visibility to gone
        workoutSummaryDialog.animate().alpha(0f).setDuration(300).withEndAction(() -> workoutSummaryDialog.setVisibility(View.GONE));
        // Reset the initial step count with the latest sensor reading if available
        if (latestSensorReading != -1) {
            initialStepCount = latestSensorReading;
        }
        // Reset workout metrics
        stepCount = 0;
        distanceCovered = 0.0;
        routePoints.clear();
        if (routePolyline != null) {
            routePolyline.remove();
        }
        tvTotalSteps.setText("Total Steps: 0");
        tvTotalDistance.setText("Total Distance: 0.0 m");
        updateDistance();
        updateSteps();
        // Re-enable main layout controls after closing the dialog
        enableMainLayout();
        // Clear any stored accelerometer data for the next workout
        accelerometerData.clear();
    }

    /**
     * disableMainLayout
     * <p>
     * Disables key UI elements to prevent user interaction during specific operations (e.g., while showing workout summary).
     * </p>
     */
    private void disableMainLayout() {
        // Disable various UI components by setting their enabled state to false
        findViewById(R.id.tvTitle).setEnabled(false);
        findViewById(R.id.tvTimerTitle).setEnabled(false);
        findViewById(R.id.tvCountDown).setEnabled(false);
        findViewById(R.id.etTimeInput).setEnabled(false);
        findViewById(R.id.btnStartTimer).setEnabled(false);
        findViewById(R.id.btnResetTimer).setEnabled(false);
        findViewById(R.id.tvStopwatchTitle).setEnabled(false);
        findViewById(R.id.tvStopwatch).setEnabled(false);
        findViewById(R.id.btnStartStopwatch).setEnabled(false);
        findViewById(R.id.btnResetStopwatch).setEnabled(false);
        findViewById(R.id.btnFinishWorkout).setEnabled(false);
        findViewById(R.id.tvSteps).setEnabled(false);
        findViewById(R.id.tvDistance).setEnabled(false);
        findViewById(R.id.btnBack).setEnabled(false);
    }

    /**
     * enableMainLayout
     * <p>
     * Re-enables key UI elements for user interaction after a process has completed.
     * </p>
     */
    private void enableMainLayout() {
        // Enable various UI components by setting their enabled state to true
        findViewById(R.id.tvTitle).setEnabled(true);
        findViewById(R.id.tvTimerTitle).setEnabled(true);
        findViewById(R.id.tvCountDown).setEnabled(true);
        findViewById(R.id.etTimeInput).setEnabled(true);
        findViewById(R.id.btnStartTimer).setEnabled(true);
        findViewById(R.id.btnResetTimer).setEnabled(true);
        findViewById(R.id.tvStopwatchTitle).setEnabled(true);
        findViewById(R.id.tvStopwatch).setEnabled(true);
        findViewById(R.id.btnStartStopwatch).setEnabled(true);
        findViewById(R.id.btnResetStopwatch).setEnabled(true);
        findViewById(R.id.btnFinishWorkout).setEnabled(true);
        findViewById(R.id.tvSteps).setEnabled(true);
        findViewById(R.id.tvDistance).setEnabled(true);
        findViewById(R.id.btnBack).setEnabled(true);
    }

    /**
     * storeWorkoutSummary
     * <p>
     * Stores the current workout summary (steps, distance, elapsed time, and route points) into Firebase Firestore.
     * </p>
     */
    private void storeWorkoutSummary() {
        // Build a HashMap containing workout summary details
        java.util.Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("steps", stepCount);
        summary.put("distance", distanceCovered);
        summary.put("elapsedTime", tvStopwatch.getText().toString());
        summary.put("routePoints", routePoints);
        summary.put("timestamp", System.currentTimeMillis());
        // Add the summary to the "workouts" collection in Firestore
        db.collection("workouts")
                .add(summary)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Workout summary saved with ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding workout summary", e));
    }

    /**
     * shareWorkoutSummary
     * <p>
     * Shares the workout summary using an implicit intent. Constructs a text summary and opens the chooser dialog.
     * </p>
     */
    private void shareWorkoutSummary() {
        // Build a summary string containing key workout data
        String shareContent = "Workout Summary:\n" +
                "Elapsed Time: " + tvStopwatch.getText().toString() + "\n" +
                "Steps: " + stepCount + "\n" +
                "Distance: " + String.format(Locale.getDefault(), "%.2f", distanceCovered) + " m\n" +
                "Average Speed: " + tvAverageSpeed.getText().toString();
        // Create an intent for sharing plain text content
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent);
        // Launch the chooser for sharing
        startActivity(Intent.createChooser(shareIntent, "Share your workout"));
    }

    /**
     * AverageSpeedAnalysisTask
     * <p>
     * An AsyncTask that computes the average speed of the workout in the background.
     * Average Speed is calculated as distance covered (in meters) divided by total elapsed time (in seconds).
     * </p>
     */
    private class AverageSpeedAnalysisTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            // If no time elapsed, return "N/A" to avoid division by zero
            if (totalElapsedTime == 0) {
                return "N/A";
            }
            // Calculate average speed
            double avgSpeed = distanceCovered / (totalElapsedTime / 1000.0);
            return String.format(Locale.getDefault(), "%.2f m/s", avgSpeed);
        }
        @Override
        protected void onPostExecute(String result) {
            // Update the average speed TextView in the workout summary dialog and log the result
            tvAverageSpeed.setText("Average Speed: " + result);
            Log.d(TAG, "Average Speed computed: " + result);
        }
    }

    /**
     * onCreateOptionsMenu
     * <p>
     * Inflates the activity's menu from the XML resource.
     * </p>
     *
     * @param menu The Menu object to inflate.
     * @return true to display the menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * onOptionsItemSelected
     * <p>
     * Handles selections from the options menu. Navigates to different activities or logs out the user.
     * </p>
     *
     * @param item The selected MenuItem.
     * @return true if the item selection was handled.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        // Determine action based on the selected menu item
        if (item.getItemId() == R.id.menu_home) {
            intent = new Intent(this, HomeActivity.class);
        } else if (item.getItemId() == R.id.menu_logout) {
            logout();
        } else if (item.getItemId() == R.id.menu_timer) {
            intent = new Intent(this, RunningActivity.class);
        } else if (item.getItemId() == R.id.menu_userdetails) {
            intent = new Intent(this, SettingsActivity.class);
        } else if (item.getItemId() == R.id.menu_ChatBot) {
            intent = new Intent(this, ChatbotActivity.class);
        }
        if (intent != null) {
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * logout
     * <p>
     * Logs out the current user by clearing stored SharedPreferences, signing out from Firebase, and
     * redirecting to the MainActivity (login screen).
     * </p>
     */
    private void logout() {
        // Clear user-specific data from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        // Sign out from Firebase Authentication
        FirebaseAuth.getInstance().signOut();

        // Redirect to the login activity and finish this activity
        Intent intent = new Intent(RunningActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
