package com.casaempresario.app.database;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class UsuarioDao_Impl implements UsuarioDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Usuario> __insertionAdapterOfUsuario;

  public UsuarioDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUsuario = new EntityInsertionAdapter<Usuario>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `usuarios` (`id`,`email`,`senha`,`nome`,`role`,`criado_em`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Usuario entity) {
        statement.bindLong(1, entity.id);
        if (entity.email == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.email);
        }
        if (entity.senha == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.senha);
        }
        if (entity.nome == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.nome);
        }
        if (entity.role == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.role);
        }
        if (entity.criadoEm == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.criadoEm);
        }
      }
    };
  }

  @Override
  public long insert(final Usuario usuario) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfUsuario.insertAndReturnId(usuario);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public Usuario login(final String email, final String senha) {
    final String _sql = "SELECT * FROM usuarios WHERE email = ? AND senha = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    if (email == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, email);
    }
    _argIndex = 2;
    if (senha == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, senha);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
      final int _cursorIndexOfSenha = CursorUtil.getColumnIndexOrThrow(_cursor, "senha");
      final int _cursorIndexOfNome = CursorUtil.getColumnIndexOrThrow(_cursor, "nome");
      final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
      final int _cursorIndexOfCriadoEm = CursorUtil.getColumnIndexOrThrow(_cursor, "criado_em");
      final Usuario _result;
      if (_cursor.moveToFirst()) {
        _result = new Usuario();
        _result.id = _cursor.getLong(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfEmail)) {
          _result.email = null;
        } else {
          _result.email = _cursor.getString(_cursorIndexOfEmail);
        }
        if (_cursor.isNull(_cursorIndexOfSenha)) {
          _result.senha = null;
        } else {
          _result.senha = _cursor.getString(_cursorIndexOfSenha);
        }
        if (_cursor.isNull(_cursorIndexOfNome)) {
          _result.nome = null;
        } else {
          _result.nome = _cursor.getString(_cursorIndexOfNome);
        }
        if (_cursor.isNull(_cursorIndexOfRole)) {
          _result.role = null;
        } else {
          _result.role = _cursor.getString(_cursorIndexOfRole);
        }
        if (_cursor.isNull(_cursorIndexOfCriadoEm)) {
          _result.criadoEm = null;
        } else {
          _result.criadoEm = _cursor.getString(_cursorIndexOfCriadoEm);
        }
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Usuario findByEmail(final String email) {
    final String _sql = "SELECT * FROM usuarios WHERE email = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (email == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, email);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
      final int _cursorIndexOfSenha = CursorUtil.getColumnIndexOrThrow(_cursor, "senha");
      final int _cursorIndexOfNome = CursorUtil.getColumnIndexOrThrow(_cursor, "nome");
      final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
      final int _cursorIndexOfCriadoEm = CursorUtil.getColumnIndexOrThrow(_cursor, "criado_em");
      final Usuario _result;
      if (_cursor.moveToFirst()) {
        _result = new Usuario();
        _result.id = _cursor.getLong(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfEmail)) {
          _result.email = null;
        } else {
          _result.email = _cursor.getString(_cursorIndexOfEmail);
        }
        if (_cursor.isNull(_cursorIndexOfSenha)) {
          _result.senha = null;
        } else {
          _result.senha = _cursor.getString(_cursorIndexOfSenha);
        }
        if (_cursor.isNull(_cursorIndexOfNome)) {
          _result.nome = null;
        } else {
          _result.nome = _cursor.getString(_cursorIndexOfNome);
        }
        if (_cursor.isNull(_cursorIndexOfRole)) {
          _result.role = null;
        } else {
          _result.role = _cursor.getString(_cursorIndexOfRole);
        }
        if (_cursor.isNull(_cursorIndexOfCriadoEm)) {
          _result.criadoEm = null;
        } else {
          _result.criadoEm = _cursor.getString(_cursorIndexOfCriadoEm);
        }
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Usuario getUsuarioById(final long id) {
    final String _sql = "SELECT * FROM usuarios WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
      final int _cursorIndexOfSenha = CursorUtil.getColumnIndexOrThrow(_cursor, "senha");
      final int _cursorIndexOfNome = CursorUtil.getColumnIndexOrThrow(_cursor, "nome");
      final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
      final int _cursorIndexOfCriadoEm = CursorUtil.getColumnIndexOrThrow(_cursor, "criado_em");
      final Usuario _result;
      if (_cursor.moveToFirst()) {
        _result = new Usuario();
        _result.id = _cursor.getLong(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfEmail)) {
          _result.email = null;
        } else {
          _result.email = _cursor.getString(_cursorIndexOfEmail);
        }
        if (_cursor.isNull(_cursorIndexOfSenha)) {
          _result.senha = null;
        } else {
          _result.senha = _cursor.getString(_cursorIndexOfSenha);
        }
        if (_cursor.isNull(_cursorIndexOfNome)) {
          _result.nome = null;
        } else {
          _result.nome = _cursor.getString(_cursorIndexOfNome);
        }
        if (_cursor.isNull(_cursorIndexOfRole)) {
          _result.role = null;
        } else {
          _result.role = _cursor.getString(_cursorIndexOfRole);
        }
        if (_cursor.isNull(_cursorIndexOfCriadoEm)) {
          _result.criadoEm = null;
        } else {
          _result.criadoEm = _cursor.getString(_cursorIndexOfCriadoEm);
        }
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
