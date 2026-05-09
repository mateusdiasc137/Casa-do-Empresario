package com.casaempresario.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.casaempresario.app.model.EventPhoto;

import java.util.List;

@Dao
public interface FotoDao {
    @Insert
    void insert(EventPhoto foto);

    @Query("SELECT * FROM fotos_evento WHERE eventoId = :eventoId")
    List<EventPhoto> getFotosByEvento(long eventoId);

    // ✅  método para buscar uma foto específica pelo ID
    @Query("SELECT * FROM fotos_evento WHERE id = :fotoId")
    EventPhoto getFotoById(Long fotoId);

    // ✅  método para deletar a entrada no banco
    @Query("DELETE FROM fotos_evento WHERE id = :fotoId")
    void deleteById(Long fotoId);
}
