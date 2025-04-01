package com.example.mypoject1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

import java.util.Locale;

public class TimerActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener, OnMapReadyCallback {

    private static final String TAG = "TimerActivity";

    // UI Elements
    Button btnBack, btnStartStopwatch, btnResetStopwatch, btnStartTimer, btnResetTimer;
    EditText etTimeInput;
    TextView tvStopwatch, tvCountDown, tvSteps, tvDistance;
    // MapView for location
    private MapView mapView;
    private GoogleMap gMap;

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
    private float initialStepCount = -1;

    // Counters (updated via sensor events)
    int stepCount = 0;
    double distanceCovered = 0.0;

    // Speed Calculation
    long totalElapsedTime = 0;

    private Button btnFinishWorkout;
    private LinearLayout workoutSummaryDialog;
    private TextView tvWorkoutSummary, tvTotalSteps, tvTotalDistance;
    private Button btnCloseDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting TimerActivity");
        setContentView(R.layout.activity_timer);

        // Initialize Maps SDK with logging
        try {
            MapsInitializer.initialize(this);
            Log.d(TAG, "MapsInitializer.initialize succeeded");
        } catch (Exception e) {
            Log.e(TAG, "MapsInitializer.initialize failed", e);
        }

        // Adjust layout for system bars
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

        // Load animations and apply to key views
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        View mainLayout = findViewById(R.id.main);
        mainLayout.startAnimation(fadeIn);
        btnBack.startAnimation(slideIn);
        btnStartStopwatch.startAnimation(slideIn);
        btnResetStopwatch.startAnimation(slideIn);
        btnStartTimer.startAnimation(slideIn);
        btnResetTimer.startAnimation(slideIn);

        // Set click listeners
        btnBack.setOnClickListener(this);
        btnStartStopwatch.setOnClickListener(this);
        btnResetStopwatch.setOnClickListener(this);
        btnStartTimer.setOnClickListener(this);
        btnResetTimer.setOnClickListener(this);
        btnFinishWorkout.setOnClickListener(this);
        btnCloseDialog.setOnClickListener(this);

        // Check permissions for activity recognition and location
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

