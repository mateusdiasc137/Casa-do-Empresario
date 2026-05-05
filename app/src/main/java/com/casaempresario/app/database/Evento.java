package com.casaempresario.app.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidade Room que representa um evento no banco local (SQLite).
 * Os campos são públicos para que o Room consiga acessá-los diretamente.
 */
@Entity(tableName = "eventos")
public class Evento {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String titulo;
    public String descricao;

    @ColumnInfo(name = "data_evento")
    public String dataEvento; // formato: "YYYY-MM-DDTHH:MM:00"

    public String local;

    @ColumnInfo(name = "capacidade_maxima")
    public Integer capacidadeMaxima;

    public String status; // AGENDADO | EM_ANDAMENTO | CONCLUIDO | CANCELADO

    @ColumnInfo(name = "criado_por")
    public Long criadoPor; // ID do usuário que criou o evento

    // ── Getters para uso nos Adapters e Activities ─────────────────────────────
    public long getId()                  { return id; }
    public String getTitulo()            { return titulo; }
    public String getDescricao()         { return descricao; }
    public String getDataEvento()        { return dataEvento; }
    public String getLocal()             { return local; }           // corrigido: antes retornava ""
    public Integer getCapacidadeMaxima() { return capacidadeMaxima; } // corrigido: antes retornava 0
    public String getStatus()            { return status; }
}
