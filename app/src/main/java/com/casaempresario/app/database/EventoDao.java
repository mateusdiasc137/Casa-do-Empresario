package com.casaempresario.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface EventoDao {

    @Insert
    long insert(Evento evento);

    @Update
    void update(Evento evento);

    @Query("SELECT * FROM eventos ORDER BY data_evento DESC")
    List<Evento> getAllEventos();

    @Query("SELECT * FROM eventos WHERE id = :id")
    Evento getEventoById(long id);

    @Query("SELECT * FROM eventos WHERE id = :id")
    Evento getEventoById(Long id);

    @Query("DELETE FROM eventos WHERE id = :id")
    void deleteById(Long id);

    /** Atualiza apenas o status do evento, sem reescrever os outros campos. */
    @Query("UPDATE eventos SET status = :novoStatus WHERE id = :id")
    void updateStatus(Long id, String novoStatus);
}