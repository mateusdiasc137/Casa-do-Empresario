package com.casaempresario.app.repository.firebase;

import com.casaempresario.app.database.Usuario;
import com.casaempresario.app.repository.RepositoryCallback;
import com.casaempresario.app.repository.UserRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

public class FirebaseUserRepository implements UserRepository {
    private final FirebaseFirestore firestore;
    private static final String COLLECTION = "usuarios";

    public FirebaseUserRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public void login(String email, String senha, RepositoryCallback<Usuario> callback) {
        firestore.collection(COLLECTION)
                .whereEqualTo("email", email)
                .whereEqualTo("senha", senha)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        Usuario u = doc.toObject(Usuario.class);
                        hidratarId(doc, u);
                        callback.onSuccess(u);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void findByEmail(String email, RepositoryCallback<Usuario> callback) {
        firestore.collection(COLLECTION)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        Usuario u = doc.toObject(Usuario.class);
                        hidratarId(doc, u);
                        callback.onSuccess(u);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void getUsuarioById(long id, RepositoryCallback<Usuario> callback) {
        firestore.collection(COLLECTION)
                .document(String.valueOf(id))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Usuario u = documentSnapshot.toObject(Usuario.class);
                        hidratarId(documentSnapshot, u);
                        callback.onSuccess(u);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void insert(Usuario usuario, RepositoryCallback<Long> callback) {
        if (usuario.id == 0) {
            usuario.id = System.currentTimeMillis() + (long) (Math.random() * 100000L);
        }
        firestore.collection(COLLECTION)
                .document(String.valueOf(usuario.id))
                .set(usuario)
                .addOnSuccessListener(aVoid -> callback.onSuccess(usuario.id))
                .addOnFailureListener(callback::onError);
    }
    @Override
    public void updateFotoPerfil(long id, String fotoPerfilUri, RepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION)
                .document(String.valueOf(id))
                .update("fotoPerfilUri", fotoPerfilUri)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    private void hidratarId(DocumentSnapshot doc, Usuario usuario) {
        if (usuario != null && usuario.id == 0) {
            try {
                usuario.id = Long.parseLong(doc.getId());
            } catch (Exception ignored) {
                // Mantém 0 se o documento não usa ID numérico.
            }
        }
    }

}
