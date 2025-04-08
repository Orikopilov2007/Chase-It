package com.example.mypoject1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * NotificationActionReceiver handles actions from notification buttons (like "Snooze").
 * Triggered when the user interacts with notification action buttons.
 */
public class NotificationActionReceiver extends BroadcastReceiver {

    /**
     * Called when the BroadcastReceiver receives an Intent broadcast.
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // Check if the intent action matches "SNOOZE_ACTION"
        if ("SNOOZE_ACTION".equals(intent.getAction())) {
            // Notify user with a Toast message
            Toast.makeText(context, "Notification snoozed for 10 minutes", Toast.LENGTH_SHORT).show();
        }
    }
}
