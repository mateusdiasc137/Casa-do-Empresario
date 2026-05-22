package com.casaempresario.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.casaempresario.app.R;
import com.casaempresario.app.database.Evento;

import java.io.File;
import java.util.List;

/**
 * Adapter da lista de eventos na MainActivity.
 * Usa a entidade Evento (Room) diretamente.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    public interface OnEventClickListener {
        void onClick(Evento evento);
    }

    private List<Evento> eventos;
    private final OnEventClickListener listener;

    public EventAdapter(List<Evento> eventos,
                        OnEventClickListener listener) {

        this.eventos = eventos;
        this.listener = listener;
    }

    public void atualizar(List<Evento> novos) {
        this.eventos = novos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);

        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull EventViewHolder holder,
            int position
    ) {

        holder.bind(eventos.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return eventos.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imgCapa;

        private final TextView tvTitulo;
        private final TextView tvData;
        private final TextView tvLocal;
        private final TextView tvStatus;
        private final TextView tvFotos;

        // NOVO
        private final TextView tvCriadoPor;
        private final TextView tvCategoria;

        public EventViewHolder(@NonNull View itemView) {

            super(itemView);

            imgCapa  = itemView.findViewById(R.id.img_capa);

            tvTitulo = itemView.findViewById(R.id.tv_titulo);
            tvData   = itemView.findViewById(R.id.tv_data);
            tvLocal  = itemView.findViewById(R.id.tv_local);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvFotos  = itemView.findViewById(R.id.tv_fotos);
            tvCategoria = itemView.findViewById(R.id.tv_categoria);

            // NOVO
            tvCriadoPor =
                    itemView.findViewById(R.id.tv_criado_por);
        }

        public void bind(
                Evento evento,
                OnEventClickListener listener
        ) {

            // TÍTULO
            tvTitulo.setText(evento.titulo);

            // LOCAL
            tvLocal.setText(
                    "📍 " +
                            (evento.local != null
                                    ? evento.local
                                    : "")
            );

            // CATEGORIA
            if (evento.categoria != null && !evento.categoria.isEmpty()) {
                tvCategoria.setText("🏷️ " + evento.categoria);
                tvCategoria.setVisibility(View.VISIBLE);
            } else {
                tvCategoria.setVisibility(View.GONE);
            }

            // FOTOS
            tvFotos.setText("📷 fotos");

            // CRIADOR
            if (evento.criadoPor != null) {

                tvCriadoPor.setText(
                        "Criado por usuário #" +
                                evento.criadoPor
                );

            } else {

                tvCriadoPor.setText(
                        "Criador desconhecido"
                );
            }

            // DATA
            String data = evento.dataEvento;

            if (data != null && data.contains("T")) {

                String[] parts =
                        data.split("T");

                String[] dateParts =
                        parts[0].split("-");

                String hora =
                        parts[1].length() >= 5
                                ? parts[1].substring(0, 5)
                                : "";

                tvData.setText(
                        "📅 "
                                + dateParts[2]
                                + "/"
                                + dateParts[1]
                                + "/"
                                + dateParts[0]
                                + " às "
                                + hora
                );

            } else {

                tvData.setText(
                        "📅 " +
                                (data != null ? data : "")
                );
            }

            // STATUS
            String status = evento.status;

            tvStatus.setText(status);

            int color;

            switch (status != null ? status : "") {

                case "AGENDADO":
                    color = 0xFF1976D2;
                    break;

                case "EM_ANDAMENTO":
                    color = 0xFF388E3C;
                    break;

                case "CONCLUIDO":
                    color = 0xFF757575;
                    break;

                case "CANCELADO":
                    color = 0xFFD32F2F;
                    break;

                default:
                    color = 0xFF9C27B0;
                    break;
            }

            tvStatus.setTextColor(color);

            // BANNER REAL
            if (evento.bannerUri != null
                    && !evento.bannerUri.isEmpty()) {

                Glide.with(itemView.getContext())
                        .load(evento.bannerUri)
                        .placeholder(
                                R.drawable.ic_event_placeholder
                        )
                        .into(imgCapa);

            } else {

                imgCapa.setImageResource(
                        R.drawable.ic_event_placeholder
                );
            }

            // CLICK
            itemView.setOnClickListener(
                    v -> listener.onClick(evento)
            );
        }
    }
}