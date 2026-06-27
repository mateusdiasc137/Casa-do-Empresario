package com.casaempresario.app.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class NotificationPermissionHelper {
    public static final int REQUEST_POST_NOTIFICATIONS = 9401;

    public static void requestPostNotificationsIfNeeded(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

            // Android 13+ exige permissão em tempo de execução.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_POST_NOTIFICATIONS
                );
                return;
            }
        }

        // Em versões anteriores, a permissão é controlada nas configurações do app.
        if (!NotificationManagerCompat.from(activity).areNotificationsEnabled()) {
            showNotificationSettingsDialog(activity);
        }
    }

    public static void requestPostNotificationsDelayed(Activity activity) {
        if (activity == null) {
            return;
        }
        activity.getWindow().getDecorView().postDelayed(
                () -> requestPostNotificationsIfNeeded(activity),
                650
        );
    }

    public static boolean hasNotificationPermission(Context context) {
        if (context == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static void showNotificationSettingsDialog(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        new MaterialAlertDialogBuilder(activity)
                .setTitle("Ativar notificações")
                .setMessage("Para receber avisos quando um organizador publicar um novo evento, ative as notificações do app nas configurações.")
                .setPositiveButton("Abrir configurações", (dialog, which) -> openNotificationSettings(activity))
                .setNegativeButton("Agora não", null)
                .show();
    }

    public static void openNotificationSettings(Activity activity) {
        try {
            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
            } else {
                intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:" + activity.getPackageName()));
            }
            activity.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(activity, "Abra as configurações do app e ative as notificações.", Toast.LENGTH_LONG).show();
        }
    }
}
