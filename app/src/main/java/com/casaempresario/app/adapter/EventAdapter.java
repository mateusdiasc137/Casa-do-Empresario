package com.casaempresario.app.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import androidx.recyclerview.widget.DiffUtil;

import com.bumptech.glide.Glide;
import com.casaempresario.app.R;
import com.casaempresario.app.database.Evento;
import com.casaempresario.app.database.Usuario;
import com.casaempresario.app.repository.RepositoryCallback;
import com.casaempresario.app.repository.RepositoryProvider;
import com.casaempresario.app.util.EventStatusUtils;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final Set<Long> animatedIds = new HashSet<>();

    public EventAdapter(List<Evento> eventos,
                        OnEventClickListener listener) {

        this.eventos = eventos;
        this.listener = listener;
    }

    public void atualizar(List<Evento> novos) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return eventos.size();
            }

            @Override
            public int getNewListSize() {
                return novos.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return eventos.get(oldItemPosition).id == novos.get(newItemPosition).id;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Evento oldItem = eventos.get(oldItemPosition);
                Evento newItem = novos.get(newItemPosition);
                return java.util.Objects.equals(oldItem.titulo, newItem.titulo) &&
                       java.util.Objects.equals(oldItem.status, newItem.status) &&
                       java.util.Objects.equals(oldItem.bannerUri, newItem.bannerUri);
            }
        });

        this.eventos = novos;
        result.dispatchUpdatesTo(this);
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
        Evento evento = eventos.get(position);
        boolean shouldAnimate = !animatedIds.contains(evento.id);
        if (shouldAnimate) {
            animatedIds.add(evento.id);
        }
        holder.bind(evento, listener, shouldAnimate);
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

        // Campo usado na interface
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

            // Campo usado na interface
            tvCriadoPor =
                    itemView.findViewById(R.id.tv_criado_por);
        }

        public void bind(
                Evento evento,
                OnEventClickListener listener,
                boolean shouldAnimate
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

            // Categoria
            if (evento.categoria != null && !evento.categoria.isEmpty()) {
                tvCategoria.setText("🏷️ " + evento.categoria);
                tvCategoria.setVisibility(View.VISIBLE);
            } else {
                tvCategoria.setVisibility(View.GONE);
            }

            // FOTOS
            tvFotos.setText("📷 fotos");

            // Organizador
            if (evento.nomeOrganizador != null) {
                tvCriadoPor.setText("Criado por " + evento.nomeOrganizador);
                tvCriadoPor.setVisibility(View.VISIBLE);
            } else if (evento.criadoPor != null) {
                tvCriadoPor.setText("");
                tvCriadoPor.setVisibility(View.INVISIBLE); // Deixa em branco para atualizar depois sem piscar errado

                RepositoryProvider.getUserRepository(itemView.getContext()).getUsuarioById(evento.criadoPor, new RepositoryCallback<Usuario>() {
                    @Override
                    public void onSuccess(Usuario usuario) {
                        itemView.post(() -> {
                            if (usuario != null && usuario.nome != null) {
                                evento.nomeOrganizador = usuario.nome;
                            } else {
                                evento.nomeOrganizador = "Criador desconhecido";
                            }
                            tvCriadoPor.setText("Criado por " + evento.nomeOrganizador);
                            tvCriadoPor.setVisibility(View.VISIBLE);
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        itemView.post(() -> {
                            evento.nomeOrganizador = "Criador desconhecido";
                            tvCriadoPor.setText("Criado por " + evento.nomeOrganizador);
                            tvCriadoPor.setVisibility(View.VISIBLE);
                        });
                    }
                });
            } else {
                tvCriadoPor.setText("Criador desconhecido");
                tvCriadoPor.setVisibility(View.VISIBLE);
            }

            String data = evento.dataEvento;

            if (data != null && data.contains("T")) {
                tvData.setText("📅 " + formatarPeriodo(evento));
            } else {
                tvData.setText("📅 " + (data != null ? data : ""));
            }

            // STATUS
            String status = EventStatusUtils.calcularStatusAutomatico(evento);
            evento.status = status;

            tvStatus.setText(status);

            int textColor;
            int strokeColor;
            int backgroundColor;

            switch (status != null ? status : "") {

                case "AGENDADO":
                    textColor = 0xFFB9DCFF;
                    strokeColor = 0xFF5AA8FF;
                    backgroundColor = 0xFF10253D;
                    break;

                case "EM_ANDAMENTO":
                    textColor = 0xFFB9F7D5;
                    strokeColor = 0xFF22C58B;
                    backgroundColor = 0xFF103127;
                    break;

                case "CONCLUIDO":
                    textColor = 0xFFE4E8EF;
                    strokeColor = 0xFF8A94A6;
                    backgroundColor = 0xFF272C36;
                    break;

                case "CANCELADO":
                    textColor = 0xFFFFB8B8;
                    strokeColor = 0xFFEF5B5B;
                    backgroundColor = 0xFF3A171A;
                    break;

                default:
                    textColor = 0xFFE7D6FF;
                    strokeColor = 0xFF9C6ADE;
                    backgroundColor = 0xFF261A35;
                    break;
            }

            tvStatus.setText(status != null ? status.replace("_", " ") : "STATUS");
            tvStatus.setTextColor(textColor);
            GradientDrawable statusBackground = new GradientDrawable();
            statusBackground.setShape(GradientDrawable.RECTANGLE);
            statusBackground.setColor(backgroundColor);
            statusBackground.setStroke(2, strokeColor);
            statusBackground.setCornerRadius(999f);
            tvStatus.setBackground(statusBackground);

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

            // Animação de entrada do card
            if (shouldAnimate) {
                itemView.setAlpha(0f);
                itemView.setTranslationY(24f);
                itemView.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(260)
                        .start();
            } else {
                itemView.setAlpha(1f);
                itemView.setTranslationY(0f);
            }

            // CLICK
            itemView.setOnClickListener(v -> listener.onClick(evento));
        }

        private String formatarPeriodo(Evento evento) {
            String inicio = formatarData(evento.dataEvento);
            String fim = evento.dataFimEvento;

            if (fim == null || !fim.contains("T")) {
                return inicio;
            }

            try {
                String[] inicioParts = evento.dataEvento.split("T");
                String[] fimParts = fim.split("T");
                String horaFim = fimParts[1].substring(0, 5);

                if (inicioParts[0].equals(fimParts[0])) {
                    return inicio + " até " + horaFim;
                }

                return inicio + " até " + formatarData(fim);
            } catch (Exception e) {
                return inicio;
            }
        }

        private String formatarData(String data) {
            if (data == null || !data.contains("T")) return data != null ? data : "";
            try {
                String[] parts = data.split("T");
                String[] dateParts = parts[0].split("-");
                String hora = parts[1].length() >= 5 ? parts[1].substring(0, 5) : "";
                return dateParts[2] + "/" + dateParts[1] + "/" + dateParts[0] + " às " + hora;
            } catch (Exception e) {
                return data;
            }
        }
    }
}