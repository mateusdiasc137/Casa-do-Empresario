package com.casaempresario.app.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
    tableName = "interesses",
    foreignKeys = {
        @ForeignKey(
            entity = Usuario.class,
            parentColumns = "id",
            childColumns = "usuario_id",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = Evento.class,
            parentColumns = "id",
            childColumns = "evento_id",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index("usuario_id"),
        @Index("evento_id")
    }
)
public class Interesse {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "usuario_id")
    public long usuarioId;

    @ColumnInfo(name = "evento_id")
    public long eventoId;

    @ColumnInfo(name = "criado_em")
    public String criadoEm;
}
