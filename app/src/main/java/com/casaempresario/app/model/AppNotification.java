package com.casaempresario.app.model;

/**
 * Item consolidado para a central interna de notificações.
 */
public class AppNotification {
    public String id;
    public String titulo;
    public String descricao;
    public String tipo;
    public long timestamp;
    public boolean destaque;

    public AppNotification() {
    }

    public AppNotification(String id, String titulo, String descricao, String tipo, long timestamp, boolean destaque) {
        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
        this.tipo = tipo;
        this.timestamp = timestamp;
        this.destaque = destaque;
    }
}
