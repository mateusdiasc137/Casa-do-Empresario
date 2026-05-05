package com.casaempresario.app.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "usuarios")
public class Usuario {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "email")
    public String email; // NOT NULL UNIQUE no SQL

    public String senha;
    public String nome;
    public String role; // Equivale ao ENUM

    @ColumnInfo(name = "criado_em")
    public String criadoEm;
}