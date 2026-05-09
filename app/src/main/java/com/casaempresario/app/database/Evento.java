package com.casaempresario.app.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "eventos")
public class Evento {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String titulo;
    public String descricao;

    @ColumnInfo(name = "data_evento")
    public String dataEvento;

    public String local;

    @ColumnInfo(name = "capacidade_maxima")
    public Integer capacidadeMaxima;

    public String status;

    @ColumnInfo(name = "criado_por")
    public Long criadoPor;

    // NOVO
    @ColumnInfo(name = "banner_uri")
    public String bannerUri;

    // Getters
    public long getId()                  { return id; }
    public String getTitulo()            { return titulo; }
    public String getDescricao()         { return descricao; }
    public String getDataEvento()        { return dataEvento; }
    public String getLocal()             { return local; }
    public Integer getCapacidadeMaxima() { return capacidadeMaxima; }
    public String getStatus()            { return status; }
    public Long getCriadoPor()           { return criadoPor; }

    // NOVO
    public String getBannerUri()         { return bannerUri; }
}