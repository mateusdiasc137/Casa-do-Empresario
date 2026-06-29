package com.casaempresario.app.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.casaempresario.app.adapter.NotificationCenterAdapter;
import com.casaempresario.app.databinding.ActivityNotificationCenterBinding;
import com.casaempresario.app.model.AppNotification;
import com.casaempresario.app.util.SessionManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central interna de notificações.
 * Consolida eventos recentes, publicações oficiais e informações de perfil em uma
 * trilha única para o usuário.
 */
public class NotificationCenterActivity extends AppCompatActivity {

    private ActivityNotificationCenterBinding binding;
    private NotificationCenterAdapter adapter;
    private SessionManager sessionManager;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationCenterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        firestore = FirebaseFirestore.getInstance();
        configurarTela();
        carregarNotificacoes();
    }

    private void configurarTela() {
        binding.toolbarNotifications.setNavigationOnClickListener(v -> finish());
        adapter = new NotificationCenterAdapter();
        binding.recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerNotifications.setAdapter(adapter);
        binding.swipeNotifications.setOnRefreshListener(this::carregarNotificacoes);
        binding.btnNotificationsRefresh.setOnClickListener(v -> carregarNotificacoes());
    }

    private void carregarNotificacoes() {
        binding.progressNotifications.setVisibility(View.VISIBLE);
        binding.layoutNotificationsEmpty.getRoot().setVisibility(View.GONE);
        List<AppNotification> consolidadas = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger pendencias = new AtomicInteger(2);

        carregarEventosRecentes(consolidadas, pendencias);
        carregarPostsOficiais(consolidadas, pendencias);
    }

    private void carregarEventosRecentes(List<AppNotification> consolidadas, AtomicInteger pendencias) {
        firestore.collection("eventos")
                .limit(8)
                .get()
                .addOnSuccessListener(snapshots -> {
                    snapshots.forEach(doc -> {
                        String titulo = doc.getString("titulo");
                        String categoria = doc.getString("categoria");
                        String local = doc.getString("local");
                        Long createdAt = doc.getLong("createdAt");
                        consolidadas.add(new AppNotification(
                                doc.getId(),
                                titulo != null ? "Novo evento: " + titulo : "Novo evento publicado",
                                montarDescricaoEvento(categoria, local),
                                "EVENTO",
                                createdAt != null ? createdAt : System.currentTimeMillis(),
                                true
                        ));
                    });
                    finalizarSePossivel(consolidadas, pendencias);
                })
                .addOnFailureListener(e -> finalizarSePossivel(consolidadas, pendencias));
    }

    private void carregarPostsOficiais(List<AppNotification> consolidadas, AtomicInteger pendencias) {
        firestore.collection("feed_posts")
                .whereEqualTo("tipo", "OFICIAL")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(8)
                .get()
                .addOnSuccessListener(snapshots -> {
                    snapshots.forEach(doc -> {
                        String autor = doc.getString("autorNome");
                        String texto = doc.getString("texto");
                        Long createdAt = doc.getLong("createdAt");
                        consolidadas.add(new AppNotification(
                                doc.getId(),
                                "Comunicado oficial" + (autor != null ? " de " + autor : ""),
                                texto != null && !texto.trim().isEmpty() ? texto : "Nova publicação no mural da comunidade.",
                                "COMUNICADO",
                                createdAt != null ? createdAt : System.currentTimeMillis(),
                                false
                        ));
                    });
                    finalizarSePossivel(consolidadas, pendencias);
                })
                .addOnFailureListener(e -> finalizarSePossivel(consolidadas, pendencias));
    }

    private String montarDescricaoEvento(String categoria, String local) {
        String categoriaFormatada = categoria != null && !categoria.trim().isEmpty() ? categoria : "Categoria geral";
        String localFormatado = local != null && !local.trim().isEmpty() ? local : "local a definir";
        return categoriaFormatada + " • " + localFormatado;
    }

    private void finalizarSePossivel(List<AppNotification> consolidadas, AtomicInteger pendencias) {
        if (pendencias.decrementAndGet() != 0) {
            return;
        }

        runOnUiThread(() -> {
            binding.progressNotifications.setVisibility(View.GONE);
            binding.swipeNotifications.setRefreshing(false);

            if (sessionManager.isLogado()) {
                consolidadas.add(new AppNotification(
                        "profile_status",
                        "Perfil corporativo",
                        "Mantenha cargo, empresa e cidade atualizados para melhorar networking em eventos.",
                        "PERFIL",
                        System.currentTimeMillis() - 60000,
                        false
                ));
            }

            Collections.sort(consolidadas, (a, b) -> Long.compare(b.timestamp, a.timestamp));
            adapter.atualizar(consolidadas);
            boolean vazio = consolidadas.isEmpty();
            binding.recyclerNotifications.setVisibility(vazio ? View.GONE : View.VISIBLE);
            binding.layoutNotificationsEmpty.getRoot().setVisibility(vazio ? View.VISIBLE : View.GONE);
            binding.layoutNotificationsEmpty.tvEmptyTitle.setText("Nenhuma notificação no momento");
            binding.layoutNotificationsEmpty.tvEmptyDescription.setText("Quando houver novos eventos, comunicados ou lembretes, eles aparecerão aqui.");
            binding.layoutNotificationsEmpty.btnEmptyAction.setText("Atualizar central");
            binding.layoutNotificationsEmpty.btnEmptyAction.setOnClickListener(v -> carregarNotificacoes());
        });
    }
}
