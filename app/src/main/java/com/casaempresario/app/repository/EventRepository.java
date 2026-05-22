package com.casaempresario.app.repository;

import com.casaempresario.app.database.Evento;
import java.util.List;

public interface EventRepository {
    void insert(Evento evento, RepositoryCallback<Long> callback);
    void update(Evento evento, RepositoryCallback<Void> callback);
    void deleteById(Long id, RepositoryCallback<Void> callback);
    void updateStatus(Long id, String novoStatus, RepositoryCallback<Void> callback);
    void getEventoById(long id, RepositoryCallback<Evento> callback);
    void getAllEventos(RepositoryCallback<List<Evento>> callback);
}
