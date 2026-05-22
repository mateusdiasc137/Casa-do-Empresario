package com.casaempresario.app.repository.firebase;

import com.casaempresario.app.database.Mensagem;
import com.casaempresario.app.repository.ChatRepository;
import com.casaempresario.app.repository.RepositoryCallback;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
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
            mensagem.id = System.currentTimeMillis() + (long) (Math.random() * 100000L);
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
                        if ((msg.remetenteId == userA && msg.destinatarioId == userB) ||
                            (msg.remetenteId == userB && msg.destinatarioId == userA)) {
                            thread.add(msg);
                        }
                    });
                    
                    // Ordenar por ID ou timestamp (ID gerado pelo currentTimeMillis é naturalmente ordenado se for sequencial,
                    // mas podemos ordenar usando uma comparação simples de timestamp ou ID)
                    Collections.sort(thread, (m1, m2) -> Long.compare(m1.id, m2.id));
                    
                    callback.onSuccess(thread);
                })
                .addOnFailureListener(callback::onError);
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
                    // Ordenar por ID decrescente (mais recente primeiro)
                    Collections.sort(mensagens, (m1, m2) -> Long.compare(m2.id, m1.id));
                    callback.onSuccess(mensagens);
                })
                .addOnFailureListener(callback::onError);
    }
}
