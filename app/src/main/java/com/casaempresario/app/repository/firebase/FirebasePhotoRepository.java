package com.casaempresario.app.repository.firebase;

import android.net.Uri;
import com.casaempresario.app.model.EventPhoto;
import com.casaempresario.app.repository.PhotoRepository;
import com.casaempresario.app.repository.RepositoryCallback;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FirebasePhotoRepository implements PhotoRepository {
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;
    private static final String COLLECTION = "fotos_evento";

    public FirebasePhotoRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    @Override
    public void insert(EventPhoto foto, RepositoryCallback<Void> callback) {
        if (foto.getId() == null) {
            foto.setId(System.currentTimeMillis() + (long) (Math.random() * 100000L));
        }

        // Se houver foto local, faz upload para o Storage primeiro
        if (foto.getUrlFoto() != null && !foto.getUrlFoto().isEmpty() && !foto.getUrlFoto().startsWith("http")) {
            uploadPhoto(foto.getId(), foto.getUrlFoto(), new RepositoryCallback<String>() {
                @Override
                public void onSuccess(String downloadUrl) {
                    foto.setUrlFoto(downloadUrl);
                    saveToFirestore(foto, callback);
                }

                @Override
                public void onError(Exception e) {
                    callback.onError(e);
                }
            });
        } else {
            saveToFirestore(foto, callback);
        }
    }

    @Override
    public void getFotosByEvento(long eventoId, RepositoryCallback<List<EventPhoto>> callback) {
        firestore.collection(COLLECTION)
                .whereEqualTo("eventoId", eventoId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<EventPhoto> fotos = new ArrayList<>();
                    queryDocumentSnapshots.forEach(doc -> {
                        EventPhoto f = doc.toObject(EventPhoto.class);
                        fotos.add(f);
                    });
                    // Ordenar por ID para manter ordem cronológica
                    Collections.sort(fotos, (f1, f2) -> Long.compare(f1.getId(), f2.getId()));
                    callback.onSuccess(fotos);
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void getFotoById(Long fotoId, RepositoryCallback<EventPhoto> callback) {
        firestore.collection(COLLECTION)
                .document(String.valueOf(fotoId))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        EventPhoto f = documentSnapshot.toObject(EventPhoto.class);
                        callback.onSuccess(f);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void deleteById(Long fotoId, RepositoryCallback<Void> callback) {
        // Primeiro busca a foto para obter a URL e poder deletar do Storage
        getFotoById(fotoId, new RepositoryCallback<EventPhoto>() {
            @Override
            public void onSuccess(EventPhoto foto) {
                if (foto != null) {
                    // Deleta do Firestore
                    firestore.collection(COLLECTION)
                            .document(String.valueOf(fotoId))
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Tenta deletar do Storage se for uma URL do Firebase
                                if (foto.getUrlFoto() != null && foto.getUrlFoto().contains("firebasestorage")) {
                                    try {
                                        StorageReference ref = storage.getReferenceFromUrl(foto.getUrlFoto());
                                        ref.delete();
                                    } catch (Exception ignored) {
                                    }
                                }
                                callback.onSuccess(null);
                            })
                            .addOnFailureListener(callback::onError);
                } else {
                    callback.onSuccess(null);
                }
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    private void uploadPhoto(long photoId, String localPath, RepositoryCallback<String> uploadCallback) {
        StorageReference ref = storage.getReference().child("event_photos/" + photoId + "_" + System.currentTimeMillis() + ".jpg");
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

    private void saveToFirestore(EventPhoto foto, RepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION)
                .document(String.valueOf(foto.getId()))
                .set(foto)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }
}
