package com.casaempresario.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.casaempresario.app.util.NotificationScheduler;
import com.casaempresario.app.util.SessionManager;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SessionManager sessionManager = new SessionManager(context.getApplicationContext());
            if (sessionManager.isLogado()) {
                NotificationScheduler.scheduleEventChecks(context.getApplicationContext());
            }
        }
    }
}
