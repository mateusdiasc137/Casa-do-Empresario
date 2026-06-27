package com.casaempresario.app.repository;

import com.casaempresario.app.database.Mensagem;
import java.util.List;

public interface ChatRepository {
    interface ChatListener {
        void remove();
    }

    void insert(Mensagem mensagem, RepositoryCallback<Long> callback);
    void getChatThread(long userA, long userB, long eventoId, RepositoryCallback<List<Mensagem>> callback);
    ChatListener listenChatThread(long userA, long userB, long eventoId, RepositoryCallback<List<Mensagem>> callback);
    void getTodasMensagensUsuario(long userId, RepositoryCallback<List<Mensagem>> callback);
}
