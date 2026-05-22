package com.casaempresario.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface MensagemDao {

    @Insert
    long insert(Mensagem mensagem);

    @Query("SELECT * FROM mensagens WHERE ((remetente_id = :userA AND destinatario_id = :userB) OR (remetente_id = :userB AND destinatario_id = :userA)) AND evento_id = :eventoId ORDER BY id ASC")
    List<Mensagem> getChatThread(long userA, long userB, long eventoId);

    @Query("SELECT * FROM mensagens WHERE remetente_id = :userId OR destinatario_id = :userId ORDER BY id DESC")
    List<Mensagem> getTodasMensagensUsuario(long userId);
}
