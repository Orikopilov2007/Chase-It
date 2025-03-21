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

public class MyService extends Service {

    private static final String CHANNEL_ID = "DailyMessagesChannel";


    /**
     * Called when the service is created. Initializes the notification channel
     * and schedules daily messages at specified times.
     */

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MyService", "Service Created");

        // Create the notification channel to handle notifications for Android O and above
        createNotificationChannel();

        // Schedule your messages (adjust times/messages as needed)
        scheduleExactMessage(this, 8, 0, "Good Morning! Have a great day! ☀️");
        scheduleExactMessage(this, 22, 0, "Good Night! Sleep well! 🌙");
    }

    /**
     * Called when the service is started. This is where the service runs.
     * It returns START_STICKY, which ensures the service restarts if it gets terminated.
     */

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("MyService", "Service Started");
        return START_STICKY;
    }

    /**
     * Called when the service is destroyed. Cleans up any necessary resources.
     */

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("MyService", "Service Destroyed");
    }

    /**
     * Used to bind to the service. Since this service does not support binding, it returns null.
     */

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Create notification channel for Android O and above

    /**
     * Creates a notification channel for devices running Android O and above.
     * This is necessary to send notifications on these versions.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Daily Messages",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // Static method to schedule an exact alarm for the notification.

    /**
     * Schedules an exact alarm to send a notification at a specified time.
     * If the time has already passed for today, the message will be scheduled for tomorrow.
     *
     * @param context The context from which the service is called
     * @param hour The hour at which the message should be sent
     * @param minute The minute at which the message should be sent
     * @param message The message to send as a notification
     */

    public static void scheduleExactMessage(Context context, int hour, int minute, String message) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Create an Intent to trigger the NotificationReceiver
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("message", message);
        intent.putExtra("hour", hour);
        intent.putExtra("minute", minute);

        // Generate a unique request code based on time
        int requestCode = hour * 100 + minute; // Unique request code based on time
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create a Calendar object to set the alarm time
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // If the time is already past for today, schedule for tomorrow.
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Set the alarm based on the SDK version
        if (alarmManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(),
                                pendingIntent
                        );
                        Log.d("MyService", "Scheduled exact message for " + hour + ":" + minute + " with message: " + message);
                    } else {
                        Log.w("MyService", "App does not have permission to schedule exact alarms");
                    }
                } else {
                    // For versions below Android S, proceed without checking
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                    Log.d("MyService", "Scheduled exact message for " + hour + ":" + minute + " with message: " + message);
                }
            } catch (SecurityException se) {
                Log.e("MyService", "SecurityException while scheduling exact alarm", se);
            }
        }
    }
}
