package com.example.mypoject1;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

/**
 * NotificationReceiver handles the creation and display of daily message notifications.
 * It builds an interactive notification with a snooze action and reschedules the notification for the next day.
 */
public class NotificationReceiver extends BroadcastReceiver {

    // The notification channel ID for daily messages.
    private static final String CHANNEL_ID = "DailyMessagesChannel";

    /**
     * Called when the BroadcastReceiver receives an Intent broadcast.
     * Builds and displays the notification with interactive snooze functionality, and reschedules it for the next day.
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The received Intent containing notification data.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // Extract the message text from the intent.
        String message = intent.getStringExtra("message");

        // Create an intent for the snooze action.
        Intent snoozeIntent = new Intent(context, NotificationActionReceiver.class);
        snoozeIntent.setAction("SNOOZE_ACTION");
        snoozeIntent.putExtra("message", message);

        // Wrap the snooze intent in a PendingIntent for broadcast.
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the interactive notification with a snooze action button.
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.my_logo)  // Replace with your app's drawable icon.
                .setContentTitle("Daily Message")
                .setContentText(message)
                .addAction(R.drawable.ic_snooze, "Snooze", snoozePendingIntent) // Adds the snooze button.
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .build();

        // Obtain the NotificationManager and display the notification with a unique ID.
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            int notificationId = (intent.getIntExtra("hour", 0) * 100) + intent.getIntExtra("minute", 0);
            manager.notify(notificationId, notification);
        }

        // Retrieve scheduled time values from the intent.
        int hour = intent.getIntExtra("hour", -1);
        int minute = intent.getIntExtra("minute", -1);

        // Reschedule the notification for the next day if valid hour and minute are provided.
        if (hour != -1 && minute != -1) {
            MyService.scheduleExactMessage(context, hour, minute, message);
        }
    }
}
