package com.casaempresario.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.casaempresario.app.adapter.OrganizerEventSummaryAdapter;
import com.casaempresario.app.database.Evento;
import com.casaempresario.app.databinding.ActivityOrganizerDashboardBinding;
import com.casaempresario.app.repository.RepositoryCallback;
import com.casaempresario.app.repository.RepositoryProvider;
import com.casaempresario.app.util.EventStatusUtils;
import com.casaempresario.app.util.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Painel executivo do organizador.
 * Consolida indicadores de eventos publicados e disponibiliza atalhos para
 * criação de novos eventos e acompanhamento operacional.
 */
public class OrganizerDashboardActivity extends AppCompatActivity {

    private ActivityOrganizerDashboardBinding binding;
    private SessionManager sessionManager;
    private OrganizerEventSummaryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrganizerDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        if (!sessionManager.isOrganizador()) {
            Toast.makeText(this, "Painel disponível apenas para organizadores.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        configurarToolbar();
        configurarLista();
        configurarAcoes();
        carregarDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null && sessionManager != null && sessionManager.isOrganizador()) {
            carregarDashboard();
        }
    }

    private void configurarToolbar() {
        binding.toolbarDashboard.setNavigationOnClickListener(v -> finish());
        binding.tvDashboardGreeting.setText("Olá, " + sessionManager.getNome());
    }

    private void configurarLista() {
        adapter = new OrganizerEventSummaryAdapter();
        binding.recyclerOrganizerEvents.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerOrganizerEvents.setAdapter(adapter);
    }

    private void configurarAcoes() {
        binding.btnCriarEventoDashboard.setOnClickListener(v -> startActivity(new Intent(this, CreateEventActivity.class)));
        binding.btnAtualizarDashboard.setOnClickListener(v -> carregarDashboard());
    }

    private void carregarDashboard() {
        binding.progressDashboard.setVisibility(View.VISIBLE);
        binding.layoutDashboardContent.setVisibility(View.GONE);
        binding.layoutDashboardEmpty.getRoot().setVisibility(View.GONE);

        RepositoryProvider.getEventRepository(this).getAllEventos(new RepositoryCallback<List<Evento>>() {
            @Override
            public void onSuccess(List<Evento> eventos) {
                List<Evento> meusEventos = filtrarEventosDoOrganizador(eventos);
                runOnUiThread(() -> renderizarDashboard(meusEventos));
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    binding.progressDashboard.setVisibility(View.GONE);
                    binding.layoutDashboardContent.setVisibility(View.VISIBLE);
                    Toast.makeText(OrganizerDashboardActivity.this, "Erro ao carregar indicadores", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private List<Evento> filtrarEventosDoOrganizador(List<Evento> eventos) {
        List<Evento> filtrados = new ArrayList<>();
        if (eventos == null) {
            return filtrados;
        }
        long userId = sessionManager.getUserId();
        for (Evento evento : eventos) {
            if (evento != null && evento.criadoPor != null && evento.criadoPor == userId) {
                evento.status = EventStatusUtils.calcularStatusAutomatico(evento);
                filtrados.add(evento);
            }
        }
        Collections.sort(filtrados, (a, b) -> Long.compare(EventStatusUtils.getEventTimeMillis(a), EventStatusUtils.getEventTimeMillis(b)));
        return filtrados;
    }

    private void renderizarDashboard(List<Evento> eventos) {
        int total = eventos.size();
        int ativos = 0;
        int concluidos = 0;
        int cancelados = 0;
        int futuros = 0;

        for (Evento evento : eventos) {
            String status = EventStatusUtils.calcularStatusAutomatico(evento);
            if ("AGENDADO".equalsIgnoreCase(status) || "EM_ANDAMENTO".equalsIgnoreCase(status)) {
                ativos++;
            }
            if ("CONCLUIDO".equalsIgnoreCase(status)) {
                concluidos++;
            }
            if ("CANCELADO".equalsIgnoreCase(status)) {
                cancelados++;
            }
            if (EventStatusUtils.getEventTimeMillis(evento) >= System.currentTimeMillis()) {
                futuros++;
            }
        }

        binding.progressDashboard.setVisibility(View.GONE);
        binding.layoutDashboardContent.setVisibility(View.VISIBLE);
        binding.tvMetricTotal.setText(String.valueOf(total));
        binding.tvMetricAtivos.setText(String.valueOf(ativos));
        binding.tvMetricConcluidos.setText(String.valueOf(concluidos));
        binding.tvMetricCancelados.setText(String.valueOf(cancelados));
        binding.tvMetricFuturos.setText(String.valueOf(futuros));
        binding.tvDashboardSummary.setText(montarResumoExecutivo(total, ativos, futuros));

        adapter.atualizar(eventos);
        boolean vazio = eventos.isEmpty();
        binding.recyclerOrganizerEvents.setVisibility(vazio ? View.GONE : View.VISIBLE);
        binding.layoutDashboardEmpty.getRoot().setVisibility(vazio ? View.VISIBLE : View.GONE);
        binding.layoutDashboardEmpty.tvEmptyTitle.setText("Nenhum evento publicado");
        binding.layoutDashboardEmpty.tvEmptyDescription.setText("Crie seu primeiro evento para acompanhar indicadores e movimentações do público.");
        binding.layoutDashboardEmpty.btnEmptyAction.setText("Criar evento");
        binding.layoutDashboardEmpty.btnEmptyAction.setOnClickListener(v -> startActivity(new Intent(this, CreateEventActivity.class)));
    }

    private String montarResumoExecutivo(int total, int ativos, int futuros) {
        if (total == 0) {
            return "Seu painel ainda não possui dados. Publique eventos para acompanhar a operação.";
        }
        if (ativos > 0) {
            return "Você possui " + ativos + " evento(s) em operação e " + futuros + " compromisso(s) futuro(s) na agenda.";
        }
        return "Você possui " + total + " evento(s) cadastrado(s). Programe novas agendas para manter o engajamento.";
    }
}
