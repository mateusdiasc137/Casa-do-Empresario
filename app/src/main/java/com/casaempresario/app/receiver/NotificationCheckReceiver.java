package com.casaempresario.app.receiver;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.casaempresario.app.activity.EventDetailActivity;
import com.casaempresario.app.activity.MainActivity;
import com.casaempresario.app.database.Evento;
import com.casaempresario.app.util.NotificationHelper;
import com.casaempresario.app.util.NotificationScheduler;
import com.casaempresario.app.util.SessionManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashSet;
import java.util.Set;

public class NotificationCheckReceiver extends BroadcastReceiver {
    public static final String ACTION_CHECK_NEW_EVENTS = "com.casaempresario.app.ACTION_CHECK_NEW_EVENTS";

    private static final String PREFS = "CasaEmpresarioNotifications";
    private static final String KEY_SEEN_EVENTS = "seen_event_ids";
    private static final String KEY_SEEN_FEED = "seen_feed_posts";

    @Override
    public void onReceive(Context context, Intent intent) {
        Context appContext = context.getApplicationContext();
        SessionManager sessionManager = new SessionManager(appContext);

        if (!sessionManager.isLogado()) {
            return;
        }

        // Reagenda a próxima verificação caso o sistema tenha removido o alarme anterior.
        NotificationScheduler.scheduleEventChecks(appContext);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        PendingResult pendingResult = goAsync();
        NotificationHelper.createNotificationChannel(appContext);

        FirebaseFirestore.getInstance()
                .collection("eventos")
                .get()
                .addOnSuccessListener(snapshots -> {
                    try {
                        SharedPreferences prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                        Set<String> seen = new HashSet<>(prefs.getStringSet(KEY_SEEN_EVENTS, new HashSet<>()));
                        boolean changed = false;
                        boolean primeiraCarga = seen.isEmpty();

                        for (QueryDocumentSnapshot document : snapshots) {
                            Evento evento = document.toObject(Evento.class);
                            String eventId = String.valueOf(evento.id != 0 ? evento.id : document.getId());
                            String statusAtual = evento.status != null ? evento.status : "AGENDADO";
                            String statusKey = "event_status_" + eventId;
                            String statusAnterior = prefs.getString(statusKey, null);

                            if (primeiraCarga) {
                                if (seen.add(eventId)) {
                                    changed = true;
                                }
                                prefs.edit().putString(statusKey, statusAtual).apply();
                                continue;
                            }

                            if (seen.contains(eventId)) {
                                if (statusAnterior != null && !statusAnterior.equalsIgnoreCase(statusAtual) && deveNotificar(evento, sessionManager)) {
                                    mostrarNotificacaoAtualizacaoEvento(appContext, evento, eventId, statusAtual);
                                }
                                prefs.edit().putString(statusKey, statusAtual).apply();
                                continue;
                            }

                            seen.add(eventId);
                            changed = true;
                            prefs.edit().putString(statusKey, statusAtual).apply();

                            if (deveNotificar(evento, sessionManager)) {
                                mostrarNotificacaoNovoEvento(appContext, evento, eventId);
                            }
                        }

                        if (changed) {
                            prefs.edit().putStringSet(KEY_SEEN_EVENTS, seen).apply();
                        }
                    } finally {
                        verificarComunicadosImportantes(appContext, sessionManager, pendingResult);
                    }
                })
                .addOnFailureListener(error -> verificarComunicadosImportantes(appContext, sessionManager, pendingResult));
    }

    private void verificarComunicadosImportantes(Context context, SessionManager sessionManager, PendingResult pendingResult) {
        FirebaseFirestore.getInstance()
                .collection("feed_posts")
                .get()
                .addOnSuccessListener(snapshots -> {
                    try {
                        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                        Set<String> seenFeed = new HashSet<>(prefs.getStringSet(KEY_SEEN_FEED, new HashSet<>()));
                        boolean changed = false;
                        boolean primeiraCarga = seenFeed.isEmpty();

                        for (QueryDocumentSnapshot document : snapshots) {
                            String postId = document.getId();
                            String tipo = document.getString("tipo");
                            Long autorId = document.getLong("autorId");
                            String texto = document.getString("texto");

                            if (primeiraCarga) {
                                if (seenFeed.add(postId)) changed = true;
                                continue;
                            }

                            if (seenFeed.contains(postId)) continue;
                            seenFeed.add(postId);
                            changed = true;

                            boolean oficial = "OFICIAL".equalsIgnoreCase(tipo);
                            boolean naoFoiMeuPost = autorId == null || autorId != sessionManager.getUserId();
                            if (oficial && naoFoiMeuPost) {
                                mostrarNotificacaoComunicado(context, postId, texto);
                            }
                        }

                        if (changed) {
                            prefs.edit().putStringSet(KEY_SEEN_FEED, seenFeed).apply();
                        }
                    } finally {
                        pendingResult.finish();
                    }
                })
                .addOnFailureListener(error -> pendingResult.finish());
    }

    private void mostrarNotificacaoComunicado(Context context, String postId, String texto) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                postId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String mensagem = texto != null && !texto.trim().isEmpty()
                ? texto.trim()
                : "A Casa do Empresário publicou um novo comunicado.";

        NotificationManagerCompat.from(context).notify(
                Math.abs(("feed_" + postId).hashCode()),
                NotificationHelper.baseEventNotification(context)
                        .setContentTitle("Comunicado importante")
                        .setContentText(mensagem)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(mensagem))
                        .setContentIntent(pendingIntent)
                        .build()
        );
    }

    private boolean deveNotificar(Evento evento, SessionManager sessionManager) {
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

    private void mostrarNotificacaoAtualizacaoEvento(Context context, Evento evento, String eventId, String statusAtual) {
        Intent intent = new Intent(context, EventDetailActivity.class);
        intent.putExtra("eventoId", evento.id);
        intent.putExtra("eventoTitulo", evento.titulo);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                ("update_" + eventId).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String titulo = evento.titulo != null && !evento.titulo.trim().isEmpty()
                ? evento.titulo.trim()
                : "Evento atualizado";

        String mensagem = "O status do evento mudou para: " + statusAtual;

        NotificationManagerCompat.from(context).notify(
                Math.abs(("update_" + eventId + statusAtual).hashCode()),
                NotificationHelper.baseEventNotification(context)
                        .setContentTitle("Atualização em evento")
                        .setContentText(titulo)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(titulo + "\n" + mensagem))
                        .setContentIntent(pendingIntent)
                        .build()
        );
    }

    private void mostrarNotificacaoNovoEvento(Context context, Evento evento, String eventId) {
        Intent intent = new Intent(context, EventDetailActivity.class);
        intent.putExtra("eventoId", evento.id);
        intent.putExtra("eventoTitulo", evento.titulo);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
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

        NotificationManagerCompat.from(context).notify(
                Math.abs(eventId.hashCode()),
                NotificationHelper.baseEventNotification(context)
                        .setContentTitle("Novo evento na Casa do Empresário")
                        .setContentText(titulo)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(mensagem + "\n" + local))
                        .setContentIntent(pendingIntent)
                        .build()
        );
    }
}
