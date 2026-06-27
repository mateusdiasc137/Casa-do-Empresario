package com.casaempresario.app.service;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import com.casaempresario.app.activity.EventDetailActivity;
import com.casaempresario.app.database.Evento;
import com.casaempresario.app.util.NotificationHelper;
import com.casaempresario.app.util.SessionManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashSet;
import java.util.Set;

public class EventNotificationService extends Service {
    private static final String PREFS = "CasaEmpresarioNotifications";
    private static final String KEY_SEEN_EVENTS = "seen_event_ids";

    private ListenerRegistration eventListener;
    private SessionManager sessionManager;
    private boolean firstSnapshot = true;

    public static void start(Context context) {
        Intent intent = new Intent(context, EventNotificationService.class);
        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, EventNotificationService.class);
        context.stopService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sessionManager = new SessionManager(this);
        NotificationHelper.createNotificationChannel(this);
        startEventListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!sessionManager.isLogado()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (eventListener == null) {
            startEventListener();
        }
        return START_STICKY;
    }

    private void startEventListener() {
        if (eventListener != null || !sessionManager.isLogado()) {
            return;
        }

        eventListener = FirebaseFirestore.getInstance()
                .collection("eventos")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        return;
                    }

                    SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                    Set<String> seen = new HashSet<>(prefs.getStringSet(KEY_SEEN_EVENTS, new HashSet<>()));
                    boolean changed = false;

                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        if (change.getType() != DocumentChange.Type.ADDED) {
                            continue;
                        }

                        Evento evento = change.getDocument().toObject(Evento.class);
                        String eventId = String.valueOf(evento.id != 0 ? evento.id : change.getDocument().getId());

                        if (firstSnapshot) {
                            if (seen.add(eventId)) {
                                changed = true;
                            }
                            continue;
                        }

                        if (seen.contains(eventId)) {
                            continue;
                        }

                        seen.add(eventId);
                        changed = true;

                        if (deveNotificar(evento)) {
                            mostrarNotificacaoNovoEvento(evento, eventId);
                        }
                    }

                    if (changed) {
                        prefs.edit().putStringSet(KEY_SEEN_EVENTS, seen).apply();
                    }

                    firstSnapshot = false;
                });
    }

    private boolean deveNotificar(Evento evento) {
        if (evento == null || !sessionManager.isLogado()) {
            return false;
        }

        String role = sessionManager.getRole();
        boolean usuarioComum = role == null || "PARTICIPANTE".equalsIgnoreCase(role.trim());
        if (!usuarioComum) {
            return false;
        }

        Long criadoPor = evento.criadoPor;
        if (criadoPor != null && criadoPor.equals(sessionManager.getUserId())) {
            return false;
        }

        return evento.status == null
                || "AGENDADO".equalsIgnoreCase(evento.status)
                || "EM_ANDAMENTO".equalsIgnoreCase(evento.status);
    }

    private void mostrarNotificacaoNovoEvento(Evento evento, String eventId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent intent = new Intent(this, EventDetailActivity.class);
        intent.putExtra("eventoId", evento.id);
        intent.putExtra("eventoTitulo", evento.titulo);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                eventId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String titulo = evento.titulo != null && !evento.titulo.trim().isEmpty()
                ? evento.titulo.trim()
                : "Novo evento publicado";

        String local = evento.local != null && !evento.local.trim().isEmpty()
                ? "Local: " + evento.local.trim()
                : "Toque para ver os detalhes.";

        String mensagem = "Um organizador acabou de publicar: " + titulo;

        NotificationManagerCompat.from(this).notify(
                Math.abs(eventId.hashCode()),
                NotificationHelper.baseEventNotification(this)
                        .setContentTitle("Novo evento na Casa do Empresário")
                        .setContentText(titulo)
                        .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle()
                                .bigText(mensagem + "\n" + local))
                        .setContentIntent(pendingIntent)
                        .build()
        );
    }

    @Override
    public void onDestroy() {
        if (eventListener != null) {
            eventListener.remove();
            eventListener = null;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
