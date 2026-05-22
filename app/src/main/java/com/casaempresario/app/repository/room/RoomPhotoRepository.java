package com.casaempresario.app.repository.room;

import android.content.Context;
import com.casaempresario.app.database.AppDatabase;
import com.casaempresario.app.model.EventPhoto;
import com.casaempresario.app.repository.PhotoRepository;
import com.casaempresario.app.repository.RepositoryCallback;
import java.util.List;

public class RoomPhotoRepository implements PhotoRepository {
    private final AppDatabase db;

    public RoomPhotoRepository(Context context) {
        this.db = AppDatabase.getDatabase(context);
    }

    @Override
    public void insert(EventPhoto foto, RepositoryCallback<Void> callback) {
        new Thread(() -> {
            try {
                db.fotoDao().insert(foto);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    @Override
    public void getFotosByEvento(long eventoId, RepositoryCallback<List<EventPhoto>> callback) {
        new Thread(() -> {
            try {
                List<EventPhoto> fotos = db.fotoDao().getFotosByEvento(eventoId);
                callback.onSuccess(fotos);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    @Override
    public void getFotoById(Long fotoId, RepositoryCallback<EventPhoto> callback) {
        new Thread(() -> {
            try {
                EventPhoto foto = db.fotoDao().getFotoById(fotoId);
                callback.onSuccess(foto);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    @Override
    public void deleteById(Long fotoId, RepositoryCallback<Void> callback) {
        new Thread(() -> {
            try {
                db.fotoDao().deleteById(fotoId);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }
}
