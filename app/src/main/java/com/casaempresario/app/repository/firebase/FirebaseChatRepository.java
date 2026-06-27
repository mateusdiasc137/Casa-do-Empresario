package com.casaempresario.app.repository.firebase;

import com.casaempresario.app.database.Mensagem;
import com.casaempresario.app.repository.ChatRepository;
import com.casaempresario.app.repository.RepositoryCallback;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FirebaseChatRepository implements ChatRepository {
    private final FirebaseFirestore firestore;
    private static final String COLLECTION = "mensagens";

    public FirebaseChatRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public void insert(Mensagem mensagem, RepositoryCallback<Long> callback) {
        if (mensagem.id == 0) {
            mensagem.id = (System.currentTimeMillis() * 10000L) + (long) (Math.random() * 10000L);
        }
        firestore.collection(COLLECTION)
                .document(String.valueOf(mensagem.id))
                .set(mensagem)
                .addOnSuccessListener(aVoid -> callback.onSuccess(mensagem.id))
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void getChatThread(long userA, long userB, long eventoId, RepositoryCallback<List<Mensagem>> callback) {
        firestore.collection(COLLECTION)
                .whereEqualTo("eventoId", eventoId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Mensagem> thread = new ArrayList<>();
                    queryDocumentSnapshots.forEach(doc -> {
                        Mensagem msg = doc.toObject(Mensagem.class);
                        if (pertenceAConversa(msg, userA, userB)) {
                            thread.add(msg);
                        }
                    });
                    ordenarCronologico(thread);
                    callback.onSuccess(thread);
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public ChatRepository.ChatListener listenChatThread(long userA, long userB, long eventoId, RepositoryCallback<List<Mensagem>> callback) {
        ListenerRegistration registration = firestore.collection(COLLECTION)
                .whereEqualTo("eventoId", eventoId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onError(error);
                        return;
                    }
                    if (snapshots == null) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }

                    List<Mensagem> thread = new ArrayList<>();
                    snapshots.forEach(doc -> {
                        Mensagem msg = doc.toObject(Mensagem.class);
                        if (pertenceAConversa(msg, userA, userB)) {
                            thread.add(msg);
                        }
                    });
                    ordenarCronologico(thread);
                    callback.onSuccess(thread);
                });

        return registration::remove;
    }

    @Override
    public void getTodasMensagensUsuario(long userId, RepositoryCallback<List<Mensagem>> callback) {
        firestore.collection(COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Mensagem> mensagens = new ArrayList<>();
                    queryDocumentSnapshots.forEach(doc -> {
                        Mensagem msg = doc.toObject(Mensagem.class);
                        if (msg.remetenteId == userId || msg.destinatarioId == userId) {
                            mensagens.add(msg);
                        }
                    });
                    Collections.sort(mensagens, (m1, m2) -> Long.compare(m2.id, m1.id));
                    callback.onSuccess(mensagens);
                })
                .addOnFailureListener(callback::onError);
    }

    private boolean pertenceAConversa(Mensagem msg, long userA, long userB) {
        return msg != null &&
                ((msg.remetenteId == userA && msg.destinatarioId == userB) ||
                 (msg.remetenteId == userB && msg.destinatarioId == userA));
    }

    private void ordenarCronologico(List<Mensagem> mensagens) {
        Collections.sort(mensagens, (m1, m2) -> Long.compare(m1.id, m2.id));
    }
}
