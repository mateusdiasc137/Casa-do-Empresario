package com.casaempresario.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.casaempresario.app.R;
import com.casaempresario.app.adapter.EventAdapter;
import com.casaempresario.app.database.Evento;
import com.casaempresario.app.database.Mensagem;
import com.casaempresario.app.database.Usuario;
import com.casaempresario.app.databinding.ActivityMainBinding;
import com.casaempresario.app.repository.RepositoryCallback;
import com.casaempresario.app.repository.RepositoryProvider;
import com.casaempresario.app.util.SessionManager;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SessionManager sessionManager;

    // Adapters
    private EventAdapter homeEventAdapter;
    private EventAdapter interestsEventAdapter;
    private ChatInboxAdapter inboxAdapter;

    // States
    private boolean mostrandoEventosAtivos = true;
    private String textoBusca = "";
    private String categoriaSelecionada = "Todos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Casa do Empresário - Eventos");
        }

        setupBottomNavigation();
        setupHomeTab();
        setupInterestsTab();
        setupMessagesTab();
        setupProfileTab();

        atualizarVisibilidadeFab();

        // Verifica se a sessão do usuário é válida no banco (previne ID órfão de
        // migrações Room)
        if (sessionManager.isLogado()) {
            RepositoryProvider.getUserRepository(this).getUsuarioById(sessionManager.getUserId(),
                    new RepositoryCallback<Usuario>() {
                        @Override
                        public void onSuccess(Usuario user) {
                            if (user == null) {
                                sessionManager.logout();
                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this,
                                            "Sessão expirada devido a atualizações do sistema. Por favor, entre novamente.",
                                            Toast.LENGTH_LONG).show();
                                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                });
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            // Ignora erros de rede temporários no check inicial
                        }
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recarrega os dados ao retornar para esta tela
        carregarEventos(mostrandoEventosAtivos);
        carregarInteresses();
        carregarInboxChats();
        carregarPerfilStatus();
        atualizarVisibilidadeFab();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIGURAÇÃO DAS ABAS / NAVIGATION
    // ─────────────────────────────────────────────────────────────────────────

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            binding.layoutTabHome.setVisibility(itemId == R.id.nav_home ? View.VISIBLE : View.GONE);
            binding.layoutTabInterests.setVisibility(itemId == R.id.nav_interests ? View.VISIBLE : View.GONE);
            binding.layoutTabMessages.setVisibility(itemId == R.id.nav_messages ? View.VISIBLE : View.GONE);
            binding.layoutTabProfile.setVisibility(itemId == R.id.nav_profile ? View.VISIBLE : View.GONE);

            // Ações específicas ao abrir abas
            if (itemId == R.id.nav_home) {
                carregarEventos(mostrandoEventosAtivos);
            } else if (itemId == R.id.nav_interests) {
                carregarInteresses();
            } else if (itemId == R.id.nav_messages) {
                carregarInboxChats();
            } else if (itemId == R.id.nav_profile) {
                carregarPerfilStatus();
            }

            atualizarVisibilidadeFab();
            return true;
        });
    }

    private void atualizarVisibilidadeFab() {
        boolean noHome = binding.layoutTabHome.getVisibility() == View.VISIBLE;
        if (noHome && sessionManager.isOrganizador()) {
            binding.fabNovoEvento.setVisibility(View.VISIBLE);
            binding.fabNovoEvento.setOnClickListener(v -> startActivity(new Intent(this, CreateEventActivity.class)));
        } else {
            binding.fabNovoEvento.setVisibility(View.GONE);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 1: LISTAGEM E BUSCA DE EVENTOS (HOME)
    // ─────────────────────────────────────────────────────────────────────────

    private void setupHomeTab() {
        // Setup RecyclerView
        homeEventAdapter = new EventAdapter(new ArrayList<>(), evento -> {
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra("eventoId", evento.id);
            intent.putExtra("eventoTitulo", evento.titulo);
            startActivity(intent);
        });
        binding.recyclerEventos.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerEventos.setAdapter(homeEventAdapter);

        // Setup Tabs (Ativos / Finalizados)
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Ativos"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Finalizados"));
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mostrandoEventosAtivos = (tab.getPosition() == 0);
                carregarEventos(mostrandoEventosAtivos);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        // Swipe Refresh
        binding.swipeRefresh.setOnRefreshListener(() -> carregarEventos(mostrandoEventosAtivos));
        binding.swipeRefresh.setColorSchemeResources(R.color.purple_500);

        // Barra de Busca Dinâmica
        binding.etBusca.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textoBusca = s.toString().toLowerCase().trim();
                carregarEventos(mostrandoEventosAtivos);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Setup Chips de Categorias
        setupCategoriasChips();
    }

    private void setupCategoriasChips() {
        binding.chipGroupCategorias.removeAllViews();

        // Adiciona chip "Todos"
        com.google.android.material.chip.Chip chipTodos = new com.google.android.material.chip.Chip(this);
        chipTodos.setId(View.generateViewId());
        chipTodos.setText("Todos");
        chipTodos.setCheckable(true);
        chipTodos.setChecked(true);
        binding.chipGroupCategorias.addView(chipTodos);

        // Adiciona as categorias dinamicamente
        for (String cat : CreateEventActivity.CATEGORIAS) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setId(View.generateViewId());
            chip.setText(cat);
            chip.setCheckable(true);
            binding.chipGroupCategorias.addView(chip);
        }

        binding.chipGroupCategorias.setOnCheckedChangeListener((group, checkedId) -> {
            com.google.android.material.chip.Chip selected = group.findViewById(checkedId);
            if (selected != null) {
                categoriaSelecionada = selected.getText().toString();
            } else {
                categoriaSelecionada = "Todos";
            }
            carregarEventos(mostrandoEventosAtivos);
        });
    }

    private void carregarEventos(boolean ativos) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvVazio.setVisibility(View.GONE);

        RepositoryProvider.getEventRepository(this).getAllEventos(new RepositoryCallback<List<Evento>>() {
            @Override
            public void onSuccess(List<Evento> eventos) {
                List<Evento> filtrados = new ArrayList<>();

                for (Evento e : eventos) {
                    // Filtro de aba (Ativos vs. Finalizados)
                    boolean bateAba = ativos
                            ? ("AGENDADO".equals(e.status) || "EM_ANDAMENTO".equals(e.status))
                            : ("CONCLUIDO".equals(e.status) || "CANCELADO".equals(e.status));

                    // Filtro de busca (por título ou local)
                    boolean bateBusca = textoBusca.isEmpty() ||
                            (e.titulo != null && e.titulo.toLowerCase().contains(textoBusca)) ||
                            (e.local != null && e.local.toLowerCase().contains(textoBusca));

                    // Filtro de categoria
                    boolean bateCategoria = "Todos".equals(categoriaSelecionada) ||
                            (e.categoria != null && e.categoria.equalsIgnoreCase(categoriaSelecionada));

                    if (bateAba && bateBusca && bateCategoria) {
                        filtrados.add(e);
                    }
                }

                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    homeEventAdapter.atualizar(filtrados);
                    binding.tvVazio.setVisibility(filtrados.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    Toast.makeText(MainActivity.this, "Erro ao carregar eventos", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 2: MEUS INTERESSES E INTEGRAÇÃO DE MAPA
    // ─────────────────────────────────────────────────────────────────────────

    private void setupInterestsTab() {
        interestsEventAdapter = new EventAdapter(new ArrayList<>(), evento -> {
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra("eventoId", evento.id);
            intent.putExtra("eventoTitulo", evento.titulo);
            startActivity(intent);
        });
        binding.recyclerInteresses.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerInteresses.setAdapter(interestsEventAdapter);

        binding.btnVerNoMapa.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapActivity.class);
            startActivity(intent);
        });
    }

    private void carregarInteresses() {
        RepositoryProvider.getInterestRepository(this).getEventosDeInteresse(sessionManager.getUserId(),
                new RepositoryCallback<List<Evento>>() {
                    @Override
                    public void onSuccess(List<Evento> interesses) {
                        runOnUiThread(() -> {
                            interestsEventAdapter.atualizar(interesses);
                            binding.tvInteressesVazio.setVisibility(interesses.isEmpty() ? View.VISIBLE : View.GONE);
                            binding.btnVerNoMapa.setEnabled(!interesses.isEmpty());
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> Toast
                                .makeText(MainActivity.this, "Erro ao carregar interesses", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 3: CONVERSAS E MENSAGENS (INBOX)
    // ─────────────────────────────────────────────────────────────────────────

    private void setupMessagesTab() {
        inboxAdapter = new ChatInboxAdapter(new ArrayList<>(), item -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("eventoId", item.eventoId);
            intent.putExtra("outroUserId", item.outroUserId);
            startActivity(intent);
        });
        binding.recyclerChats.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerChats.setAdapter(inboxAdapter);
    }

    private void carregarInboxChats() {
        long currentUserId = sessionManager.getUserId();
        RepositoryProvider.getChatRepository(this).getTodasMensagensUsuario(currentUserId,
                new RepositoryCallback<List<Mensagem>>() {
                    private java.util.concurrent.atomic.AtomicInteger counter;
                    private java.util.concurrent.ConcurrentHashMap<String, ChatItem> threadMap;

                    @Override
                    public void onSuccess(List<Mensagem> todasMsg) {
                        if (todasMsg.isEmpty()) {
                            runOnUiThread(() -> {
                                inboxAdapter.atualizar(new ArrayList<>());
                                binding.tvChatsVazio.setVisibility(View.VISIBLE);
                            });
                            return;
                        }

                        threadMap = new java.util.concurrent.ConcurrentHashMap<>();
                        List<Mensagem> uniqueThreads = new ArrayList<>();
                        for (Mensagem m : todasMsg) {
                            long outroUserId = (m.remetenteId == currentUserId) ? m.destinatarioId : m.remetenteId;
                            String threadKey = outroUserId + "_" + m.eventoId;
                            if (!threadMap.containsKey(threadKey)) {
                                ChatItem item = new ChatItem();
                                item.outroUserId = outroUserId;
                                item.eventoId = m.eventoId;
                                item.ultimaMensagem = m.texto;
                                item.timestamp = m.timestamp != null ? m.timestamp : "";
                                threadMap.put(threadKey, item);
                                uniqueThreads.add(m);
                            }
                        }

                        counter = new java.util.concurrent.atomic.AtomicInteger(uniqueThreads.size() * 2);

                        for (Mensagem m : uniqueThreads) {
                            long outroUserId = (m.remetenteId == currentUserId) ? m.destinatarioId : m.remetenteId;
                            String threadKey = outroUserId + "_" + m.eventoId;
                            ChatItem item = threadMap.get(threadKey);

                            // Busca o usuário
                            RepositoryProvider.getUserRepository(MainActivity.this).getUsuarioById(outroUserId,
                                    new RepositoryCallback<Usuario>() {
                                        @Override
                                        public void onSuccess(Usuario outro) {
                                            if (outro != null) {
                                                item.outroUserNome = outro.nome;
                                            } else {
                                                item.outroUserNome = "Usuário #" + outroUserId;
                                            }
                                            checkComplete();
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            item.outroUserNome = "Usuário #" + outroUserId;
                                            checkComplete();
                                        }
                                    });

                            // Busca o evento
                            RepositoryProvider.getEventRepository(MainActivity.this).getEventoById(m.eventoId,
                                    new RepositoryCallback<Evento>() {
                                        @Override
                                        public void onSuccess(Evento e) {
                                            if (e != null) {
                                                item.eventoTitulo = e.titulo;
                                            } else {
                                                item.eventoTitulo = "Evento Desconhecido";
                                            }
                                            checkComplete();
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            item.eventoTitulo = "Evento Desconhecido";
                                            checkComplete();
                                        }
                                    });
                        }
                    }

                    private void checkComplete() {
                        if (counter.decrementAndGet() == 0) {
                            List<ChatItem> listaChats = new ArrayList<>(threadMap.values());
                            // Ordenar por hora (mais recente primeiro)
                            listaChats.sort((a, b) -> b.timestamp.compareTo(a.timestamp));
                            runOnUiThread(() -> {
                                inboxAdapter.atualizar(listaChats);
                                binding.tvChatsVazio.setVisibility(listaChats.isEmpty() ? View.VISIBLE : View.GONE);
                            });
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> Toast
                                .makeText(MainActivity.this, "Erro ao carregar conversas", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 4: PERFIL E CONFIGURAÇÕES
    // ─────────────────────────────────────────────────────────────────────────

    private void setupProfileTab() {
        binding.btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

    }

    private void carregarPerfilStatus() {
        binding.tvPerfilNome.setText(sessionManager.getNome());
        binding.tvPerfilEmail.setText(sessionManager.getEmail());
        String role = sessionManager.getRole();
        binding.tvPerfilRole.setText("Tipo de Conta: " + role);

    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADAPTER E MODEL PARA O INBOX (INTERNO)
    // ─────────────────────────────────────────────────────────────────────────

    interface OnChatItemClickListener {
        void onItemClick(ChatItem item);
    }

    private class ChatItem {
        long outroUserId;
        long eventoId;
        String outroUserNome;
        String eventoTitulo;
        String ultimaMensagem;
        String timestamp;
    }

    private class ChatInboxAdapter extends RecyclerView.Adapter<ChatInboxAdapter.ViewHolder> {
        private List<ChatItem> items;
        private OnChatItemClickListener listener;

        ChatInboxAdapter(List<ChatItem> items, OnChatItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        void atualizar(List<ChatItem> novos) {
            this.items = novos;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent,
                    false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChatItem item = items.get(position);
            holder.text1.setText(item.outroUserNome + " (sobre " + item.eventoTitulo + ")");
            holder.text2.setText(item.ultimaMensagem);
            holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;

            ViewHolder(View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
            }
        }
    }
}
