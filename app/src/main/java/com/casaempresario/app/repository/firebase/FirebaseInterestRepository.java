package com.casaempresario.app.repository.firebase;

import com.casaempresario.app.database.Evento;
import com.casaempresario.app.database.Interesse;
import com.casaempresario.app.repository.InterestRepository;
import com.casaempresario.app.repository.RepositoryCallback;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class FirebaseInterestRepository implements InterestRepository {
    private final FirebaseFirestore firestore;
    private static final String COLLECTION = "interesses";

    public FirebaseInterestRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public void insert(Interesse interesse, RepositoryCallback<Long> callback) {
        if (interesse.id == 0) {
            interesse.id = System.currentTimeMillis() + (long) (Math.random() * 100000L);
        }
        firestore.collection(COLLECTION)
                .document(String.valueOf(interesse.id))
                .set(interesse)
                .addOnSuccessListener(aVoid -> callback.onSuccess(interesse.id))
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void deleteByUsuarioAndEvento(long usuarioId, long eventoId, RepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION)
                .whereEqualTo("usuarioId", usuarioId)
                .whereEqualTo("eventoId", eventoId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onSuccess(null);
                        return;
                    }
                    AtomicInteger counter = new AtomicInteger(queryDocumentSnapshots.size());
                    queryDocumentSnapshots.forEach(doc -> {
                        doc.getReference().delete()
                                .addOnCompleteListener(task -> {
                                    if (counter.decrementAndGet() == 0) {
                                        callback.onSuccess(null);
                                    }
                                });
                    });
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void getInteresse(long usuarioId, long eventoId, RepositoryCallback<Interesse> callback) {
        firestore.collection(COLLECTION)
                .whereEqualTo("usuarioId", usuarioId)
                .whereEqualTo("eventoId", eventoId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Interesse interesse = queryDocumentSnapshots.getDocuments().get(0).toObject(Interesse.class);
                        callback.onSuccess(interesse);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void getEventosDeInteresse(long usuarioId, RepositoryCallback<List<Evento>> callback) {
        firestore.collection(COLLECTION)
                .whereEqualTo("usuarioId", usuarioId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }
                    
                    List<Interesse> interesses = new ArrayList<>();
                    queryDocumentSnapshots.forEach(doc -> interesses.add(doc.toObject(Interesse.class)));
                    
                    List<Evento> eventos = new CopyOnWriteArrayList<>();
                    AtomicInteger counter = new AtomicInteger(interesses.size());
                    
                    for (Interesse inter : interesses) {
                        firestore.collection("eventos")
                                .document(String.valueOf(inter.eventoId))
                                .get()
                                .addOnSuccessListener(eventDoc -> {
                                    if (eventDoc.exists()) {
                                        eventos.add(eventDoc.toObject(Evento.class));
                                    }
                                    if (counter.decrementAndGet() == 0) {
                                        callback.onSuccess(new ArrayList<>(eventos));
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (counter.decrementAndGet() == 0) {
                                        callback.onSuccess(new ArrayList<>(eventos));
                                    }
                                });
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void deleteByEvento(long eventoId, RepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION)
                .whereEqualTo("eventoId", eventoId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onSuccess(null);
                        return;
                    }
                    AtomicInteger counter = new AtomicInteger(queryDocumentSnapshots.size());
                    queryDocumentSnapshots.forEach(doc -> {
                        doc.getReference().delete()
                                .addOnCompleteListener(task -> {
                                    if (counter.decrementAndGet() == 0) {
                                        callback.onSuccess(null);
                                    }
                                });
                    });
                })
                .addOnFailureListener(callback::onError);
    }
}
