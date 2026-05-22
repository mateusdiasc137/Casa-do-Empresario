package com.casaempresario.app.repository.room;

import android.content.Context;
import com.casaempresario.app.database.AppDatabase;
import com.casaempresario.app.database.Usuario;
import com.casaempresario.app.repository.RepositoryCallback;
import com.casaempresario.app.repository.UserRepository;

public class RoomUserRepository implements UserRepository {
    private final AppDatabase db;

    public RoomUserRepository(Context context) {
        this.db = AppDatabase.getDatabase(context);
    }

    @Override
    public void login(String email, String senha, RepositoryCallback<Usuario> callback) {
        new Thread(() -> {
            try {
                Usuario u = db.usuarioDao().login(email, senha);
                callback.onSuccess(u);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    @Override
    public void findByEmail(String email, RepositoryCallback<Usuario> callback) {
        new Thread(() -> {
            try {
                Usuario u = db.usuarioDao().findByEmail(email);
                callback.onSuccess(u);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    @Override
    public void getUsuarioById(long id, RepositoryCallback<Usuario> callback) {
        new Thread(() -> {
            try {
                Usuario u = db.usuarioDao().getUsuarioById(id);
                callback.onSuccess(u);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    @Override
    public void insert(Usuario usuario, RepositoryCallback<Long> callback) {
        new Thread(() -> {
            try {
                long id = db.usuarioDao().insert(usuario);
                callback.onSuccess(id);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }
}
