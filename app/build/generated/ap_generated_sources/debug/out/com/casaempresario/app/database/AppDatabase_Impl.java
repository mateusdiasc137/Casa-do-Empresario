package com.casaempresario.app.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile EventoDao _eventoDao;

  private volatile FotoDao _fotoDao;

  private volatile UsuarioDao _usuarioDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(5) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `usuarios` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `email` TEXT, `senha` TEXT, `nome` TEXT, `role` TEXT, `criado_em` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `eventos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `titulo` TEXT, `descricao` TEXT, `data_evento` TEXT, `local` TEXT, `capacidade_maxima` INTEGER, `status` TEXT, `criado_por` INTEGER, `banner_uri` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `fotos_evento` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `eventoId` INTEGER NOT NULL, `urlFoto` TEXT, `legenda` TEXT, `enviadoEm` TEXT, `usuarioNome` TEXT, FOREIGN KEY(`eventoId`) REFERENCES `eventos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fotos_evento_eventoId` ON `fotos_evento` (`eventoId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'c6bbb30eb4f82b3280e90d8cf6c555ef')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `usuarios`");
        db.execSQL("DROP TABLE IF EXISTS `eventos`");
        db.execSQL("DROP TABLE IF EXISTS `fotos_evento`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsUsuarios = new HashMap<String, TableInfo.Column>(6);
        _columnsUsuarios.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsuarios.put("email", new TableInfo.Column("email", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsuarios.put("senha", new TableInfo.Column("senha", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsuarios.put("nome", new TableInfo.Column("nome", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsuarios.put("role", new TableInfo.Column("role", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsuarios.put("criado_em", new TableInfo.Column("criado_em", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUsuarios = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUsuarios = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUsuarios = new TableInfo("usuarios", _columnsUsuarios, _foreignKeysUsuarios, _indicesUsuarios);
        final TableInfo _existingUsuarios = TableInfo.read(db, "usuarios");
        if (!_infoUsuarios.equals(_existingUsuarios)) {
          return new RoomOpenHelper.ValidationResult(false, "usuarios(com.casaempresario.app.database.Usuario).\n"
                  + " Expected:\n" + _infoUsuarios + "\n"
                  + " Found:\n" + _existingUsuarios);
        }
        final HashMap<String, TableInfo.Column> _columnsEventos = new HashMap<String, TableInfo.Column>(9);
        _columnsEventos.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEventos.put("titulo", new TableInfo.Column("titulo", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEventos.put("descricao", new TableInfo.Column("descricao", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEventos.put("data_evento", new TableInfo.Column("data_evento", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEventos.put("local", new TableInfo.Column("local", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEventos.put("capacidade_maxima", new TableInfo.Column("capacidade_maxima", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEventos.put("status", new TableInfo.Column("status", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEventos.put("criado_por", new TableInfo.Column("criado_por", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEventos.put("banner_uri", new TableInfo.Column("banner_uri", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysEventos = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesEventos = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoEventos = new TableInfo("eventos", _columnsEventos, _foreignKeysEventos, _indicesEventos);
        final TableInfo _existingEventos = TableInfo.read(db, "eventos");
        if (!_infoEventos.equals(_existingEventos)) {
          return new RoomOpenHelper.ValidationResult(false, "eventos(com.casaempresario.app.database.Evento).\n"
                  + " Expected:\n" + _infoEventos + "\n"
                  + " Found:\n" + _existingEventos);
        }
        final HashMap<String, TableInfo.Column> _columnsFotosEvento = new HashMap<String, TableInfo.Column>(6);
        _columnsFotosEvento.put("id", new TableInfo.Column("id", "INTEGER", false, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFotosEvento.put("eventoId", new TableInfo.Column("eventoId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFotosEvento.put("urlFoto", new TableInfo.Column("urlFoto", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFotosEvento.put("legenda", new TableInfo.Column("legenda", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFotosEvento.put("enviadoEm", new TableInfo.Column("enviadoEm", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFotosEvento.put("usuarioNome", new TableInfo.Column("usuarioNome", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFotosEvento = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysFotosEvento.add(new TableInfo.ForeignKey("eventos", "CASCADE", "NO ACTION", Arrays.asList("eventoId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesFotosEvento = new HashSet<TableInfo.Index>(1);
        _indicesFotosEvento.add(new TableInfo.Index("index_fotos_evento_eventoId", false, Arrays.asList("eventoId"), Arrays.asList("ASC")));
        final TableInfo _infoFotosEvento = new TableInfo("fotos_evento", _columnsFotosEvento, _foreignKeysFotosEvento, _indicesFotosEvento);
        final TableInfo _existingFotosEvento = TableInfo.read(db, "fotos_evento");
        if (!_infoFotosEvento.equals(_existingFotosEvento)) {
          return new RoomOpenHelper.ValidationResult(false, "fotos_evento(com.casaempresario.app.model.EventPhoto).\n"
                  + " Expected:\n" + _infoFotosEvento + "\n"
                  + " Found:\n" + _existingFotosEvento);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "c6bbb30eb4f82b3280e90d8cf6c555ef", "97ff1dd871990794a4077d0292953585");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "usuarios","eventos","fotos_evento");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `usuarios`");
      _db.execSQL("DELETE FROM `eventos`");
      _db.execSQL("DELETE FROM `fotos_evento`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(EventoDao.class, EventoDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(FotoDao.class, FotoDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(UsuarioDao.class, UsuarioDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public EventoDao eventoDao() {
    if (_eventoDao != null) {
      return _eventoDao;
    } else {
      synchronized(this) {
        if(_eventoDao == null) {
          _eventoDao = new EventoDao_Impl(this);
        }
        return _eventoDao;
      }
    }
  }

  @Override
  public FotoDao fotoDao() {
    if (_fotoDao != null) {
      return _fotoDao;
    } else {
      synchronized(this) {
        if(_fotoDao == null) {
          _fotoDao = new FotoDao_Impl(this);
        }
        return _fotoDao;
      }
    }
  }

  @Override
  public UsuarioDao usuarioDao() {
    if (_usuarioDao != null) {
      return _usuarioDao;
    } else {
      synchronized(this) {
        if(_usuarioDao == null) {
          _usuarioDao = new UsuarioDao_Impl(this);
        }
        return _usuarioDao;
      }
    }
  }
}
