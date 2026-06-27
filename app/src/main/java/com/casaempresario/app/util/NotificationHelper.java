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
            if (manager == null) return;

            // Canal de eventos
            if (manager.getNotificationChannel(CHANNEL_EVENTOS) == null) {
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

            // Canal de mensagens de chat
            if (manager.getNotificationChannel(CHANNEL_MENSAGENS) == null) {
                NotificationChannel chatChannel = new NotificationChannel(
                        CHANNEL_MENSAGENS,
                        "Mensagens",
                        NotificationManager.IMPORTANCE_HIGH
                );
                chatChannel.setDescription("Notificações de novas mensagens de chat.");
                chatChannel.enableVibration(true);
                chatChannel.setShowBadge(true);
                manager.createNotificationChannel(chatChannel);
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
                .setSubText("Casa do Empresário");
    }
}
