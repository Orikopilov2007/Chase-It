package com.example.mypoject1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

import android.Manifest;


public class TimerActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

    // UI Elements
    Button btnBack, btnStartStopwatch, btnResetStopwatch, btnStartTimer, btnResetTimer;
    EditText etTimeInput;
    TextView tvStopwatch, tvCountDown, tvSteps, tvDistance;

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
    long totalElapsedTime = 0;  // Total time for speed calculation

    private Button btnFinishWorkout;
    private LinearLayout workoutSummaryDialog;
    private TextView tvWorkoutSummary, tvTotalSteps, tvTotalDistance;
    private Button btnCloseDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        // Initialize UI Elements
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

        // Set click listeners
        btnFinishWorkout.setOnClickListener(this);
        btnCloseDialog.setOnClickListener(this);

        // Set click listeners
        btnBack.setOnClickListener(this);
        btnStartStopwatch.setOnClickListener(this);
        btnResetStopwatch.setOnClickListener(this);
        btnStartTimer.setOnClickListener(this);
        btnResetTimer.setOnClickListener(this);

        // Check for the ACTIVITY_RECOGNITION permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 1);
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
        }
        if (stepCounterSensor == null) {
            Log.d("Sensor", "Step counter sensor not available.");
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d("Sensor", "sensorManager is good");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
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
        }

        else if (view == btnFinishWorkout) {
            showWorkoutSummary();
        } else if (view == btnCloseDialog) {
            closeWorkoutSummaryDialog();
        }
    }

    // Stopwatch Methods

    /**
     * Starts the stopwatch and updates the button text to "Pause".
     */
    private void startStopwatch() {
        stopwatchStartTime = System.currentTimeMillis();
        isStopwatchRunning = true;
        btnStartStopwatch.setText("Pause");
        runStopwatch();
        startCounters(); // Start counters when stopwatch starts
    }

    /**
     * Pauses the stopwatch and updates the button text to "Resume".
     */

    private void pauseStopwatch() {
        stopwatchTime += System.currentTimeMillis() - stopwatchStartTime;
        isStopwatchRunning = false;
        btnStartStopwatch.setText("Resume");
        timerHandler.removeCallbacks(stopwatchRunnable);
        stopCounters(); // Stop counters when stopwatch pauses
    }


    /**
     * Resets the stopwatch, clears the time, and sets the button text to "Start".
     */
    private void resetStopwatch() {
        stopwatchTime = 0;
        totalElapsedTime = 0;
        updateStopwatchText(0);
        isStopwatchRunning = false;
        btnStartStopwatch.setText("Start");
        stopCounters(); // Stop counters when stopwatch resets
    }


    /**
     * Runs the stopwatch to update the display every 10 milliseconds.
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
     * Updates the stopwatch display with the elapsed time.
     */
    private void updateStopwatchText(long elapsedMillis) {
        int hours = (int) (elapsedMillis / 3600000);
        int minutes = (int) (elapsedMillis / 60000) % 60;
        int seconds = (int) (elapsedMillis / 1000) % 60;
        int milliseconds = (int) (elapsedMillis % 1000) / 10;
        String timeFormat = String.format(Locale.getDefault(), "%02d:%02d:%02d.%02d", hours, minutes, seconds, milliseconds);
        tvStopwatch.setText(timeFormat);
    }

    // Timer Methods

    /**
     * Sets the time for the countdown timer based on user input.
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
     * Resets the timer and clears the remaining time.
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
        stopCounters(); // Stop counters when timer resets
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
                    stopCounters(); // Stop counters when timer finishes
                }
            }
        };
        timerHandler.post(countdownRunnable);
        isTimerRunning = true;
        btnStartTimer.setText("Pause");
        startCounters(); // Start counters when timer starts
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
        stopCounters(); // Stop counters when timer pauses
    }


    /**
     * Updates the countdown timer text with the remaining time.
     */
    private void updateCountText(long millisUntilFinished) {
        int minutes = (int) (millisUntilFinished / 1000) / 60;
        int seconds = (int) (millisUntilFinished / 1000) % 60;
        int milliseconds = (int) (millisUntilFinished % 1000) / 10;
        String timeFormat = String.format(Locale.getDefault(), "%02d:%02d:%02d", minutes, seconds, milliseconds);
        tvCountDown.setText(timeFormat);
    }

    // SensorEventListener Methods for Step Counter

    /**
     * Updates step count on step detection.
     */
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
        // No need to handle accuracy changes for this use case
    }

    //


    // UI update methods for counters


    private void updateSteps() {
        tvSteps.setText("Steps: " + stepCount);
    }


    private void updateDistance() {
        tvDistance.setText("Distance: " + String.format(Locale.getDefault(), "%.2f", distanceCovered) + " m");
    }



    /**
     * Starts step counters to begin tracking steps.
     */
    private void startCounters() {
        // Do not reset step count, just allow accumulation when running
        if (isStopwatchRunning || isTimerRunning) {
            if (initialStepCount < 0) {
                initialStepCount = 0; // Start fresh counting when starting
            }
        }
    }

    private void stopCounters() {
        // Only stop updating the UI, not resetting the counters
        updateSteps();
        updateDistance();
    }




    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_home) {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_logout) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (item.getItemId() == R.id.menu_camera) {
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_timer) {
            Intent intent = new Intent(this, TimerActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_userdetails) {
            Intent intent = new Intent(this, com.example.mypoject1.UserDetailsActivity.class);
            startActivity(intent);
        }
        else if (item.getItemId() == R.id.menu_ForgotPassword) {
            Intent intent = new Intent(this, com.example.mypoject1.ForgotPasswordActivity.class);
            startActivity(intent);
        }
//        else if (item.getItemId() == R.id.menu_ChatBot) {
//            Intent intent = new Intent(this, com.example.mypoject1.ChatbotActivity.class);
//            startActivity(intent);
//        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Shows the workout summary dialog with total steps and distance.
     */
    private void showWorkoutSummary() {
        tvTotalSteps.setText("Total Steps: " + stepCount);
        tvTotalDistance.setText("Total Distance: " + String.format(Locale.getDefault(), "%.2f", distanceCovered) + " m");

        // Show dialog and disable interaction with the main layout
        workoutSummaryDialog.setVisibility(View.VISIBLE);
        workoutSummaryDialog.setAlpha(0f);
        workoutSummaryDialog.animate().alpha(1f).setDuration(300); // Fade in the dialog

        // Disable other UI components
        disableMainLayout();
    }


    /**
     * Closes the workout summary dialog.
     */
    private void closeWorkoutSummaryDialog() {
        // Fade out the dialog
        workoutSummaryDialog.animate().alpha(0f).setDuration(300).withEndAction(() -> workoutSummaryDialog.setVisibility(View.GONE));

        // Reset the steps and distance to 0
        stepCount = 0;
        distanceCovered = 0.0;

        // Update the UI to reflect the reset values
        tvTotalSteps.setText("Total Steps: 0");
        tvTotalDistance.setText("Total Distance: 0.0 m");

        updateDistance();
        updateSteps();

        // Re-enable main layout interaction
        enableMainLayout();
    }

    //Disable the interactions with the page
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

    //Re-enable the interactions with the page
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
