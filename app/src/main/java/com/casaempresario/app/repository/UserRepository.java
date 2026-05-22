package com.casaempresario.app.repository;

import com.casaempresario.app.database.Usuario;

public interface UserRepository {
    void login(String email, String senha, RepositoryCallback<Usuario> callback);
    void findByEmail(String email, RepositoryCallback<Usuario> callback);
    void getUsuarioById(long id, RepositoryCallback<Usuario> callback);
    void insert(Usuario usuario, RepositoryCallback<Long> callback);
}
