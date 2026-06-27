package com.casaempresario.app.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.casaempresario.app.receiver.NotificationCheckReceiver;

public class NotificationScheduler {
    private static final int REQUEST_CODE = 5801;
    private static final long INTERVAL_MILLIS = 15 * 60 * 1000L;

    public static void scheduleEventChecks(Context context) {
        Context appContext = context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = createPendingIntent(appContext, PendingIntent.FLAG_UPDATE_CURRENT);

        long firstRun = SystemClock.elapsedRealtime() + 60 * 1000L;
        alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                firstRun,
                INTERVAL_MILLIS,
                pendingIntent
        );
    }

    public static void cancelEventChecks(Context context) {
        Context appContext = context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = createPendingIntent(appContext, PendingIntent.FLAG_NO_CREATE);
        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private static PendingIntent createPendingIntent(Context context, int extraFlags) {
        Intent intent = new Intent(context, NotificationCheckReceiver.class);
        intent.setAction(NotificationCheckReceiver.ACTION_CHECK_NEW_EVENTS);
        return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                extraFlags | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
