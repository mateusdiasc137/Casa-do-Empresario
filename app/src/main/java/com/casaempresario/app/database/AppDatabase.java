package com.casaempresario.app.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.casaempresario.app.model.EventPhoto;

/**
 * Banco de dados local Room (SQLite).
 * Versão 4: adicionado UsuarioDao para autenticação local.
 *
 * Na primeira instalação, um usuário administrador padrão é criado automaticamente:
 *   E-mail : admin@admin.com
 *   Senha  : admin123
 */
@Database(entities = {Usuario.class, Evento.class, EventPhoto.class}, version = 4)
public abstract class AppDatabase extends RoomDatabase {

    public abstract EventoDao eventoDao();
    public abstract FotoDao fotoDao();
    public abstract UsuarioDao usuarioDao();

    private static AppDatabase INSTANCE;

    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "casa_empresario_db")
                            .allowMainThreadQueries()          // simplifica operações pontuais na UI
                            .fallbackToDestructiveMigration()  // recria o banco se a versão mudar
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    // Cria o admin padrão na primeira vez que o banco é criado
                                    db.execSQL(
                                        "INSERT INTO usuarios (email, senha, nome, role, criado_em) " +
                                        "VALUES ('admin@admin.com', 'admin123', 'Administrador', 'ADMIN', datetime('now'))"
                                    );
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
