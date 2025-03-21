package com.example.mypoject1;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;

public class NotificationReceiver extends BroadcastReceiver {

    // Channel ID used for the notification
    private static final String CHANNEL_ID = "DailyMessagesChannel";

    /**
     * Called when the BroadcastReceiver receives a broadcast.
     * It displays a daily notification with a message and reschedules the alarm for the next day.
     *
     * @param context The context in which the receiver is running.
     * @param intent The Intent that triggered the receiver, containing the message and time.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // Extract the message passed with the Intent
        String message = intent.getStringExtra("message");

        // Build the notification with the extracted message
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.my_logo)  // Ensure this drawable exists or use your app icon
                .setContentTitle("Daily Message")  // Title of the notification
                .setContentText(message)  // The actual message content
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)  // Default notification priority
                .build();

        // Get the NotificationManager system service to display the notification
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            // Use a unique notification ID based on the time (hour and minute)
            int notificationId = (intent.getIntExtra("hour", 0) * 100) + intent.getIntExtra("minute", 0);
            // Display the notification
            manager.notify(notificationId, notification);
        }

        // Reschedule the alarm for the next day with the same message and time
        int hour = intent.getIntExtra("hour", -1);
        int minute = intent.getIntExtra("minute", -1);
        if (hour != -1 && minute != -1) {
            // Reschedule the notification to trigger at the same time on the next day
            MyService.scheduleExactMessage(context, hour, minute, message);
        }
    }
}