        // Initialize sensor for step counting
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            Log.d(TAG, "Step counter sensor: " + (stepCounterSensor != null ? "available" : "not available"));
        }
        if (stepCounterSensor == null) {
            Log.d(TAG, "Step counter sensor not available.");
        }

        // Initialize MapView
        if (mapView == null) {
            Log.e(TAG, "mapView is null in onCreate!");
        } else {
            Log.d(TAG, "Initializing MapView");
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }
    }

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
        btnCloseDialog = findViewById(R.id.btnCloseDialog);
        // MapView (make sure its id matches in your XML)
        mapView = findViewById(R.id.mapView);
        if (mapView == null) {
            Log.e(TAG, "mapView not found in layout!");
        } else {
            Log.d(TAG, "mapView successfully found in layout");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "Sensor listener registered");
        }
        if (mapView != null) {
            Log.d(TAG, "Calling mapView.onResume()");
            mapView.onResume();
        }
    }

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            Log.d(TAG, "Calling mapView.onDestroy()");
            mapView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            Log.d(TAG, "Calling mapView.onLowMemory()");
            mapView.onLowMemory();
        }
    }

    // Map Ready Callback – sets up the map and location updates
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: Map is ready");
        gMap = googleMap;

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                gMap.setMyLocationEnabled(true);
                Log.d(TAG, "MyLocation enabled on map");

                // Request a fresh location update when the map is ready
                requestFreshLocation(fusedLocationClient);

                // Override the MyLocation button click so it always requests a fresh update
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

    private void requestFreshLocation(FusedLocationProviderClient fusedLocationClient) {
        // Create a location request for a fresh location update
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000); // 1 second interval
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
                    // Zoom level 18f for a closer view
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

    private void moveToDefaultLocation() {
        // Coordinates for Golomov 15, Givatayim
        LatLng defaultLocation = new LatLng(32.072550, 34.811370);
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 18f));
        Log.d(TAG, "Camera moved to default location: " + defaultLocation.toString());
    }

    // SensorEventListener Methods for step counting
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (initialStepCount == -1) {
                initialStepCount = event.values[0];
            }
            stepCount = (int) (event.values[0] - initialStepCount);
            distanceCovered = stepCount * 0.8; // Assuming an average step length of 0.8 meters
            updateSteps();
            updateDistance();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No action needed for this use case
    }

    // UI update methods for counters
    private void updateSteps() {
        tvSteps.setText("Steps: " + stepCount);
    }

    private void updateDistance() {
        tvDistance.setText("Distance: " + String.format(Locale.getDefault(), "%.2f", distanceCovered) + " m");
    }

    // Stopwatch and Timer methods (start, pause, reset, update, etc.)
    private void startStopwatch() {
        stopwatchStartTime = System.currentTimeMillis();
        isStopwatchRunning = true;
        btnStartStopwatch.setText("Pause");
        runStopwatch();
        startCounters();
    }

    private void pauseStopwatch() {
        stopwatchTime += System.currentTimeMillis() - stopwatchStartTime;
        isStopwatchRunning = false;
        btnStartStopwatch.setText("Resume");
        timerHandler.removeCallbacks(stopwatchRunnable);
        stopCounters();
    }

    private void resetStopwatch() {
        stopwatchTime = 0;
        totalElapsedTime = 0;
        updateStopwatchText(0);
        isStopwatchRunning = false;
        btnStartStopwatch.setText("Start");
        stopCounters();
    }

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

    private void updateStopwatchText(long elapsedMillis) {
        int hours = (int) (elapsedMillis / 3600000);
        int minutes = (int) (elapsedMillis / 60000) % 60;
        int seconds = (int) (elapsedMillis / 1000) % 60;
        int milliseconds = (int) (elapsedMillis % 1000) / 10;
        String timeFormat = String.format(Locale.getDefault(), "%02d:%02d:%02d.%02d", hours, minutes, seconds, milliseconds);
        tvStopwatch.setText(timeFormat);
    }

    private void setTimeFromInput() {
        String input = etTimeInput.getText().toString();
        if (!input.isEmpty()) {
            int seconds = Integer.parseInt(input);
            timeRemain = seconds * 1000L;
            updateCountText(timeRemain);
        }
    }

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

    private void pauseTimer() {
        if (countdownRunnable != null) {
            timerHandler.removeCallbacks(countdownRunnable);
        }
        millisAtPause = timeRemain;
        isTimerRunning = false;
        btnStartTimer.setText("Resume");
        stopCounters();
    }

    private void updateCountText(long millisUntilFinished) {
        int minutes = (int) (millisUntilFinished / 1000) / 60;
        int seconds = (int) (millisUntilFinished / 1000) % 60;
        int milliseconds = (int) (millisUntilFinished % 1000) / 10;
        String timeFormat = String.format(Locale.getDefault(), "%02d:%02d:%02d", minutes, seconds, milliseconds);
        tvCountDown.setText(timeFormat);
    }

    private void startCounters() {
        if (isStopwatchRunning || isTimerRunning) {
            if (initialStepCount < 0) {
                initialStepCount = 0;
            }
        }
    }

    private void stopCounters() {
        updateSteps();
        updateDistance();
    }

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
            showWorkoutSummary();
        } else if (view == btnCloseDialog) {
            closeWorkoutSummaryDialog();
        }
    }

    private void showWorkoutSummary() {
        tvTotalSteps.setText("Total Steps: " + stepCount);
        tvTotalDistance.setText("Total Distance: " + String.format(Locale.getDefault(), "%.2f", distanceCovered) + " m");
        workoutSummaryDialog.setVisibility(View.VISIBLE);
        workoutSummaryDialog.setAlpha(0f);
        workoutSummaryDialog.animate().alpha(1f).setDuration(300);
        disableMainLayout();
    }

    private void closeWorkoutSummaryDialog() {
        workoutSummaryDialog.animate().alpha(0f).setDuration(300).withEndAction(() -> workoutSummaryDialog.setVisibility(View.GONE));
        stepCount = 0;
        distanceCovered = 0.0;
        tvTotalSteps.setText("Total Steps: 0");
        tvTotalDistance.setText("Total Distance: 0.0 m");
        updateDistance();
        updateSteps();
        enableMainLayout();
    }

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
}
