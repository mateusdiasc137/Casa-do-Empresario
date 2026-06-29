package com.casaempresario.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.casaempresario.app.R;
import com.casaempresario.app.model.AppNotification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationCenterAdapter extends RecyclerView.Adapter<NotificationCenterAdapter.ViewHolder> {

    private final List<AppNotification> items = new ArrayList<>();
    private final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM HH:mm", new Locale("pt", "BR"));

    public void atualizar(List<AppNotification> novos) {
        items.clear();
        if (novos != null) {
            items.addAll(novos);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_center, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppNotification item = items.get(position);
        holder.titulo.setText(item.titulo != null ? item.titulo : "Atualização");
        holder.descricao.setText(item.descricao != null ? item.descricao : "Nova movimentação registrada no aplicativo.");
        holder.tipo.setText(item.tipo != null ? item.tipo : "SISTEMA");
        holder.data.setText(item.timestamp > 0 ? formatter.format(new Date(item.timestamp)) : "Agora");
        holder.indicador.setVisibility(item.destaque ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View indicador;
        TextView titulo;
        TextView descricao;
        TextView tipo;
        TextView data;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            indicador = itemView.findViewById(R.id.view_notification_indicator);
            titulo = itemView.findViewById(R.id.tv_notification_title);
            descricao = itemView.findViewById(R.id.tv_notification_description);
            tipo = itemView.findViewById(R.id.tv_notification_type);
            data = itemView.findViewById(R.id.tv_notification_date);
        }
    }
}
