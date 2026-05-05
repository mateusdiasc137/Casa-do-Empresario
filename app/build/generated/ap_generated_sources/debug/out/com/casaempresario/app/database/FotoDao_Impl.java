package com.casaempresario.app.database;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.casaempresario.app.model.EventPhoto;
import java.lang.Class;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class FotoDao_Impl implements FotoDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<EventPhoto> __insertionAdapterOfEventPhoto;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public FotoDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfEventPhoto = new EntityInsertionAdapter<EventPhoto>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `fotos_evento` (`id`,`eventoId`,`urlFoto`,`legenda`,`enviadoEm`,`usuarioNome`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final EventPhoto entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindLong(1, entity.getId());
        }
        statement.bindLong(2, entity.getEventoId());
        if (entity.getUrlFoto() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getUrlFoto());
        }
        if (entity.getLegenda() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getLegenda());
        }
        if (entity.getEnviadoEm() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getEnviadoEm());
        }
        if (entity.getUsuarioNome() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getUsuarioNome());
        }
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM fotos_evento WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public void insert(final EventPhoto foto) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfEventPhoto.insert(foto);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteById(final Long fotoId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
    int _argIndex = 1;
    if (fotoId == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindLong(_argIndex, fotoId);
    }
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteById.release(_stmt);
    }
  }

  @Override
  public List<EventPhoto> getFotosByEvento(final long eventoId) {
    final String _sql = "SELECT * FROM fotos_evento WHERE eventoId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, eventoId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfEventoId = CursorUtil.getColumnIndexOrThrow(_cursor, "eventoId");
      final int _cursorIndexOfUrlFoto = CursorUtil.getColumnIndexOrThrow(_cursor, "urlFoto");
      final int _cursorIndexOfLegenda = CursorUtil.getColumnIndexOrThrow(_cursor, "legenda");
      final int _cursorIndexOfEnviadoEm = CursorUtil.getColumnIndexOrThrow(_cursor, "enviadoEm");
      final int _cursorIndexOfUsuarioNome = CursorUtil.getColumnIndexOrThrow(_cursor, "usuarioNome");
      final List<EventPhoto> _result = new ArrayList<EventPhoto>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final EventPhoto _item;
        _item = new EventPhoto();
        final Long _tmpId;
        if (_cursor.isNull(_cursorIndexOfId)) {
          _tmpId = null;
        } else {
          _tmpId = _cursor.getLong(_cursorIndexOfId);
        }
        _item.setId(_tmpId);
        final long _tmpEventoId;
        _tmpEventoId = _cursor.getLong(_cursorIndexOfEventoId);
        _item.setEventoId(_tmpEventoId);
        final String _tmpUrlFoto;
        if (_cursor.isNull(_cursorIndexOfUrlFoto)) {
          _tmpUrlFoto = null;
        } else {
          _tmpUrlFoto = _cursor.getString(_cursorIndexOfUrlFoto);
        }
        _item.setUrlFoto(_tmpUrlFoto);
        final String _tmpLegenda;
        if (_cursor.isNull(_cursorIndexOfLegenda)) {
          _tmpLegenda = null;
        } else {
          _tmpLegenda = _cursor.getString(_cursorIndexOfLegenda);
        }
        _item.setLegenda(_tmpLegenda);
        final String _tmpEnviadoEm;
        if (_cursor.isNull(_cursorIndexOfEnviadoEm)) {
          _tmpEnviadoEm = null;
        } else {
          _tmpEnviadoEm = _cursor.getString(_cursorIndexOfEnviadoEm);
        }
        _item.setEnviadoEm(_tmpEnviadoEm);
        final String _tmpUsuarioNome;
        if (_cursor.isNull(_cursorIndexOfUsuarioNome)) {
          _tmpUsuarioNome = null;
        } else {
          _tmpUsuarioNome = _cursor.getString(_cursorIndexOfUsuarioNome);
        }
        _item.setUsuarioNome(_tmpUsuarioNome);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public EventPhoto getFotoById(final Long fotoId) {
    final String _sql = "SELECT * FROM fotos_evento WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (fotoId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, fotoId);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfEventoId = CursorUtil.getColumnIndexOrThrow(_cursor, "eventoId");
      final int _cursorIndexOfUrlFoto = CursorUtil.getColumnIndexOrThrow(_cursor, "urlFoto");
      final int _cursorIndexOfLegenda = CursorUtil.getColumnIndexOrThrow(_cursor, "legenda");
      final int _cursorIndexOfEnviadoEm = CursorUtil.getColumnIndexOrThrow(_cursor, "enviadoEm");
      final int _cursorIndexOfUsuarioNome = CursorUtil.getColumnIndexOrThrow(_cursor, "usuarioNome");
      final EventPhoto _result;
      if (_cursor.moveToFirst()) {
        _result = new EventPhoto();
        final Long _tmpId;
        if (_cursor.isNull(_cursorIndexOfId)) {
          _tmpId = null;
        } else {
          _tmpId = _cursor.getLong(_cursorIndexOfId);
        }
        _result.setId(_tmpId);
        final long _tmpEventoId;
        _tmpEventoId = _cursor.getLong(_cursorIndexOfEventoId);
        _result.setEventoId(_tmpEventoId);
        final String _tmpUrlFoto;
        if (_cursor.isNull(_cursorIndexOfUrlFoto)) {
          _tmpUrlFoto = null;
        } else {
          _tmpUrlFoto = _cursor.getString(_cursorIndexOfUrlFoto);
        }
        _result.setUrlFoto(_tmpUrlFoto);
        final String _tmpLegenda;
        if (_cursor.isNull(_cursorIndexOfLegenda)) {
          _tmpLegenda = null;
        } else {
          _tmpLegenda = _cursor.getString(_cursorIndexOfLegenda);
        }
        _result.setLegenda(_tmpLegenda);
        final String _tmpEnviadoEm;
        if (_cursor.isNull(_cursorIndexOfEnviadoEm)) {
          _tmpEnviadoEm = null;
        } else {
          _tmpEnviadoEm = _cursor.getString(_cursorIndexOfEnviadoEm);
        }
        _result.setEnviadoEm(_tmpEnviadoEm);
        final String _tmpUsuarioNome;
        if (_cursor.isNull(_cursorIndexOfUsuarioNome)) {
          _tmpUsuarioNome = null;
        } else {
          _tmpUsuarioNome = _cursor.getString(_cursorIndexOfUsuarioNome);
        }
        _result.setUsuarioNome(_tmpUsuarioNome);
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
