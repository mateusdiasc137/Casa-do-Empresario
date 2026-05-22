package com.casaempresario.app.repository.room;

import android.content.Context;
import com.casaempresario.app.database.AppDatabase;
import com.casaempresario.app.database.Mensagem;
import com.casaempresario.app.repository.ChatRepository;
import com.casaempresario.app.repository.RepositoryCallback;
import java.util.List;

public class RoomChatRepository implements ChatRepository {
    private final AppDatabase db;

    public RoomChatRepository(Context context) {
        this.db = AppDatabase.getDatabase(context);
    }

    @Override
    public void insert(Mensagem mensagem, RepositoryCallback<Long> callback) {
        new Thread(() -> {
            try {
                long id = db.mensagemDao().insert(mensagem);
                callback.onSuccess(id);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    @Override
    public void getChatThread(long userA, long userB, long eventoId, RepositoryCallback<List<Mensagem>> callback) {
        new Thread(() -> {
            try {
                List<Mensagem> mensagens = db.mensagemDao().getChatThread(userA, userB, eventoId);
                callback.onSuccess(mensagens);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    @Override
    public void getTodasMensagensUsuario(long userId, RepositoryCallback<List<Mensagem>> callback) {
        new Thread(() -> {
            try {
                List<Mensagem> mensagens = db.mensagemDao().getTodasMensagensUsuario(userId);
                callback.onSuccess(mensagens);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }
}
