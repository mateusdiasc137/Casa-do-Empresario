package com.casaempresario.app.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;
import com.casaempresario.app.database.Evento;

@Entity(tableName = "fotos_evento",
        foreignKeys = @ForeignKey(entity = Evento.class,
                parentColumns = "id",
                childColumns = "eventoId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("eventoId")})
public class EventPhoto {

    @PrimaryKey(autoGenerate = true)
    private Long id;
    private long eventoId; // ID do evento dono da foto
    private String urlFoto; // Aqui salvaremos o caminho do arquivo no celular
    private String legenda;
    private String enviadoEm;

    private String usuarioNome;
    public String getUsuarioNome() { return usuarioNome; }
    public void setUsuarioNome(String usuarioNome) { this.usuarioNome = usuarioNome; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getEventoId() { return eventoId; }
    public void setEventoId(long eventoId) { this.eventoId = eventoId; }
    public String getUrlFoto() { return urlFoto; }
    public void setUrlFoto(String urlFoto) { this.urlFoto = urlFoto; }
    public String getLegenda() { return legenda; }
    public void setLegenda(String legenda) { this.legenda = legenda; }
    public String getEnviadoEm() { return enviadoEm; }
    public void setEnviadoEm(String enviadoEm) { this.enviadoEm = enviadoEm; }
}