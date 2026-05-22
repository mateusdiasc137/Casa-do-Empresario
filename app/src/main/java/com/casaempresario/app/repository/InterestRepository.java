package com.casaempresario.app.repository;

import com.casaempresario.app.database.Evento;
import com.casaempresario.app.database.Interesse;
import java.util.List;

public interface InterestRepository {
    void insert(Interesse interesse, RepositoryCallback<Long> callback);
    void deleteByUsuarioAndEvento(long usuarioId, long eventoId, RepositoryCallback<Void> callback);
    void getInteresse(long usuarioId, long eventoId, RepositoryCallback<Interesse> callback);
    void getEventosDeInteresse(long usuarioId, RepositoryCallback<List<Evento>> callback);
    void deleteByEvento(long eventoId, RepositoryCallback<Void> callback);
}
