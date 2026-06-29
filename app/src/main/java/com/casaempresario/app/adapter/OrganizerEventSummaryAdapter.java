package com.casaempresario.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.casaempresario.app.R;
import com.casaempresario.app.database.Evento;
import com.casaempresario.app.util.EventStatusUtils;

import java.util.ArrayList;
import java.util.List;

public class OrganizerEventSummaryAdapter extends RecyclerView.Adapter<OrganizerEventSummaryAdapter.ViewHolder> {

    private final List<Evento> eventos = new ArrayList<>();

    public void atualizar(List<Evento> novos) {
        eventos.clear();
        if (novos != null) {
            eventos.addAll(novos);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_organizer_event_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Evento evento = eventos.get(position);
        String status = EventStatusUtils.calcularStatusAutomatico(evento);
        holder.titulo.setText(evento.titulo != null ? evento.titulo : "Evento sem título");
        holder.local.setText(evento.local != null && !evento.local.trim().isEmpty() ? evento.local : "Local a definir");
        holder.data.setText(evento.dataEvento != null ? evento.dataEvento : "Data a definir");
        holder.status.setText(status.replace("_", " "));
        holder.categoria.setText(evento.categoria != null ? evento.categoria : "Geral");
    }

    @Override
    public int getItemCount() {
        return eventos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titulo;
        TextView local;
        TextView data;
        TextView status;
        TextView categoria;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.tv_summary_title);
            local = itemView.findViewById(R.id.tv_summary_location);
            data = itemView.findViewById(R.id.tv_summary_date);
            status = itemView.findViewById(R.id.tv_summary_status);
            categoria = itemView.findViewById(R.id.tv_summary_category);
        }
    }
}
