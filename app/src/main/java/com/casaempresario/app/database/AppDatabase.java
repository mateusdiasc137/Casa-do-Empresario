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
 *
 * Versão 11:
 * - Adicionados campos de perfil corporativo expandido.
 *
 * Versão 10:
 * - Adicionado horário de término dos eventos.
 *
 * Versão 9:
 * - Adicionada foto de perfil de usuário e propagação no feed/chat.
 *
 * Versão 8:
 * - Adicionado campo categoria na entidade Evento.
 *
 * Versão 7:
 * - Papéis disponíveis: PARTICIPANTE e ORGANIZADOR.
 */
@Database(
        entities = {
                Usuario.class,
                Evento.class,
                EventPhoto.class,
                Interesse.class,
                Mensagem.class
        },
        version = 11
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract EventoDao eventoDao();

    public abstract FotoDao fotoDao();

    public abstract UsuarioDao usuarioDao();

    public abstract InteresseDao interesseDao();

    public abstract MensagemDao mensagemDao();

    private static AppDatabase INSTANCE;

    public static AppDatabase getDatabase(Context context) {

        if (INSTANCE == null) {

            synchronized (AppDatabase.class) {

                if (INSTANCE == null) {

                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "casa_empresario_db"
                            )

                            // Permite consultas diretas nas telas do app
                            .allowMainThreadQueries()

                            // Recria o banco quando houver mudança incompatível de versão
                            .fallbackToDestructiveMigration()

                            // Insere contas iniciais para uso do sistema
                            .addCallback(new Callback() {

                                @Override
                                public void onCreate(
                                        @NonNull SupportSQLiteDatabase db
                                ) {

                                    super.onCreate(db);

                                    // Conta inicial de organizador
                                    db.execSQL(
                                            "INSERT INTO usuarios " +
                                                    "(email, senha, nome, role, criado_em) " +
                                                    "VALUES (" +
                                                    "'equipe@casadoempresario.com', " +
                                                    "'equipe123', " +
                                                    "'Equipe Casa do Empresário', " +
                                                    "'ORGANIZADOR', " +
                                                    "datetime('now')" +
                                                    ")"
                                    );

                                    // Conta inicial de administração
                                    db.execSQL(
                                            "INSERT INTO usuarios " +
                                                    "(email, senha, nome, role, criado_em) " +
                                                    "VALUES (" +
                                                    "'admin@admin.com', " +
                                                    "'admin123', " +
                                                    "'Administrador Geral', " +
                                                    "'ORGANIZADOR', " +
                                                    "datetime('now')" +
                                                    ")"
                                    );

                                    // Conta inicial de participante
                                    db.execSQL(
                                            "INSERT INTO usuarios " +
                                                    "(email, senha, nome, role, criado_em) " +
                                                    "VALUES (" +
                                                    "'participante@teste.com', " +
                                                    "'123456', " +
                                                    "'Participante Teste', " +
                                                    "'PARTICIPANTE', " +
                                                    "datetime('now')" +
                                                    ")"
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