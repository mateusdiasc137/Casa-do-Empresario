package com.casaempresario.app.database;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class EventoDao_Impl implements EventoDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Evento> __insertionAdapterOfEvento;

  private final EntityDeletionOrUpdateAdapter<Evento> __updateAdapterOfEvento;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public EventoDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfEvento = new EntityInsertionAdapter<Evento>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `eventos` (`id`,`titulo`,`descricao`,`data_evento`,`local`,`capacidade_maxima`,`status`,`criado_por`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Evento entity) {
        statement.bindLong(1, entity.id);
        if (entity.titulo == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.titulo);
        }
        if (entity.descricao == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.descricao);
        }
        if (entity.dataEvento == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.dataEvento);
        }
        if (entity.local == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.local);
        }
        if (entity.capacidadeMaxima == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.capacidadeMaxima);
        }
        if (entity.status == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.status);
        }
        if (entity.criadoPor == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.criadoPor);
        }
      }
    };
    this.__updateAdapterOfEvento = new EntityDeletionOrUpdateAdapter<Evento>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `eventos` SET `id` = ?,`titulo` = ?,`descricao` = ?,`data_evento` = ?,`local` = ?,`capacidade_maxima` = ?,`status` = ?,`criado_por` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Evento entity) {
        statement.bindLong(1, entity.id);
        if (entity.titulo == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.titulo);
        }
        if (entity.descricao == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.descricao);
        }
        if (entity.dataEvento == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.dataEvento);
        }
        if (entity.local == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.local);
        }
        if (entity.capacidadeMaxima == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.capacidadeMaxima);
        }
        if (entity.status == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.status);
        }
        if (entity.criadoPor == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.criadoPor);
        }
        statement.bindLong(9, entity.id);
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM eventos WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public long insert(final Evento evento) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfEvento.insertAndReturnId(evento);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final Evento evento) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfEvento.handle(evento);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteById(final Long id) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
    int _argIndex = 1;
    if (id == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindLong(_argIndex, id);
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
  public List<Evento> getAllEventos() {
    final String _sql = "SELECT * FROM eventos ORDER BY data_evento DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfTitulo = CursorUtil.getColumnIndexOrThrow(_cursor, "titulo");
      final int _cursorIndexOfDescricao = CursorUtil.getColumnIndexOrThrow(_cursor, "descricao");
      final int _cursorIndexOfDataEvento = CursorUtil.getColumnIndexOrThrow(_cursor, "data_evento");
      final int _cursorIndexOfLocal = CursorUtil.getColumnIndexOrThrow(_cursor, "local");
      final int _cursorIndexOfCapacidadeMaxima = CursorUtil.getColumnIndexOrThrow(_cursor, "capacidade_maxima");
      final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
      final int _cursorIndexOfCriadoPor = CursorUtil.getColumnIndexOrThrow(_cursor, "criado_por");
      final List<Evento> _result = new ArrayList<Evento>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final Evento _item;
        _item = new Evento();
        _item.id = _cursor.getLong(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfTitulo)) {
          _item.titulo = null;
        } else {
          _item.titulo = _cursor.getString(_cursorIndexOfTitulo);
        }
        if (_cursor.isNull(_cursorIndexOfDescricao)) {
          _item.descricao = null;
        } else {
          _item.descricao = _cursor.getString(_cursorIndexOfDescricao);
        }
        if (_cursor.isNull(_cursorIndexOfDataEvento)) {
          _item.dataEvento = null;
        } else {
          _item.dataEvento = _cursor.getString(_cursorIndexOfDataEvento);
        }
        if (_cursor.isNull(_cursorIndexOfLocal)) {
          _item.local = null;
        } else {
          _item.local = _cursor.getString(_cursorIndexOfLocal);
        }
        if (_cursor.isNull(_cursorIndexOfCapacidadeMaxima)) {
          _item.capacidadeMaxima = null;
        } else {
          _item.capacidadeMaxima = _cursor.getInt(_cursorIndexOfCapacidadeMaxima);
        }
        if (_cursor.isNull(_cursorIndexOfStatus)) {
          _item.status = null;
        } else {
          _item.status = _cursor.getString(_cursorIndexOfStatus);
        }
        if (_cursor.isNull(_cursorIndexOfCriadoPor)) {
          _item.criadoPor = null;
        } else {
          _item.criadoPor = _cursor.getLong(_cursorIndexOfCriadoPor);
        }
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Evento getEventoById(final long id) {
    final String _sql = "SELECT * FROM eventos WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfTitulo = CursorUtil.getColumnIndexOrThrow(_cursor, "titulo");
      final int _cursorIndexOfDescricao = CursorUtil.getColumnIndexOrThrow(_cursor, "descricao");
      final int _cursorIndexOfDataEvento = CursorUtil.getColumnIndexOrThrow(_cursor, "data_evento");
      final int _cursorIndexOfLocal = CursorUtil.getColumnIndexOrThrow(_cursor, "local");
      final int _cursorIndexOfCapacidadeMaxima = CursorUtil.getColumnIndexOrThrow(_cursor, "capacidade_maxima");
      final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
      final int _cursorIndexOfCriadoPor = CursorUtil.getColumnIndexOrThrow(_cursor, "criado_por");
      final Evento _result;
      if (_cursor.moveToFirst()) {
        _result = new Evento();
        _result.id = _cursor.getLong(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfTitulo)) {
          _result.titulo = null;
        } else {
          _result.titulo = _cursor.getString(_cursorIndexOfTitulo);
        }
        if (_cursor.isNull(_cursorIndexOfDescricao)) {
          _result.descricao = null;
        } else {
          _result.descricao = _cursor.getString(_cursorIndexOfDescricao);
        }
        if (_cursor.isNull(_cursorIndexOfDataEvento)) {
          _result.dataEvento = null;
        } else {
          _result.dataEvento = _cursor.getString(_cursorIndexOfDataEvento);
        }
        if (_cursor.isNull(_cursorIndexOfLocal)) {
          _result.local = null;
        } else {
          _result.local = _cursor.getString(_cursorIndexOfLocal);
        }
        if (_cursor.isNull(_cursorIndexOfCapacidadeMaxima)) {
          _result.capacidadeMaxima = null;
        } else {
          _result.capacidadeMaxima = _cursor.getInt(_cursorIndexOfCapacidadeMaxima);
        }
        if (_cursor.isNull(_cursorIndexOfStatus)) {
          _result.status = null;
        } else {
          _result.status = _cursor.getString(_cursorIndexOfStatus);
        }
        if (_cursor.isNull(_cursorIndexOfCriadoPor)) {
          _result.criadoPor = null;
        } else {
          _result.criadoPor = _cursor.getLong(_cursorIndexOfCriadoPor);
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
  public Evento getEventoById(final Long id) {
    final String _sql = "SELECT * FROM eventos WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (id == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, id);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfTitulo = CursorUtil.getColumnIndexOrThrow(_cursor, "titulo");
      final int _cursorIndexOfDescricao = CursorUtil.getColumnIndexOrThrow(_cursor, "descricao");
      final int _cursorIndexOfDataEvento = CursorUtil.getColumnIndexOrThrow(_cursor, "data_evento");
      final int _cursorIndexOfLocal = CursorUtil.getColumnIndexOrThrow(_cursor, "local");
      final int _cursorIndexOfCapacidadeMaxima = CursorUtil.getColumnIndexOrThrow(_cursor, "capacidade_maxima");
      final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
      final int _cursorIndexOfCriadoPor = CursorUtil.getColumnIndexOrThrow(_cursor, "criado_por");
      final Evento _result;
      if (_cursor.moveToFirst()) {
        _result = new Evento();
        _result.id = _cursor.getLong(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfTitulo)) {
          _result.titulo = null;
        } else {
          _result.titulo = _cursor.getString(_cursorIndexOfTitulo);
        }
        if (_cursor.isNull(_cursorIndexOfDescricao)) {
          _result.descricao = null;
        } else {
          _result.descricao = _cursor.getString(_cursorIndexOfDescricao);
        }
        if (_cursor.isNull(_cursorIndexOfDataEvento)) {
          _result.dataEvento = null;
        } else {
          _result.dataEvento = _cursor.getString(_cursorIndexOfDataEvento);
        }
        if (_cursor.isNull(_cursorIndexOfLocal)) {
          _result.local = null;
        } else {
          _result.local = _cursor.getString(_cursorIndexOfLocal);
        }
        if (_cursor.isNull(_cursorIndexOfCapacidadeMaxima)) {
          _result.capacidadeMaxima = null;
        } else {
          _result.capacidadeMaxima = _cursor.getInt(_cursorIndexOfCapacidadeMaxima);
        }
        if (_cursor.isNull(_cursorIndexOfStatus)) {
          _result.status = null;
        } else {
          _result.status = _cursor.getString(_cursorIndexOfStatus);
        }
        if (_cursor.isNull(_cursorIndexOfCriadoPor)) {
          _result.criadoPor = null;
        } else {
          _result.criadoPor = _cursor.getLong(_cursorIndexOfCriadoPor);
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
