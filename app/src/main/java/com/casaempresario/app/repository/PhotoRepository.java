package com.casaempresario.app.repository;

import com.casaempresario.app.model.EventPhoto;
import java.util.List;

public interface PhotoRepository {
    void insert(EventPhoto foto, RepositoryCallback<Void> callback);
    void getFotosByEvento(long eventoId, RepositoryCallback<List<EventPhoto>> callback);
    void getFotoById(Long fotoId, RepositoryCallback<EventPhoto> callback);
    void deleteById(Long fotoId, RepositoryCallback<Void> callback);
}
