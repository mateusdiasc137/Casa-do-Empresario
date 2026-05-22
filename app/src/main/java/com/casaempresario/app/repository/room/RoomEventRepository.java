package com.casaempresario.app.repository.room;

import android.content.Context;
import com.casaempresario.app.database.AppDatabase;
import com.casaempresario.app.database.Evento;
import com.casaempresario.app.repository.EventRepository;
import com.casaempresario.app.repository.RepositoryCallback;
import java.util.List;

public class RoomEventRepository implements EventRepository {
    private final AppDatabase db;

    public RoomEventRepository(Context context) {
        this.db = AppDatabase.getDatabase(context);
    }

    @Override
    public void insert(Evento evento, RepositoryCallback<Long> callback) {
        new Thread(() -> {
            try {
                long id = db.eventoDao().insert(evento);
                callback.onSuccess(id);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    @Override
    public void update(Evento evento, RepositoryCallback<Void> callback) {
        new Thread(() -> {
            try {
                db.eventoDao().update(evento);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    @Override
    public void deleteById(Long id, RepositoryCallback<Void> callback) {
        new Thread(() -> {
            try {
                db.eventoDao().deleteById(id);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    @Override
    public void updateStatus(Long id, String novoStatus, RepositoryCallback<Void> callback) {
        new Thread(() -> {
            try {
                db.eventoDao().updateStatus(id, novoStatus);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    @Override
    public void getEventoById(long id, RepositoryCallback<Evento> callback) {
        new Thread(() -> {
            try {
                Evento e = db.eventoDao().getEventoById(id);
                callback.onSuccess(e);
            } catch (Exception ex) {
                callback.onError(ex);
            }
        }).start();
    }

    @Override
    public void getAllEventos(RepositoryCallback<List<Evento>> callback) {
        new Thread(() -> {
            try {
                List<Evento> eventos = db.eventoDao().getAllEventos();
                callback.onSuccess(eventos);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }
}
