package com.example.mypoject1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.os.AsyncTask;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import com.google.android.gms.maps.model.PolylineOptions;

// API usage: Firebase Firestore API is imported to store and retrieve workout summary data remotely.
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.location.LocationServices;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TimerActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener, OnMapReadyCallback {

    private static final String TAG = "TimerActivity";

    // UI Elements
    Button btnBack, btnStartStopwatch, btnResetStopwatch, btnStartTimer, btnResetTimer;
    Button btnFinishWorkout, btnCloseDialog, btnShareSummary;
    EditText etTimeInput;
    TextView tvWorkoutSummary, tvTotalSteps, tvTotalDistance, tvAverageSpeed;
    LinearLayout workoutSummaryDialog;
    TextView tvStopwatch, tvCountDown, tvSteps, tvDistance;

    // MapView for location display
    private MapView mapView;
    private GoogleMap gMap;

    // API usage: Firebase Firestore is used to store workout summary data remotely.
    private FirebaseFirestore db;

    // Route tracking fields
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
    private Sensor accelerometerSensor; // Advanced subject: using accelerometer sensor
    private float initialStepCount = -1;

    // Counters (updated via sensor events)
    int stepCount = 0;
    double distanceCovered = 0.0;

    // Accelerometer data storage for later analysis (advanced subject: sensor data processing)
    private List<float[]> accelerometerData = new ArrayList<>();

    // Total elapsed time used for speed calculation
    long totalElapsedTime = 0;
    private float latestSensorReading = -1;

    /**
     * Called when the activity is first created. This method initializes the UI, sensors, Firebase, and MapView.
     * It also sets up animations and event listeners.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting TimerActivity");
        setContentView(R.layout.activity_timer);

        // API usage: Initialize Firebase Firestore for data storage.
        db = FirebaseFirestore.getInstance();

        // API usage: Initialize Google Maps SDK.
        try {
            MapsInitializer.initialize(this);
            Log.d(TAG, "MapsInitializer.initialize succeeded");
        } catch (Exception e) {
            Log.e(TAG, "MapsInitializer.initialize failed", e);
        }

        // Adjust layout to account for system bars (advanced subject: UI adaptation)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        // Initialize UI Elements and MapView
        findViews();

        // Load animations and apply to key views (advanced subject: UI animation)
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        View mainLayout = findViewById(R.id.main);
        mainLayout.startAnimation(fadeIn);
        btnBack.startAnimation(slideIn);
        btnStartStopwatch.startAnimation(slideIn);
        btnResetStopwatch.startAnimation(slideIn);
        btnStartTimer.startAnimation(slideIn);
        btnResetTimer.startAnimation(slideIn);

        // Set click listeners for buttons
        btnBack.setOnClickListener(this);
        btnStartStopwatch.setOnClickListener(this);
        btnResetStopwatch.setOnClickListener(this);
        btnStartTimer.setOnClickListener(this);
        btnResetTimer.setOnClickListener(this);
        btnFinishWorkout.setOnClickListener(this);
        btnCloseDialog.setOnClickListener(this);
        btnShareSummary.setOnClickListener(this);

        // Check for necessary permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting ACTIVITY_RECOGNITION permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting ACCESS_FINE_LOCATION permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
        }

        // Initialize timer and stopwatch displays
        updateStopwatchText(0);
        updateCountText(0);
        updateSteps();
        updateDistance();

        // Initialize sensors for step counting and accelerometer (advanced subject: sensor integration)
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

        // Initialize MapView for location display (advanced subject: maps integration)
        if (mapView == null) {
            Log.e(TAG, "mapView is null in onCreate!");
        } else {
            Log.d(TAG, "Initializing MapView");
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }
    }

    /**
     * Finds and initializes all view elements from the layout.
     */
    private void findViews() {
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
        tvAverageSpeed = findViewById(R.id.tvAverageSpeed); // New TextView for average speed
        btnCloseDialog = findViewById(R.id.btnCloseDialog);
        btnShareSummary = findViewById(R.id.btnShareSummary);
        mapView = findViewById(R.id.mapView);
        if (mapView == null) {
            Log.e(TAG, "mapView not found in layout!");
        } else {
            Log.d(TAG, "mapView successfully found in layout");
        }
    }

    /**
     * Called when the activity resumes. Registers sensor listeners and resumes the MapView.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_FASTEST);
            Log.d(TAG, "Sensor listener registered");
        }
        if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "Accelerometer sensor listener registered");
        }
        if (mapView != null) {
            Log.d(TAG, "Calling mapView.onResume()");
            mapView.onResume();
        }
    }

    /**
     * Called when the activity pauses. Unregisters sensor listeners and pauses the MapView.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            Log.d(TAG, "Sensor listener unregistered");
        }
        if (mapView != null) {
            Log.d(TAG, "Calling mapView.onPause()");
            mapView.onPause();
        }
    }

    /**
     * Called when the activity is destroyed. Destroys the MapView.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            Log.d(TAG, "Calling mapView.onDestroy()");
            mapView.onDestroy();
        }
    }

    /**
     * Called when the system is running low on memory. Passes this event to the MapView.
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
     * Callback when the MapView is ready. Enables MyLocation and sets up location updates.
     *
     * @param googleMap the GoogleMap object
     *
     * API usage: Google Maps API and Google Location Services API are used to show the user’s current location on the map.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: Map is ready");
        gMap = googleMap;
        // API usage: FusedLocationProviderClient from Google Location Services API for location updates.
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                gMap.setMyLocationEnabled(true);
                Log.d(TAG, "MyLocation enabled on map");
                // Request a fresh location update when the map is ready
                requestFreshLocation(fusedLocationClient);
                // Override the MyLocation button click for fresh updates
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
     * Requests a single high-accuracy location update and moves the map camera accordingly.
     *
     * @param fusedLocationClient the FusedLocationProviderClient for location updates
     *
     * API usage: Uses Google Location Services API to get a high-accuracy location.
     */
    private void requestFreshLocation(FusedLocationProviderClient fusedLocationClient) {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(500);
        locationRequest.setNumUpdates(1);  // Only one update is needed

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.d(TAG, "Location result is null. Falling back to default location.");
                    moveToDefaultLocation();
                    return;
                }
                android.location.Location location = locationResult.getLastLocation();
                if (location != null) {
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    /**
     * Moves the map camera to a default location.
     *
     * API usage: Utilizes Google Maps API to move the camera.
     */
    private void moveToDefaultLocation() {
        LatLng defaultLocation = new LatLng(32.072550, 34.811370);
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 18f));
        Log.d(TAG, "Camera moved to default location: " + defaultLocation.toString());
    }

    /**
     * Callback for sensor events. Updates step count, distance and collects accelerometer data.
     *
     * API usage: Uses Android Sensor APIs.
     *
     * @param event the sensor event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            latestSensorReading = event.values[0];
            if (initialStepCount == -1) {
                initialStepCount = event.values[0];
            }
            stepCount = (int) (event.values[0] - initialStepCount);
            distanceCovered = stepCount * 0.8; // Assumes an average step length of 0.8 meters
            updateSteps();
            updateDistance();
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Advanced subject: Collect accelerometer data for later asynchronous analysis
            accelerometerData.add(event.values.clone());
        }
    }

    /**
     * Not used in this implementation.
     *
     * @param sensor   the sensor
     * @param accuracy the new accuracy of this sensor
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No action needed for this use case.
    }

    /**
     * Updates the UI element displaying the current step count.
     */
    private void updateSteps() {
        tvSteps.setText("Steps: " + stepCount);
    }

    /**
     * Updates the UI element displaying the current distance covered.
     */
    private void updateDistance() {
        tvDistance.setText("Distance: " + String.format(Locale.getDefault(), "%.2f", distanceCovered) + " m");
    }

    /**
     * Starts the stopwatch and initializes its counters.
     */
    private void startStopwatch() {
        stopwatchStartTime = System.currentTimeMillis();
        isStopwatchRunning = true;
        btnStartStopwatch.setText("Pause");
        runStopwatch();
        startCounters();
    }

    /**
     * Pauses the stopwatch, updates the elapsed time, and stops the counter.
     */
    private void pauseStopwatch() {
        stopwatchTime += System.currentTimeMillis() - stopwatchStartTime;
        isStopwatchRunning = false;
        btnStartStopwatch.setText("Resume");
        timerHandler.removeCallbacks(stopwatchRunnable);
        stopCounters();
    }

    /**
     * Resets the stopwatch to zero.
     */
    private void resetStopwatch() {
        stopwatchTime = 0;
        totalElapsedTime = 0;
        updateStopwatchText(0);
        isStopwatchRunning = false;
        btnStartStopwatch.setText("Start");
        stopCounters();
    }

    /**
     * Runs the stopwatch updating its display periodically.
     * Uses advanced subject: Handler for repeated delayed tasks.
     */
    private void runStopwatch() {
        stopwatchRunnable = new Runnable() {
            @Override
            public void run() {
                if (isStopwatchRunning) {
                    long elapsedMillis = System.currentTimeMillis() - stopwatchStartTime + stopwatchTime;
                    updateStopwatchText(elapsedMillis);
                    totalElapsedTime = elapsedMillis;
                    timerHandler.postDelayed(this, 10);
                }
            }
        };
        timerHandler.post(stopwatchRunnable);
    }

    /**
     * Updates the stopwatch TextView with the formatted elapsed time.
     *
     * @param elapsedMillis elapsed time in milliseconds
     */
    private void updateStopwatchText(long elapsedMillis) {
        int hours = (int) (elapsedMillis / 3600000);
        int minutes = (int) (elapsedMillis / 60000) % 60;
        int seconds = (int) (elapsedMillis / 1000) % 60;
        int milliseconds = (int) (elapsedMillis % 1000) / 10;
        String timeFormat = String.format(Locale.getDefault(), "%02d:%02d:%02d.%02d", hours, minutes, seconds, milliseconds);
        tvStopwatch.setText(timeFormat);
    }

    /**
     * Parses time input from the EditText and sets the countdown timer accordingly.
     */
    private void setTimeFromInput() {
        String input = etTimeInput.getText().toString();
        if (!input.isEmpty()) {
            int seconds = Integer.parseInt(input);
            timeRemain = seconds * 1000L;
            updateCountText(timeRemain);
        }
    }

    /**
     * Resets the countdown timer.
     */
    private void resetTimer() {
        if (countdownRunnable != null) {
            timerHandler.removeCallbacks(countdownRunnable);
        }
        timeRemain = 0;
        millisAtPause = 0;
        updateCountText(0);
        isTimerRunning = false;
        btnStartTimer.setText("Start");
        stopCounters();
    }

    /**
     * Starts the countdown timer.
     */
    private void startTimer() {
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (timeRemain > 0) {
                    timeRemain -= 10;
                    updateCountText(timeRemain);
                    timerHandler.postDelayed(this, 10);
                } else {
                    isTimerRunning = false;
                    updateCountText(0);
                    btnStartTimer.setText("Start");
                    Toast.makeText(TimerActivity.this, "Time's up! Great job!", Toast.LENGTH_LONG).show();
                    stopCounters();
                }
            }
        };
        timerHandler.post(countdownRunnable);
        isTimerRunning = true;
        btnStartTimer.setText("Pause");
        startCounters();
    }

    /**
     * Pauses the countdown timer.
     */
    private void pauseTimer() {
        if (countdownRunnable != null) {
            timerHandler.removeCallbacks(countdownRunnable);
        }
        millisAtPause = timeRemain;
        isTimerRunning = false;
        btnStartTimer.setText("Resume");
        stopCounters();
    }

    /**
     * Updates the countdown timer TextView with the formatted time.
     *
     * @param millisUntilFinished time remaining in milliseconds
     */
    private void updateCountText(long millisUntilFinished) {
        int minutes = (int) (millisUntilFinished / 1000) / 60;
        int seconds = (int) (millisUntilFinished / 1000) % 60;
        int milliseconds = (int) (millisUntilFinished % 1000) / 10;
        String timeFormat = String.format(Locale.getDefault(), "%02d:%02d:%02d", minutes, seconds, milliseconds);
        tvCountDown.setText(timeFormat);
    }

    /**
     * Prepares the counters when the stopwatch or timer is running.
     */
    private void startCounters() {
        if (isStopwatchRunning || isTimerRunning) {
            if (initialStepCount < 0) {
                initialStepCount = 0;
            }
        }
    }

    /**
     * Updates the UI counters (steps and distance).
     */
    private void stopCounters() {
        updateSteps();
        updateDistance();
    }

    /**
     * Handles button click events and delegates to the corresponding methods.
     *
     * Advanced subjects used: Activity navigation, sensor management, asynchronous tasks.
     *
     * @param view the clicked view
     */
    @Override
    public void onClick(View view) {
        if (view == btnBack) {
            Intent intent = new Intent(TimerActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        } else if (view == btnStartStopwatch) {
            if (!isStopwatchRunning && !isTimerRunning) {
                startStopwatch();
            } else if (isStopwatchRunning) {
                pauseStopwatch();
            }
        } else if (view == btnResetStopwatch) {
            resetStopwatch();
        } else if (view == btnStartTimer) {
            if (!isTimerRunning && !isStopwatchRunning) {
                if (timeRemain == 0) {
                    setTimeFromInput();
                }
                startTimer();
            } else if (isTimerRunning) {
                pauseTimer();
            }
        } else if (view == btnResetTimer) {
            resetTimer();
        } else if (view == btnFinishWorkout) {
            // Show summary, store data, and run asynchronous average speed calculation
            showWorkoutSummary();
            storeWorkoutSummary();
            new AverageSpeedAnalysisTask().execute(); // Advanced subject: AsyncTask usage
        } else if (view == btnCloseDialog) {
            closeWorkoutSummaryDialog();
        } else if (view == btnShareSummary) {
            shareWorkoutSummary();
        }
    }

    /**
     * Displays the workout summary dialog and updates summary UI elements.
     */
    private void showWorkoutSummary() {
        tvTotalSteps.setText("Total Steps: " + stepCount);
        tvTotalDistance.setText("Total Distance: " + String.format(Locale.getDefault(), "%.2f", distanceCovered) + " m");
        tvWorkoutSummary.setText("Elapsed Time: " + tvStopwatch.getText().toString());
        workoutSummaryDialog.setVisibility(View.VISIBLE);
        workoutSummaryDialog.setAlpha(0f);
        workoutSummaryDialog.animate().alpha(1f).setDuration(300);
        disableMainLayout();
    }

    /**
     * Closes the workout summary dialog and resets counters and sensor data for a new workout.
     */
    private void closeWorkoutSummaryDialog() {
        workoutSummaryDialog.animate().alpha(0f).setDuration(300).withEndAction(() -> workoutSummaryDialog.setVisibility(View.GONE));
        if (latestSensorReading != -1) {
            initialStepCount = latestSensorReading;
        }
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
        enableMainLayout();
        accelerometerData.clear();
    }

    /**
     * Disables UI elements to prevent user interaction during certain processes.
     */
    private void disableMainLayout() {
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
     * Enables UI elements for user interaction.
     */
    private void enableMainLayout() {
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
     * Stores the workout summary data in Firebase Firestore.
     *
     * API usage: Firebase Firestore API is used to store workout data in the remote database.
     */
    private void storeWorkoutSummary() {
        java.util.Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("steps", stepCount);
        summary.put("distance", distanceCovered);
        summary.put("elapsedTime", tvStopwatch.getText().toString());
        summary.put("routePoints", routePoints);
        summary.put("timestamp", System.currentTimeMillis());
        db.collection("workouts")
                .add(summary)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Workout summary saved with ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding workout summary", e));
    }

    /**
     * Shares the workout summary using an implicit intent.
     *
     * API usage: Android Intent API (ACTION_SEND) is used to share data with other apps.
     */
    private void shareWorkoutSummary() {
        String shareContent = "Workout Summary:\n" +
                "Elapsed Time: " + tvStopwatch.getText().toString() + "\n" +
                "Steps: " + stepCount + "\n" +
                "Distance: " + String.format(Locale.getDefault(), "%.2f", distanceCovered) + " m\n" +
                "Average Speed: " + tvAverageSpeed.getText().toString();
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent);
        startActivity(Intent.createChooser(shareIntent, "Share your workout"));
    }

    /**
     * AsyncTask to compute average speed from workout data.
     * Advanced subject: This uses AsyncTask to perform background computation without blocking the UI.
     *
     * Average Speed = distanceCovered (in meters) / (totalElapsedTime in seconds)
     */
    private class AverageSpeedAnalysisTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            if (totalElapsedTime == 0) {
                return "N/A";
            }
            double avgSpeed = distanceCovered / (totalElapsedTime / 1000.0);
            return String.format(Locale.getDefault(), "%.2f m/s", avgSpeed);
        }
        @Override
        protected void onPostExecute(String result) {
            // Update the average speed TextView in the workout summary dialog
            tvAverageSpeed.setText("Average Speed: " + result);
            Log.d(TAG, "Average Speed computed: " + result);
        }
    }
}
