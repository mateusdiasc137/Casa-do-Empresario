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
    public static final String CHANNEL_MENSAGENS = "mensagens_channel";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) {
                return;
            }

            if (manager.getNotificationChannel(CHANNEL_EVENTOS) == null) {
                NotificationChannel eventosChannel = new NotificationChannel(
                        CHANNEL_EVENTOS,
                        "Novos eventos",
                        NotificationManager.IMPORTANCE_HIGH
                );
                eventosChannel.setDescription("Avisos quando organizadores publicarem novos eventos.");
                eventosChannel.enableVibration(true);
                eventosChannel.setShowBadge(true);
                manager.createNotificationChannel(eventosChannel);
            }

            if (manager.getNotificationChannel(CHANNEL_MENSAGENS) == null) {
                NotificationChannel mensagensChannel = new NotificationChannel(
                        CHANNEL_MENSAGENS,
                        "Mensagens",
                        NotificationManager.IMPORTANCE_HIGH
                );
                mensagensChannel.setDescription("Avisos quando uma nova mensagem chegar.");
                mensagensChannel.enableVibration(true);
                mensagensChannel.setShowBadge(true);
                manager.createNotificationChannel(mensagensChannel);
            }
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
                .setSubText("CapiHub");
    }

    public static NotificationCompat.Builder baseMessageNotification(Context context) {
        return new NotificationCompat.Builder(context, CHANNEL_MENSAGENS)
                .setSmallIcon(R.drawable.ic_notification_event)
                .setColor(context.getColor(R.color.secondary))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSubText("CapiHub");
    }
}
