package com.example.mypoject1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class NotificationActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("SNOOZE_ACTION".equals(intent.getAction())) {
            // Example: Reschedule notification after a 10-minute snooze
            Toast.makeText(context, "Notification snoozed for 10 minutes", Toast.LENGTH_SHORT).show();
            // Implement your snooze rescheduling logic here
        }
    }
}
