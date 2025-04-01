package com.example.mypoject1;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "DailyMessagesChannel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String message = intent.getStringExtra("message");

        // Create a snooze action
        Intent snoozeIntent = new Intent(context, NotificationActionReceiver.class);
        snoozeIntent.setAction("SNOOZE_ACTION");
        snoozeIntent.putExtra("message", message);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the interactive notification with an action button
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.my_logo)  // Replace with your drawable
                .setContentTitle("Daily Message")
                .setContentText(message)
                .addAction(R.drawable.ic_snooze, "Snooze", snoozePendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .build();

        // Display the notification using a unique ID
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            int notificationId = (intent.getIntExtra("hour", 0) * 100) + intent.getIntExtra("minute", 0);
            manager.notify(notificationId, notification);
        }

        // Reschedule the notification for the next day
        int hour = intent.getIntExtra("hour", -1);
        int minute = intent.getIntExtra("minute", -1);
        if (hour != -1 && minute != -1) {
            MyService.scheduleExactMessage(context, hour, minute, message);
        }
    }
}
