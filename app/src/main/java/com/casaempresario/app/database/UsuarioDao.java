package com.casaempresario.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface UsuarioDao {

    @Insert
    long insert(Usuario usuario);

    /** Retorna o usuário se email + senha baterem, senão retorna null. */
    @Query("SELECT * FROM usuarios WHERE email = :email AND senha = :senha LIMIT 1")
    Usuario login(String email, String senha);

    /** Verifica se já existe um usuário com este e-mail (para evitar duplicatas). */
    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    Usuario findByEmail(String email);
}
