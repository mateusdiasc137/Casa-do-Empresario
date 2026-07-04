package com.casaempresario.app.service;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.casaempresario.app.R;
import com.casaempresario.app.activity.ChatActivity;
import com.casaempresario.app.activity.EventDetailActivity;
import com.casaempresario.app.util.NotificationHelper;
import com.casaempresario.app.util.SessionManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Serviço FCM que recebe notificações push do Firebase Cloud Messaging.
 * Funciona mesmo com o app fechado ou em segundo plano.
 */
public class FCMService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";

    /**
     * Chamado quando o token FCM é gerado ou atualizado.
     * Salva o token no Firestore vinculado ao usuário logado.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Novo token FCM: " + token);
        salvarTokenNoFirestore(token);
    }

    /**
     * Chamado quando uma mensagem push chega do servidor.
     * Isso funciona mesmo com o app em segundo plano ou fechado.
     *
     * IMPORTANTE: Se a mensagem vier com "notification" payload (ex.: campanha do Console),
     * o Android exibe automaticamente quando o app está em SEGUNDO PLANO.
     * onMessageReceived só é chamado se o app estiver em PRIMEIRO PLANO,
     * ou se a mensagem for somente "data" payload (Cloud Functions).
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "onMessageReceived chamado. Data: " + remoteMessage.getData());

        // Cria os canais de notificação (caso ainda não existam)
        NotificationHelper.createNotificationChannel(this);

        // Verifica permissão de notificação
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Sem permissão POST_NOTIFICATIONS, ignorando.");
            return;
        }

        Map<String, String> data = remoteMessage.getData();
        String tipo = data.get("tipo");

        if ("mensagem".equals(tipo)) {
            mostrarNotificacaoMensagem(data);
        } else if ("evento".equals(tipo)) {
            mostrarNotificacaoEvento(data);
        } else if ("evento_atualizacao".equals(tipo)) {
            mostrarNotificacaoAtualizacaoEvento(data);
        } else if ("feed".equals(tipo)) {
            mostrarNotificacaoFeed(data);
        } else {
            // Notificação genérica (campanha do Console ou outro)
            RemoteMessage.Notification notification = remoteMessage.getNotification();
            if (notification != null) {
                Log.d(TAG, "Notificação genérica recebida: " + notification.getTitle());
                mostrarNotificacaoGenerica(
                        notification.getTitle(),
                        notification.getBody()
                );
            } else if (!data.isEmpty()) {
                // Data-only sem tipo conhecido
                Log.d(TAG, "Data-only sem tipo conhecido: " + data);
                mostrarNotificacaoGenerica(
                        data.getOrDefault("title", "CapiHub"),
                        data.getOrDefault("body", "Você tem uma nova notificação.")
                );
            }
        }
    }

    private void mostrarNotificacaoMensagem(Map<String, String> data) {
        String remetenteNome = data.get("remetenteNome");
        String texto = data.get("texto");
        String eventoIdStr = data.get("eventoId");
        String remetenteIdStr = data.get("remetenteId");

        if (remetenteNome == null) remetenteNome = "Alguém";
        if (texto == null) texto = "Nova mensagem recebida";

        // Não notifica se a mensagem for do próprio usuário
        SessionManager session = new SessionManager(this);
        if (session.isLogado() && remetenteIdStr != null) {
            try {
                long remetenteId = Long.parseLong(remetenteIdStr);
                if (remetenteId == session.getUserId()) {
                    return;
                }
            } catch (NumberFormatException ignored) {}
        }

        Intent intent = new Intent(this, ChatActivity.class);
        if (eventoIdStr != null) {
            try {
                intent.putExtra("eventoId", Long.parseLong(eventoIdStr));
            } catch (NumberFormatException ignored) {}
        }
        if (remetenteIdStr != null) {
            try {
                intent.putExtra("outroUserId", Long.parseLong(remetenteIdStr));
            } catch (NumberFormatException ignored) {}
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                ("msg_" + remetenteIdStr + "_" + eventoIdStr).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = NotificationHelper.baseMessageNotification(this)
                .setContentTitle(remetenteNome)
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE);

        int notificationId = ("chat_" + remetenteIdStr + "_" + eventoIdStr).hashCode();
        NotificationManagerCompat.from(this).notify(Math.abs(notificationId), builder.build());
    }

    private void mostrarNotificacaoEvento(Map<String, String> data) {
        String titulo = data.get("titulo");
        String local = data.get("local");
        String eventoIdStr = data.get("eventoId");

        if (titulo == null) titulo = "Novo evento publicado";

        Intent intent = new Intent(this, EventDetailActivity.class);
        if (eventoIdStr != null) {
            try {
                intent.putExtra("eventoId", Long.parseLong(eventoIdStr));
                intent.putExtra("eventoTitulo", titulo);
            } catch (NumberFormatException ignored) {}
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                ("evt_" + eventoIdStr).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String corpo = "Um organizador publicou: " + titulo;
        if (local != null && !local.isEmpty()) {
            corpo += "\nLocal: " + local;
        }

        NotificationCompat.Builder builder = NotificationHelper.baseEventNotification(this)
                .setContentTitle("Novo evento no CapiHub")
                .setContentText(titulo)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(corpo))
                .setContentIntent(pendingIntent);

        int notificationId = ("evt_" + eventoIdStr).hashCode();
        NotificationManagerCompat.from(this).notify(Math.abs(notificationId), builder.build());
    }

    private void mostrarNotificacaoAtualizacaoEvento(Map<String, String> data) {
        String titulo = data.get("titulo");
        String status = data.get("status");
        String eventoIdStr = data.get("eventoId");
        String mensagemCustomizada = data.get("mensagemCustomizada");

        if (titulo == null) titulo = "Evento atualizado";

        Intent intent = new Intent(this, EventDetailActivity.class);
        if (eventoIdStr != null) {
            try {
                intent.putExtra("eventoId", Long.parseLong(eventoIdStr));
                intent.putExtra("eventoTitulo", titulo);
            } catch (NumberFormatException ignored) {}
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                ("update_" + eventoIdStr).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String mensagem = (mensagemCustomizada != null && !mensagemCustomizada.trim().isEmpty())
                ? mensagemCustomizada
                : "O status do evento mudou para: " + (status != null ? status : "atualizado");

        NotificationCompat.Builder builder = NotificationHelper.baseEventNotification(this)
                .setContentTitle("Atualização em evento")
                .setContentText(titulo)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(titulo + "\n" + mensagem))
                .setContentIntent(pendingIntent);

        int notificationId = ("update_" + eventoIdStr + status).hashCode();
        NotificationManagerCompat.from(this).notify(Math.abs(notificationId), builder.build());
    }

    private void mostrarNotificacaoFeed(Map<String, String> data) {
        String texto = data.get("texto");
        String postId = data.get("postId");

        Intent intent = new Intent(this, com.casaempresario.app.activity.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (postId != null ? postId.hashCode() : (int) System.currentTimeMillis()),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String mensagem = texto != null && !texto.trim().isEmpty()
                ? texto.trim()
                : "A Casa do Empresário publicou um novo comunicado.";

        NotificationCompat.Builder builder = NotificationHelper.baseEventNotification(this)
                .setContentTitle("Comunicado importante")
                .setContentText(mensagem)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(mensagem))
                .setContentIntent(pendingIntent);

        int notificationId = ("feed_" + postId).hashCode();
        NotificationManagerCompat.from(this).notify(Math.abs(notificationId), builder.build());
    }

    private void mostrarNotificacaoGenerica(String titulo, String corpo) {
        if (titulo == null) titulo = "CapiHub";
        if (corpo == null) corpo = "Você tem uma nova notificação.";

        NotificationCompat.Builder builder = NotificationHelper.baseEventNotification(this)
                .setContentTitle(titulo)
                .setContentText(corpo);

        NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
    }

    /**
     * Salva o token FCM no Firestore, vinculado ao usuário logado.
     * Isso permite que o servidor saiba para qual dispositivo enviar a notificação.
     */
    private void salvarTokenNoFirestore(String token) {
        SessionManager session = new SessionManager(this);
        if (!session.isLogado()) {
            return;
        }

        long userId = session.getUserId();
        if (userId <= 0) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("fcm_tokens")
                .document(String.valueOf(userId))
                .set(java.util.Collections.singletonMap("token", token))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Token FCM salvo para usuário " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "Erro ao salvar token FCM", e));
    }

    /**
     * Método utilitário para forçar o registro/atualização do token FCM.
     * Deve ser chamado após o login do usuário.
     */
    public static void registrarToken(android.content.Context context) {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    Log.d(TAG, "Token FCM obtido: " + token);
                    SessionManager session = new SessionManager(context);
                    if (session.isLogado() && session.getUserId() > 0) {
                        FirebaseFirestore.getInstance()
                                .collection("fcm_tokens")
                                .document(String.valueOf(session.getUserId()))
                                .set(java.util.Collections.singletonMap("token", token))
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Token FCM registrado com sucesso"))
                                .addOnFailureListener(e -> Log.e(TAG, "Falha ao registrar token", e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Erro ao obter token FCM", e));
    }
}
