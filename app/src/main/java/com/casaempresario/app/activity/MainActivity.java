package com.casaempresario.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.casaempresario.app.R;
import com.casaempresario.app.adapter.EventAdapter;
import com.casaempresario.app.database.AppDatabase;
import com.casaempresario.app.database.Evento;
import com.casaempresario.app.databinding.ActivityMainBinding;
import com.casaempresario.app.util.SessionManager;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Tela principal — exibe a lista de eventos do banco local.
 * Admin vê o botão para criar novos eventos.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SessionManager sessionManager;
    private EventAdapter adapter;
    private AppDatabase db;
    private boolean mostrandoProximos = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getDatabase(this);
        sessionManager = new SessionManager(this);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Casa do Empresário");
        }

        setupRecyclerView();
        setupTabs();
        setupSwipeRefresh();

        // Somente admin vê o botão de criar evento
        if (sessionManager.isAdmin()) {
            binding.fabNovoEvento.setVisibility(View.VISIBLE);
            binding.fabNovoEvento.setOnClickListener(v ->
                    startActivity(new Intent(this, CreateEventActivity.class)));
        }

        carregarEventos(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Atualiza a lista ao voltar de outra tela (ex.: depois de criar/editar evento)
        carregarEventos(mostrandoProximos);
    }

    private void setupRecyclerView() {
        adapter = new EventAdapter(new ArrayList<>(), evento -> {
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra("eventoId", evento.id);
            intent.putExtra("eventoTitulo", evento.titulo);
            startActivity(intent);
        });
        binding.recyclerEventos.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerEventos.setAdapter(adapter);
    }

    private void setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Próximos"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Todos"));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mostrandoProximos = (tab.getPosition() == 0);
                carregarEventos(mostrandoProximos);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> carregarEventos(mostrandoProximos));
        binding.swipeRefresh.setColorSchemeResources(R.color.purple_500);
    }

    private void carregarEventos(boolean proximos) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvVazio.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                List<Evento> eventos = db.eventoDao().getAllEventos();

                // Filtra apenas próximos (status AGENDADO ou EM_ANDAMENTO) se a aba for "Próximos"
                List<Evento> filtrados = new ArrayList<>();
                for (Evento e : eventos) {
                    if (!proximos || "AGENDADO".equals(e.status) || "EM_ANDAMENTO".equals(e.status)) {
                        filtrados.add(e);
                    }
                }

                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    adapter.atualizar(filtrados);
                    binding.tvVazio.setVisibility(filtrados.isEmpty() ? View.VISIBLE : View.GONE);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Erro ao carregar eventos", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            sessionManager.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_perfil) {
            String info = sessionManager.getNome() + " (" + sessionManager.getRole() + ")";
            Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
