package com.casaempresario.app.repository.firebase;

import android.net.Uri;
import com.casaempresario.app.database.Evento;
import com.casaempresario.app.repository.EventRepository;
import com.casaempresario.app.repository.RepositoryCallback;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FirebaseEventRepository implements EventRepository {
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;
    private static final String COLLECTION = "eventos";

    public FirebaseEventRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    @Override
    public void insert(Evento evento, RepositoryCallback<Long> callback) {
        if (evento.id == 0) {
            evento.id = System.currentTimeMillis() + (long) (Math.random() * 100000L);
        }
        
        // Se houver banner local, faz upload para o Storage primeiro
        if (evento.bannerUri != null && !evento.bannerUri.isEmpty() && !evento.bannerUri.startsWith("http")) {
            uploadBanner(evento.id, evento.bannerUri, new RepositoryCallback<String>() {
                @Override
                public void onSuccess(String downloadUrl) {
                    evento.bannerUri = downloadUrl;
                    saveToFirestore(evento, callback);
                }

                @Override
                public void onError(Exception e) {
                    callback.onError(e);
                }
            });
        } else {
            saveToFirestore(evento, callback);
        }
    }

    @Override
    public void update(Evento evento, RepositoryCallback<Void> callback) {
        // Se houver banner local, faz upload para o Storage primeiro
        if (evento.bannerUri != null && !evento.bannerUri.isEmpty() && !evento.bannerUri.startsWith("http")) {
            uploadBanner(evento.id, evento.bannerUri, new RepositoryCallback<String>() {
                @Override
                public void onSuccess(String downloadUrl) {
                    evento.bannerUri = downloadUrl;
                    updateInFirestore(evento, callback);
                }

                @Override
                public void onError(Exception e) {
                    callback.onError(e);
                }
            });
        } else {
            updateInFirestore(evento, callback);
        }
    }

    @Override
    public void deleteById(Long id, RepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION)
                .document(String.valueOf(id))
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void updateStatus(Long id, String novoStatus, RepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION)
                .document(String.valueOf(id))
                .update("status", novoStatus)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void getEventoById(long id, RepositoryCallback<Evento> callback) {
        firestore.collection(COLLECTION)
                .document(String.valueOf(id))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Evento e = documentSnapshot.toObject(Evento.class);
                        callback.onSuccess(e);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void getAllEventos(RepositoryCallback<List<Evento>> callback) {
        firestore.collection(COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Evento> eventos = new ArrayList<>();
                    queryDocumentSnapshots.forEach(doc -> {
                        Evento e = doc.toObject(Evento.class);
                        eventos.add(e);
                    });
                    callback.onSuccess(eventos);
                })
                .addOnFailureListener(callback::onError);
    }

    private void uploadBanner(long eventId, String localPath, RepositoryCallback<String> uploadCallback) {
        StorageReference ref = storage.getReference().child("banners/" + eventId + "_" + System.currentTimeMillis() + ".jpg");
        Uri fileUri;
        if (localPath.startsWith("content://") || localPath.startsWith("file://")) {
            fileUri = Uri.parse(localPath);
        } else {
            fileUri = Uri.fromFile(new File(localPath));
        }
        ref.putFile(fileUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> uploadCallback.onSuccess(uri.toString()))
                .addOnFailureListener(uploadCallback::onError);
    }

    private void saveToFirestore(Evento evento, RepositoryCallback<Long> callback) {
        firestore.collection(COLLECTION)
                .document(String.valueOf(evento.id))
                .set(evento)
                .addOnSuccessListener(aVoid -> callback.onSuccess(evento.id))
                .addOnFailureListener(callback::onError);
    }

    private void updateInFirestore(Evento evento, RepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION)
                .document(String.valueOf(evento.id))
                .set(evento)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }
}
