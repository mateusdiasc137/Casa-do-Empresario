package com.casaempresario.app.model;

public class FeedPost {
    public String id;
    public long autorId;
    public String autorNome;
    public String autorEmail;
    public String autorRole;
    public String autorFotoUri;
    public String texto;
    public String imagemUri;
    public String tipo;
    public long createdAt;

    public FeedPost() {
        // Necessário para o Firebase Firestore
    }
}
