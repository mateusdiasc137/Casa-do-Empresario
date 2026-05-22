package com.casaempresario.app.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "mensagens",
    foreignKeys = {
        @ForeignKey(
            entity = Evento.class,
            parentColumns = "id",
            childColumns = "evento_id",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = Usuario.class,
            parentColumns = "id",
            childColumns = "remetente_id",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = Usuario.class,
            parentColumns = "id",
            childColumns = "destinatario_id",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index("evento_id"),
        @Index("remetente_id"),
        @Index("destinatario_id")
    }
)
public class Mensagem {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "evento_id")
    public long eventoId;

    @ColumnInfo(name = "remetente_id")
    public long remetenteId;

    @ColumnInfo(name = "destinatario_id")
    public long destinatarioId;

    public String texto;
    public String timestamp;
}
