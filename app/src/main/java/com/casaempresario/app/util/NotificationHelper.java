package com.casaempresario.app.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.casaempresario.app.R;

public class NotificationHelper {
    public static final String CHANNEL_EVENTOS = "novos_eventos_channel";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null || manager.getNotificationChannel(CHANNEL_EVENTOS) != null) {
                return;
            }

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_EVENTOS,
                    "Novos eventos",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Avisos quando organizadores publicarem novos eventos.");
            channel.enableVibration(true);
            channel.setShowBadge(true);
            manager.createNotificationChannel(channel);
        }
    }

    public static NotificationCompat.Builder baseEventNotification(Context context) {
        return new NotificationCompat.Builder(context, CHANNEL_EVENTOS)
                .setSmallIcon(R.drawable.ic_notification_event)
                .setColor(context.getColor(R.color.secondary))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSubText("Casa do Empresário");
    }
}
