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
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.casaempresario.app.activity.ChatActivity;
import com.casaempresario.app.activity.EventDetailActivity;
import com.casaempresario.app.database.Evento;
import com.casaempresario.app.database.Mensagem;
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
    private static final String KEY_LAST_MESSAGE_ID = "last_message_id_";

    private ListenerRegistration eventListener;
    private ListenerRegistration messageListener;
    private SessionManager sessionManager;
    private boolean firstEventSnapshot = true;
    private boolean firstMessageSnapshot = true;

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
        startMessageListener();
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
        if (messageListener == null) {
            startMessageListener();
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

                        if (firstEventSnapshot) {
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

                        if (deveNotificarEvento(evento)) {
                            mostrarNotificacaoNovoEvento(evento, eventId);
                        }
                    }

                    if (changed) {
                        prefs.edit().putStringSet(KEY_SEEN_EVENTS, seen).apply();
                    }

                    firstEventSnapshot = false;
                });
    }

    private void startMessageListener() {
        if (messageListener != null || !sessionManager.isLogado()) {
            return;
        }

        long currentUserId = sessionManager.getUserId();
        messageListener = FirebaseFirestore.getInstance()
                .collection("mensagens")
                .whereEqualTo("destinatarioId", currentUserId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        return;
                    }

                    SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                    String lastMessageKey = KEY_LAST_MESSAGE_ID + currentUserId;
                    long lastKnownId = prefs.getLong(lastMessageKey, 0L);
                    long maxSeenId = lastKnownId;

                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        if (change.getType() != DocumentChange.Type.ADDED) {
                            continue;
                        }

                        Mensagem mensagem = change.getDocument().toObject(Mensagem.class);
                        long msgId = mensagem.id != 0 ? mensagem.id : parseLong(change.getDocument().getId());
                        if (msgId > maxSeenId) {
                            maxSeenId = msgId;
                        }

                        if (lastKnownId == 0L && firstMessageSnapshot) {
                            continue;
                        }

                        if (msgId > lastKnownId && mensagem.destinatarioId == currentUserId
                                && mensagem.remetenteId != currentUserId) {
                            mostrarNotificacaoNovaMensagem(mensagem, msgId);
                        }
                    }

                    if (maxSeenId > lastKnownId) {
                        prefs.edit().putLong(lastMessageKey, maxSeenId).apply();
                    }
                    firstMessageSnapshot = false;
                });
    }

    private boolean deveNotificarEvento(Evento evento) {
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

    private boolean podeMostrarNotificacao() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void mostrarNotificacaoNovoEvento(Evento evento, String eventId) {
        if (!podeMostrarNotificacao()) {
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
                        .setContentTitle("Novo evento no CapiHub")
                        .setContentText(titulo)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(mensagem + "\n" + local))
                        .setContentIntent(pendingIntent)
                        .build()
        );
    }

    private void mostrarNotificacaoNovaMensagem(Mensagem mensagem, long msgId) {
        if (!podeMostrarNotificacao()) {
            return;
        }

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("eventoId", mensagem.eventoId);
        intent.putExtra("outroUserId", mensagem.remetenteId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) (msgId % Integer.MAX_VALUE),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String texto = mensagem.texto != null && !mensagem.texto.trim().isEmpty()
                ? mensagem.texto.trim()
                : "Você recebeu uma nova mensagem.";

        NotificationManagerCompat.from(this).notify(
                Math.abs(("msg_" + msgId).hashCode()),
                NotificationHelper.baseMessageNotification(this)
                        .setContentTitle("Nova mensagem no CapiHub")
                        .setContentText(texto)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                        .setContentIntent(pendingIntent)
                        .build()
        );
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public void onDestroy() {
        if (eventListener != null) {
            eventListener.remove();
            eventListener = null;
        }
        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
