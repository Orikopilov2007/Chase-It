package com.example.mypoject1;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import java.util.Calendar;

/**
 * MyService is an Android Service that handles the scheduling of daily notifications.
 * It creates a notification channel (for Android O and above) and schedules daily messages
 * at specific times using the AlarmManager.
 */
public class MyService extends Service {

    // Unique identifier for the notification channel
    private static final String CHANNEL_ID = "DailyMessagesChannel";

    /**
     * Called when the service is first created.
     * <p>
     * This method initializes the service by creating the necessary notification channel
     * and scheduling the daily messages at specified times. In this case, a morning message
     * at 8:00 AM and a night message at 10:00 PM.
     * </p>
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MyService", "Service Created");

        // Create the notification channel required for devices running Android O and above.
        createNotificationChannel();

        // Schedule the daily notifications with exact timing.
        // Schedules a "Good Morning" notification at 8:00 AM.
        scheduleExactMessage(this, 8, 0, "Good Morning! Have a great running!");

        // Schedules a "Good Night" notification at 10:00 PM.
        scheduleExactMessage(this, 22, 0, "Good Night!");
    }

    /**
     * Called when the service is explicitly started.
     * <p>
     * This method is responsible for handling any incoming start requests. The service
     * is set to return START_STICKY, meaning that if the service is terminated by the system,
     * it will be recreated with a null intent.
     * </p>
     *
     * @param intent  The Intent supplied to startService(Intent), as given.
     * @param flags   Additional data about this start request.
     * @param startId A unique integer representing this specific request.
     * @return START_STICKY to indicate that the service should be restarted if it is terminated.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("MyService", "Service Started");
        return START_STICKY;
    }

    /**
     * Called when the service is about to be destroyed.
     * <p>
     * This method is used to perform any final cleanup before the service is destroyed.
     * </p>
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("MyService", "Service Destroyed");
    }

    /**
     * This service does not support binding, so this method returns null.
     *
     * @param intent The Intent that was used to bind to this service.
     * @return Always returns null since binding is not supported.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Creates a notification channel for Android O (API level 26) and above.
     * <p>
     * A notification channel is required to post notifications on Android O and higher.
     * This method sets up the channel with a unique identifier, a name ("Daily Messages"),
     * and a default importance level.
     * </p>
     */
    private void createNotificationChannel() {
        // Check if the device is running Android O or later
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create a NotificationChannel with specified ID, name, and importance
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Daily Messages",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            // Retrieve the NotificationManager from the system services
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                // Register the notification channel with the system
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Schedules an exact alarm to trigger a notification at the specified time.
     * <p>
     * This method sets an alarm using the AlarmManager to fire a broadcast which will be
     * received by NotificationReceiver. If the specified time has already passed for the current day,
     * the alarm is scheduled for the next day.
     * </p>
     *
     * @param context The context used to access system services.
     * @param hour    The hour of the day (in 24-hour format) when the notification should be sent.
     * @param minute  The minute when the notification should be sent.
     * @param message The message to be sent as part of the notification.
     */
    public static void scheduleExactMessage(Context context, int hour, int minute, String message) {
        // Retrieve the AlarmManager system service for scheduling alarms
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Create an intent to trigger NotificationReceiver with the provided message and time data.
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("message", message);
        intent.putExtra("hour", hour);
        intent.putExtra("minute", minute);

        // Generate a unique request code based on the specified hour and minute.
        int requestCode = hour * 100 + minute;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                // FLAG_UPDATE_CURRENT ensures the PendingIntent is updated if it already exists,
                // FLAG_IMMUTABLE ensures it cannot be modified by other apps.
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create and configure a Calendar instance to the target time (hour:minute:00)
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // Check if the specified time has already passed today; if so, schedule for the next day.
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // If the AlarmManager is available, attempt to schedule the exact alarm.
        if (alarmManager != null) {
            try {
                // For Android S (API level 31) and above, check if the app has permission to schedule exact alarms.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        // Set an exact alarm that can wake the device while idle.
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(),
                                pendingIntent
                        );
                        Log.d("MyService", "Scheduled exact message for " + hour + ":" + minute + " with message: " + message);
                    } else {
                        // Log a warning if the app does not have permission to schedule exact alarms.
                        Log.w("MyService", "App does not have permission to schedule exact alarms");
                    }
                } else {
                    // For devices below Android S, directly set the alarm without additional permission checks.
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                    Log.d("MyService", "Scheduled exact message for " + hour + ":" + minute + " with message: " + message);
                }
            } catch (SecurityException se) {
                // Catch and log any SecurityExceptions that occur if scheduling fails due to permission issues.
                Log.e("MyService", "SecurityException while scheduling exact alarm", se);
            }
        }
    }
}
