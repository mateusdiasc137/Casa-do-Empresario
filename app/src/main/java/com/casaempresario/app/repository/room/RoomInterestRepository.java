package com.casaempresario.app.repository.room;

import android.content.Context;
import com.casaempresario.app.database.AppDatabase;
import com.casaempresario.app.database.Evento;
import com.casaempresario.app.database.Interesse;
import com.casaempresario.app.repository.InterestRepository;
import com.casaempresario.app.repository.RepositoryCallback;
import java.util.List;

public class RoomInterestRepository implements InterestRepository {
    private final AppDatabase db;

    public RoomInterestRepository(Context context) {
        this.db = AppDatabase.getDatabase(context);
    }

    @Override
    public void insert(Interesse interesse, RepositoryCallback<Long> callback) {
        new Thread(() -> {
            try {
                long id = db.interesseDao().insert(interesse);
                callback.onSuccess(id);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    @Override
    public void deleteByUsuarioAndEvento(long usuarioId, long eventoId, RepositoryCallback<Void> callback) {
        new Thread(() -> {
            try {
                db.interesseDao().deleteByUsuarioAndEvento(usuarioId, eventoId);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    @Override
    public void getInteresse(long usuarioId, long eventoId, RepositoryCallback<Interesse> callback) {
        new Thread(() -> {
            try {
                Interesse interesse = db.interesseDao().getInteresse(usuarioId, eventoId);
                callback.onSuccess(interesse);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    @Override
    public void getEventosDeInteresse(long usuarioId, RepositoryCallback<List<Evento>> callback) {
        new Thread(() -> {
            try {
                List<Evento> eventos = db.interesseDao().getEventosDeInteresse(usuarioId);
                callback.onSuccess(eventos);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    @Override
    public void deleteByEvento(long eventoId, RepositoryCallback<Void> callback) {
        new Thread(() -> {
            try {
                db.interesseDao().deleteByEvento(eventoId);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }
}
