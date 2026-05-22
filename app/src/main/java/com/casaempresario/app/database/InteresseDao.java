package com.casaempresario.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface InteresseDao {

    @Insert
    long insert(Interesse interesse);

    @Query("DELETE FROM interesses WHERE usuario_id = :usuarioId AND evento_id = :eventoId")
    void deleteByUsuarioAndEvento(long usuarioId, long eventoId);

    @Query("SELECT * FROM interesses WHERE usuario_id = :usuarioId AND evento_id = :eventoId LIMIT 1")
    Interesse getInteresse(long usuarioId, long eventoId);

    @Query("SELECT * FROM interesses WHERE usuario_id = :usuarioId")
    List<Interesse> getInteressesByUser(long usuarioId);

    @Query("SELECT e.* FROM eventos e INNER JOIN interesses i ON e.id = i.evento_id WHERE i.usuario_id = :usuarioId")
    List<Evento> getEventosDeInteresse(long usuarioId);

    @Query("DELETE FROM interesses WHERE evento_id = :eventoId")
    void deleteByEvento(long eventoId);
}
