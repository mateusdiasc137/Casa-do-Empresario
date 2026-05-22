package com.casaempresario.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.casaempresario.app.R;
import com.casaempresario.app.model.EventPhoto;
import com.casaempresario.app.util.SessionManager;

import java.io.File;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    public interface OnDeleteClickListener {
        void onDelete(Long fotoId);
    }

    public interface OnPhotoClickListener {
        void onPhotoClick(EventPhoto foto);
    }

    private List<EventPhoto> fotos;
    private final SessionManager sessionManager;
    private final OnDeleteClickListener deleteListener;
    private final OnPhotoClickListener clickListener;

    public PhotoAdapter(List<EventPhoto> fotos, SessionManager sessionManager,
                        OnDeleteClickListener deleteListener,
                        OnPhotoClickListener clickListener) {
        this.fotos = fotos;
        this.sessionManager = sessionManager;
        this.deleteListener = deleteListener;
        this.clickListener = clickListener;
    }

    public void atualizar(List<EventPhoto> novas) {
        this.fotos = novas;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        EventPhoto foto = fotos.get(position);
        holder.bind(foto, sessionManager, deleteListener, clickListener);
    }

    @Override
    public int getItemCount() {
        return fotos.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imgFoto;
        private final TextView tvUsuario;
        private final TextView tvLegenda;
        private final ImageButton btnDeletar;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFoto = itemView.findViewById(R.id.img_foto);
            tvUsuario = itemView.findViewById(R.id.tv_usuario);
            tvLegenda = itemView.findViewById(R.id.tv_legenda);
            btnDeletar = itemView.findViewById(R.id.btn_deletar);
        }

        public void bind(EventPhoto foto, SessionManager session,
                         OnDeleteClickListener deleteListener,
                         OnPhotoClickListener clickListener) {

            // Exibir nome do criador/autor da publicação
            String autor = foto.getUsuarioNome() != null && !foto.getUsuarioNome().isEmpty()
                    ? foto.getUsuarioNome()
                    : "Organizador";
            tvUsuario.setText(autor);
            tvUsuario.setVisibility(View.VISIBLE);

            if (foto.getLegenda() != null && !foto.getLegenda().isEmpty()) {
                tvLegenda.setVisibility(View.VISIBLE);
                tvLegenda.setText(foto.getLegenda());
            } else {
                tvLegenda.setVisibility(View.GONE);
            }

            // Exclusão rápida opcional no grid para organizador
            boolean podeDeletar = session.isOrganizador();
            btnDeletar.setVisibility(podeDeletar ? View.VISIBLE : View.GONE);
            btnDeletar.setOnClickListener(v -> deleteListener.onDelete(foto.getId()));

            // Clique no item abre o Post em tamanho cheio
            itemView.setOnClickListener(v -> clickListener.onPhotoClick(foto));

            String caminhoLocal = foto.getUrlFoto();
            Glide.with(itemView.getContext())
                    .load(caminhoLocal)
                    .placeholder(R.drawable.ic_photo_placeholder)
                    .centerCrop()
                    .into(imgFoto);
        }
    }
}