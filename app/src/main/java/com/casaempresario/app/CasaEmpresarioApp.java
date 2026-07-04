package com.casaempresario.app;

import android.app.Application;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import com.casaempresario.app.util.NotificationHelper;
import com.casaempresario.app.service.FCMService;

public class CasaEmpresarioApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Cria os canais de notificação assim que o app inicia.
        // Isso é fundamental para que notificações FCM que chegam
        // com o app em segundo plano possam ser exibidas pelo Android.
        NotificationHelper.createNotificationChannel(this);

        // Registra/atualiza o token FCM sempre que o app iniciar
        FCMService.registrarToken(this);
    }
}
