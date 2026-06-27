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
import com.casaempresario.app.database.Mensagem;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Mensagem> mensagens;
    private final long currentUserId;
    private String currentUserPhotoUri;
    private String otherUserPhotoUri;
    private String currentUserInitials = "EU";
    private String otherUserInitials = "CE";

    public ChatAdapter(List<Mensagem> mensagens, long currentUserId) {
        this.mensagens = mensagens;
        this.currentUserId = currentUserId;
    }

    public ChatAdapter(List<Mensagem> mensagens, long currentUserId, String currentUserPhotoUri, String otherUserPhotoUri) {
        this.mensagens = mensagens;
        this.currentUserId = currentUserId;
        this.currentUserPhotoUri = currentUserPhotoUri;
        this.otherUserPhotoUri = otherUserPhotoUri;
    }

    public void setUserPhotos(String currentUserPhotoUri, String otherUserPhotoUri) {
        this.currentUserPhotoUri = currentUserPhotoUri;
        this.otherUserPhotoUri = otherUserPhotoUri;
        notifyDataSetChanged();
    }

    public void setInitials(String currentUserInitials, String otherUserInitials) {
        this.currentUserInitials = currentUserInitials != null ? currentUserInitials : "EU";
        this.otherUserInitials = otherUserInitials != null ? otherUserInitials : "CE";
        notifyDataSetChanged();
    }

    public void atualizar(List<Mensagem> novasMensagens) {
        this.mensagens = novasMensagens;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Mensagem msg = mensagens.get(position);
        if (msg.remetenteId == currentUserId) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Mensagem msg = mensagens.get(position);
        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).bind(msg);
        } else if (holder instanceof ReceivedViewHolder) {
            ((ReceivedViewHolder) holder).bind(msg);
        }
    }

    @Override
    public int getItemCount() {
        return mensagens.size();
    }

    private void bindAvatar(View itemView, ImageView imgAvatar, TextView tvInitials, String fotoUri, String fallbackInitials) {
        if (fotoUri != null && !fotoUri.trim().isEmpty()) {
            imgAvatar.setVisibility(View.VISIBLE);
            tvInitials.setVisibility(View.GONE);
            Glide.with(itemView).load(fotoUri).circleCrop().into(imgAvatar);
        } else {
            imgAvatar.setVisibility(View.GONE);
            tvInitials.setVisibility(View.VISIBLE);
            tvInitials.setText(fallbackInitials);
        }
    }

    class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvTexto, tvTimestamp, tvInitials;
        ImageView imgAvatar;

        SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTexto = itemView.findViewById(R.id.tv_texto);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            imgAvatar = itemView.findViewById(R.id.img_avatar);
            tvInitials = itemView.findViewById(R.id.tv_avatar_iniciais);
        }

        void bind(Mensagem msg) {
            tvTexto.setText(msg.texto);
            tvTimestamp.setText(msg.timestamp);
            String foto = msg.remetenteFotoUri != null && !msg.remetenteFotoUri.trim().isEmpty()
                    ? msg.remetenteFotoUri
                    : currentUserPhotoUri;
            bindAvatar(itemView, imgAvatar, tvInitials, foto, currentUserInitials);
        }
    }

    class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvTexto, tvTimestamp, tvInitials;
        ImageView imgAvatar;

        ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTexto = itemView.findViewById(R.id.tv_texto);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            imgAvatar = itemView.findViewById(R.id.img_avatar);
            tvInitials = itemView.findViewById(R.id.tv_avatar_iniciais);
        }

        void bind(Mensagem msg) {
            tvTexto.setText(msg.texto);
            tvTimestamp.setText(msg.timestamp);
            String foto = msg.remetenteFotoUri != null && !msg.remetenteFotoUri.trim().isEmpty()
                    ? msg.remetenteFotoUri
                    : otherUserPhotoUri;
            bindAvatar(itemView, imgAvatar, tvInitials, foto, otherUserInitials);
        }
    }
}
