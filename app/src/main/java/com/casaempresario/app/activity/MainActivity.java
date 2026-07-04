package com.casaempresario.app.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.ImageView;
import android.provider.MediaStore;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.casaempresario.app.R;
import com.casaempresario.app.adapter.EventAdapter;
import com.casaempresario.app.database.Evento;
import com.casaempresario.app.database.Mensagem;
import com.casaempresario.app.database.Usuario;
import com.casaempresario.app.model.FeedPost;
import com.casaempresario.app.databinding.ActivityMainBinding;
import com.casaempresario.app.repository.RepositoryCallback;
import com.casaempresario.app.repository.RepositoryProvider;
import com.casaempresario.app.util.SessionManager;
import com.casaempresario.app.util.EventStatusUtils;
import com.casaempresario.app.util.NotificationHelper;
import com.casaempresario.app.util.NotificationPermissionHelper;
import com.casaempresario.app.service.FCMService;
import com.google.android.material.chip.Chip;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SessionManager sessionManager;

    // Listas da interface
    private EventAdapter homeEventAdapter;
    private EventAdapter interestsEventAdapter;
    private ChatInboxAdapter inboxAdapter;
    private FeedAdapter feedAdapter;
    private FirebaseFirestore feedFirestore;
    private ListenerRegistration feedListener;

    // Estado da tela
    private boolean mostrandoEventosAtivos = true;
    private String textoBusca = "";
    private String categoriaSelecionada = "Todos";
    private Uri feedImagemSelecionadaUri;
    private final Map<Long, String> organizadorNomeCache = new HashMap<>();

    private final ActivityResultLauncher<Intent> selecionarFotoPerfil =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        salvarFotoPerfil(uri);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> selecionarFotoFeed =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    feedImagemSelecionadaUri = result.getData().getData();
                    mostrarPreviewFeedImagem(feedImagemSelecionadaUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        NotificationHelper.createNotificationChannel(this);
        if (sessionManager.isLogado()) {
            NotificationPermissionHelper.requestPostNotificationsDelayed(this);
            FCMService.registrarToken(this);
        }

        configurarMarcaToolbar();

        binding.getRoot().setAlpha(0f);
        binding.getRoot().animate().alpha(1f).setDuration(320).start();

        setupBottomNavigation();
        setupHomeTab();
        setupFeedTab();
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

    private void configurarMarcaToolbar() {
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setTitle("");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        View brandView = getLayoutInflater().inflate(R.layout.view_toolbar_brand, binding.toolbar, false);
        androidx.appcompat.widget.Toolbar.LayoutParams params =
                new androidx.appcompat.widget.Toolbar.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
        params.gravity = android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL;
        binding.toolbar.addView(brandView, params);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLogado()) {
            NotificationPermissionHelper.requestPostNotificationsDelayed(this);
        }
        // Recarrega os dados ao retornar para esta tela
        carregarEventos(mostrandoEventosAtivos);
        carregarInteresses();
        carregarInboxChats();
        carregarPerfilStatus();
        atualizarVisibilidadeFab();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (feedListener != null) {
            feedListener.remove();
            feedListener = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NotificationPermissionHelper.REQUEST_POST_NOTIFICATIONS) {
            boolean granted = grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                Toast.makeText(this, "Notificações desativadas. Você pode ativar depois nas configurações do app.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navegação principal
    // ─────────────────────────────────────────────────────────────────────────

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            View selectedTab;

            binding.layoutTabHome.setVisibility(itemId == R.id.nav_home ? View.VISIBLE : View.GONE);
            binding.layoutTabFeed.setVisibility(itemId == R.id.nav_feed ? View.VISIBLE : View.GONE);
            binding.layoutTabInterests.setVisibility(itemId == R.id.nav_interests ? View.VISIBLE : View.GONE);
            binding.layoutTabMessages.setVisibility(itemId == R.id.nav_messages ? View.VISIBLE : View.GONE);
            binding.layoutTabProfile.setVisibility(itemId == R.id.nav_profile ? View.VISIBLE : View.GONE);

            // Ações específicas ao abrir abas
            if (itemId == R.id.nav_home) {
                selectedTab = binding.layoutTabHome;
                carregarEventos(mostrandoEventosAtivos);
            } else if (itemId == R.id.nav_feed) {
                selectedTab = binding.layoutTabFeed;
                iniciarFeedTempoReal();
            } else if (itemId == R.id.nav_interests) {
                selectedTab = binding.layoutTabInterests;
                carregarInteresses();
            } else if (itemId == R.id.nav_messages) {
                selectedTab = binding.layoutTabMessages;
                carregarInboxChats();
            } else {
                selectedTab = binding.layoutTabProfile;
                carregarPerfilStatus();
            }

            animarTrocaDeAba(selectedTab);
            atualizarVisibilidadeFab();
            return true;
        });
    }

    private void atualizarVisibilidadeFab() {
        boolean noHome = binding.layoutTabHome.getVisibility() == View.VISIBLE;
        boolean podeCriarEvento = sessionManager.isOrganizador();

        binding.fabNovoEvento.animate().cancel();

        if (noHome && podeCriarEvento) {
            binding.fabNovoEvento.setVisibility(View.VISIBLE);
            binding.fabNovoEvento.setAlpha(1f);
            binding.fabNovoEvento.setScaleX(1f);
            binding.fabNovoEvento.setScaleY(1f);
            binding.fabNovoEvento.bringToFront();
            binding.fabNovoEvento.setOnClickListener(v -> startActivity(new Intent(this, CreateEventActivity.class)));
        } else {
            binding.fabNovoEvento.setVisibility(View.GONE);
        }
    }

    private void animarTrocaDeAba(View view) {
        view.setAlpha(0f);
        view.setTranslationY(22f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(260)
                .start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lista e busca de eventos
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
        Chip chipTodos = new Chip(this);
        chipTodos.setId(View.generateViewId());
        chipTodos.setText("Todos");
        chipTodos.setCheckable(true);
        chipTodos.setChecked(true);
        estilizarChipCategoria(chipTodos);
        binding.chipGroupCategorias.addView(chipTodos);

        // Adiciona as categorias dinamicamente
        for (String cat : CreateEventActivity.CATEGORIAS) {
            Chip chip = new Chip(this);
            chip.setId(View.generateViewId());
            chip.setText(cat);
            chip.setCheckable(true);
            estilizarChipCategoria(chip);
            binding.chipGroupCategorias.addView(chip);
        }

        binding.chipGroupCategorias.setOnCheckedChangeListener((group, checkedId) -> {
            Chip selected = group.findViewById(checkedId);
            if (selected != null) {
                categoriaSelecionada = selected.getText().toString();
            } else {
                categoriaSelecionada = "Todos";
            }
            carregarEventos(mostrandoEventosAtivos);
        });
    }

    private void estilizarChipCategoria(Chip chip) {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };

        chip.setChipBackgroundColor(new ColorStateList(states, new int[]{
                getColor(R.color.cev_gold),
                getColor(R.color.cev_surface_2)
        }));
        chip.setTextColor(new ColorStateList(states, new int[]{
                getColor(R.color.cev_background),
                getColor(R.color.cev_text_primary)
        }));
        chip.setChipStrokeColor(new ColorStateList(states, new int[]{
                getColor(R.color.cev_gold),
                getColor(R.color.cev_gold_border)
        }));
        chip.setChipStrokeWidth(1f);
        chip.setRippleColor(ColorStateList.valueOf(getColor(R.color.cev_gold_border)));
        chip.setEnsureMinTouchTargetSize(false);
        chip.setTextSize(13f);
    }

    private void carregarEventos(boolean ativos) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvVazio.setVisibility(View.GONE);

        RepositoryProvider.getEventRepository(this).getAllEventos(new RepositoryCallback<List<Evento>>() {
            @Override
            public void onSuccess(List<Evento> eventos) {
                prepararEventosParaExibicao(eventos != null ? eventos : new ArrayList<>(), ativos);
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

    private void prepararEventosParaExibicao(List<Evento> eventos, boolean ativos) {
        Set<Long> organizadoresParaBuscar = new HashSet<>();

        for (Evento evento : eventos) {
            atualizarStatusAutomaticoSeNecessario(evento);
            if (evento.criadoPor != null && !organizadorNomeCache.containsKey(evento.criadoPor)) {
                organizadoresParaBuscar.add(evento.criadoPor);
            }
        }

        if (organizadoresParaBuscar.isEmpty()) {
            aplicarFiltrosEOrdenacao(eventos, ativos);
            return;
        }

        AtomicInteger pendentes = new AtomicInteger(organizadoresParaBuscar.size());
        for (Long organizadorId : organizadoresParaBuscar) {
            RepositoryProvider.getUserRepository(this).getUsuarioById(organizadorId, new RepositoryCallback<Usuario>() {
                @Override
                public void onSuccess(Usuario usuario) {
                    if (usuario != null && usuario.nome != null) {
                        organizadorNomeCache.put(organizadorId, usuario.nome);
                    } else {
                        organizadorNomeCache.put(organizadorId, "Organizador #" + organizadorId);
                    }
                    finalizarBuscaOrganizadoresSePossivel(pendentes, eventos, ativos);
                }

                @Override
                public void onError(Exception e) {
                    organizadorNomeCache.put(organizadorId, "Organizador #" + organizadorId);
                    finalizarBuscaOrganizadoresSePossivel(pendentes, eventos, ativos);
                }
            });
        }
    }

    private void finalizarBuscaOrganizadoresSePossivel(AtomicInteger pendentes, List<Evento> eventos, boolean ativos) {
        if (pendentes.decrementAndGet() == 0) {
            aplicarFiltrosEOrdenacao(eventos, ativos);
        }
    }

    private void atualizarStatusAutomaticoSeNecessario(Evento evento) {
        if (evento == null || evento.id == 0) return;

        String statusAnterior = evento.status != null ? evento.status : "";
        String statusNovo = EventStatusUtils.calcularStatusAutomatico(evento);
        if (!statusAnterior.equalsIgnoreCase(statusNovo)) {
            evento.status = statusNovo;
            RepositoryProvider.getEventRepository(this).updateStatus(evento.id, statusNovo, new RepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void result) { }

                @Override
                public void onError(Exception e) { }
            });
        }
    }

    private void aplicarFiltrosEOrdenacao(List<Evento> eventos, boolean ativos) {
        List<Evento> filtrados = new ArrayList<>();

        for (Evento e : eventos) {
            if (e == null) continue;
            String status = EventStatusUtils.calcularStatusAutomatico(e);
            e.status = status;

            boolean bateAba = ativos
                    ? ("AGENDADO".equalsIgnoreCase(status) || "EM_ANDAMENTO".equalsIgnoreCase(status))
                    : ("CONCLUIDO".equalsIgnoreCase(status) || "CANCELADO".equalsIgnoreCase(status));

            String organizadorNome = e.criadoPor != null ? organizadorNomeCache.get(e.criadoPor) : null;
            boolean bateBusca = textoBusca.isEmpty()
                    || contem(e.titulo, textoBusca)
                    || contem(e.local, textoBusca)
                    || contem(e.descricao, textoBusca)
                    || contem(e.categoria, textoBusca)
                    || contem(status, textoBusca)
                    || contem(organizadorNome, textoBusca);

            boolean bateCategoria = "Todos".equals(categoriaSelecionada)
                    || (e.categoria != null && e.categoria.equalsIgnoreCase(categoriaSelecionada));

            if (bateAba && bateBusca && bateCategoria) {
                filtrados.add(e);
            }
        }

        Collections.sort(filtrados, (a, b) -> {
            long ta = EventStatusUtils.getEventTimeMillis(a);
            long tb = EventStatusUtils.getEventTimeMillis(b);
            return ativos ? Long.compare(ta, tb) : Long.compare(tb, ta);
        });

        runOnUiThread(() -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.swipeRefresh.setRefreshing(false);
            homeEventAdapter.atualizar(filtrados);
            binding.tvVazio.setVisibility(filtrados.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private boolean contem(String valor, String busca) {
        return valor != null && busca != null && valor.toLowerCase().contains(busca);
    }



    // ─────────────────────────────────────────────────────────────────────────
    // Feed de comunicação
    // ─────────────────────────────────────────────────────────────────────────

    private void setupFeedTab() {
        feedFirestore = FirebaseFirestore.getInstance();
        feedAdapter = new FeedAdapter(new ArrayList<>());
        binding.recyclerFeed.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerFeed.setAdapter(feedAdapter);
        binding.progressFeed.setVisibility(View.VISIBLE);

        boolean visitante = !sessionManager.isLogado();
        boolean organizador = sessionManager.isOrganizador();

        if (visitante) {
            binding.tvFeedTituloPublicacao.setText("Acompanhe o mural");
            binding.etFeedMensagem.setHint("Entre para participar da conversa");
            binding.btnFeedEnviar.setText("Entrar para publicar");
        } else if (organizador) {
            binding.tvFeedTituloPublicacao.setText("Publicar aviso oficial");
            binding.etFeedMensagem.setHint("Ex.: Últimas vagas para o evento de hoje. Esperamos vocês!");
            binding.btnFeedEnviar.setText("Publicar como organizador");
        } else {
            binding.tvFeedTituloPublicacao.setText("Participar da conversa");
            binding.etFeedMensagem.setHint("Escreva uma mensagem pública para a comunidade");
            binding.btnFeedEnviar.setText("Enviar mensagem");
        }

        binding.btnFeedFoto.setOnClickListener(v -> abrirGaleriaFeed());
        binding.btnFeedRemoverImagem.setOnClickListener(v -> limparImagemFeed());
        binding.btnFeedEnviar.setOnClickListener(v -> enviarMensagemFeed());
        iniciarFeedTempoReal();
    }

    private void iniciarFeedTempoReal() {
        if (feedFirestore == null) {
            feedFirestore = FirebaseFirestore.getInstance();
        }

        if (feedListener != null) {
            return;
        }

        binding.progressFeed.setVisibility(View.VISIBLE);
        feedListener = feedFirestore.collection("feed_posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        runOnUiThread(() -> {
                            binding.progressFeed.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Erro ao atualizar o feed", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    List<FeedPost> posts = new ArrayList<>();
                    if (snapshots != null) {
                        snapshots.forEach(doc -> {
                            FeedPost post = doc.toObject(FeedPost.class);
                            post.id = doc.getId();
                            posts.add(post);
                        });
                    }

                    runOnUiThread(() -> {
                        binding.progressFeed.setVisibility(View.GONE);
                        feedAdapter.atualizar(posts);
                        binding.tvFeedVazio.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
                    });
                });
    }

    private void abrirGaleriaFeed() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        selecionarFotoFeed.launch(intent);
    }

    private void mostrarPreviewFeedImagem(Uri uri) {
        if (uri == null) return;
        binding.layoutFeedImagemPreview.setVisibility(View.VISIBLE);
        Glide.with(this).load(uri).centerCrop().into(binding.imgFeedPreview);
    }

    private void limparImagemFeed() {
        feedImagemSelecionadaUri = null;
        binding.imgFeedPreview.setImageDrawable(null);
        binding.layoutFeedImagemPreview.setVisibility(View.GONE);
    }

    private void enviarMensagemFeed() {
        String texto = binding.etFeedMensagem.getText() != null
                ? binding.etFeedMensagem.getText().toString().trim()
                : "";

        if (!sessionManager.isLogado()) {
            solicitarLoginParaAcao("Entre para publicar no feed da comunidade.");
            return;
        }

        if (texto.isEmpty() && feedImagemSelecionadaUri == null) {
            Toast.makeText(this, "Digite uma mensagem ou selecione uma foto", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean organizador = sessionManager.getRole() != null
                && !"PARTICIPANTE".equalsIgnoreCase(sessionManager.getRole().trim());
        long agora = System.currentTimeMillis();
        String id = agora + "_" + sessionManager.getUserId();

        binding.btnFeedEnviar.setEnabled(false);
        binding.btnFeedFoto.setEnabled(false);

        if (feedImagemSelecionadaUri != null) {
            binding.btnFeedEnviar.setText("Enviando foto...");
            uploadImageToFirebase(feedImagemSelecionadaUri, "feed_images", new ImageUploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    salvarPostFeed(id, texto, organizador, agora, imageUrl);
                }

                @Override
                public void onError(Exception e) {
                    try {
                        String localPath = copiarImagemParaArquivoInterno(feedImagemSelecionadaUri, "FEED_");
                        salvarPostFeed(id, texto, organizador, agora, localPath);
                    } catch (Exception ex) {
                        runOnUiThread(() -> {
                            binding.btnFeedEnviar.setEnabled(true);
                            binding.btnFeedFoto.setEnabled(true);
                            restaurarTextoBotaoFeed(organizador);
                            Toast.makeText(MainActivity.this, "Não foi possível enviar a foto do feed", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        } else {
            salvarPostFeed(id, texto, organizador, agora, null);
        }
    }

    private void salvarPostFeed(String id, String texto, boolean organizador, long agora, String imagemUri) {
        Map<String, Object> post = new HashMap<>();
        post.put("id", id);
        post.put("autorId", sessionManager.getUserId());
        post.put("autorNome", sessionManager.getNome());
        post.put("autorEmail", sessionManager.getEmail());
        post.put("autorRole", sessionManager.getRole());
        post.put("autorFotoUri", sessionManager.getProfilePhotoUri());
        post.put("texto", texto);
        post.put("imagemUri", imagemUri);
        post.put("tipo", organizador ? "OFICIAL" : "COMUNIDADE");
        post.put("createdAt", agora);

        feedFirestore.collection("feed_posts")
                .document(id)
                .set(post)
                .addOnSuccessListener(unused -> runOnUiThread(() -> {
                    binding.etFeedMensagem.setText("");
                    limparImagemFeed();
                    binding.btnFeedEnviar.setEnabled(true);
                    binding.btnFeedFoto.setEnabled(true);
                    restaurarTextoBotaoFeed(organizador);
                }))
                .addOnFailureListener(e -> runOnUiThread(() -> {
                    binding.btnFeedEnviar.setEnabled(true);
                    binding.btnFeedFoto.setEnabled(true);
                    restaurarTextoBotaoFeed(organizador);
                    Toast.makeText(MainActivity.this, "Não foi possível publicar no feed", Toast.LENGTH_SHORT).show();
                }));
    }

    private void restaurarTextoBotaoFeed(boolean organizador) {
        binding.btnFeedEnviar.setText(organizador ? "Publicar como organizador" : "Enviar mensagem");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 3: MEUS INTERESSES E INTEGRAÇÃO DE MAPA
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
        if (!sessionManager.isLogado()) {
            runOnUiThread(() -> {
                interestsEventAdapter.atualizar(new ArrayList<>());
                binding.tvInteressesVazio.setText("Entre para salvar eventos de interesse.");
                binding.tvInteressesVazio.setVisibility(View.VISIBLE);
                binding.btnVerNoMapa.setEnabled(false);
            });
            return;
        }
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
    // TAB 4: CONVERSAS E MENSAGENS (INBOX)
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
        if (!sessionManager.isLogado()) {
            runOnUiThread(() -> {
                inboxAdapter.atualizar(new ArrayList<>());
                binding.tvChatsVazio.setText("Entre para acessar suas mensagens.");
                binding.tvChatsVazio.setVisibility(View.VISIBLE);
            });
            return;
        }
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
    // Perfil e configurações
    // ─────────────────────────────────────────────────────────────────────────

    private void setupProfileTab() {
        binding.btnAlterarFotoPerfil.setOnClickListener(v -> abrirGaleriaPerfil());
        binding.btnPerfilCompleto.setOnClickListener(v -> startActivity(new Intent(this, ProfileDetailsActivity.class)));
        binding.btnCentralNotificacoes.setOnClickListener(v -> startActivity(new Intent(this, NotificationCenterActivity.class)));
        binding.btnDashboardOrganizador.setOnClickListener(v -> {
            if (sessionManager.isOrganizador()) {
                startActivity(new Intent(this, OrganizerDashboardActivity.class));
            } else {
                Toast.makeText(this, "Recurso disponível para organizadores.", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void carregarPerfilStatus() {
        if (!sessionManager.isLogado()) {
            binding.tvPerfilNome.setText("Visitante");
            binding.tvPerfilEmail.setText("Explore eventos sem login");
            binding.tvPerfilRole.setText("Tipo de Conta: VISITANTE");
            binding.imgPerfilFoto.setImageDrawable(null);
            binding.imgPerfilFoto.setVisibility(View.GONE);
            binding.tvPerfilAvatar.setVisibility(View.VISIBLE);
            binding.tvPerfilAvatar.setText("CE");
            binding.btnAlterarFotoPerfil.setEnabled(false);
            binding.btnAlterarFotoPerfil.setText("Entre para alterar foto");
            binding.btnDashboardOrganizador.setVisibility(View.GONE);
            binding.btnPerfilCompleto.setText("Entrar para completar perfil");
            binding.tvProfileCompletionPreview.setText("Entre para habilitar perfil corporativo e notificações personalizadas.");
            binding.tvProfileCompanyPreview.setText("Você pode explorar eventos como visitante, mas recursos de networking exigem autenticação.");
            binding.btnLogout.setText("Entrar ou criar conta");
            return;
        }
        binding.btnAlterarFotoPerfil.setEnabled(true);
        binding.btnAlterarFotoPerfil.setText("Alterar foto de perfil");
        binding.btnPerfilCompleto.setText("Ver perfil completo");
        binding.btnDashboardOrganizador.setVisibility(sessionManager.isOrganizador() ? View.VISIBLE : View.GONE);
        binding.btnLogout.setText("Fazer logout");
        atualizarResumoPerfilCorporativo();
        binding.tvPerfilNome.setText(sessionManager.getNome());
        binding.tvPerfilEmail.setText(sessionManager.getEmail());
        String role = sessionManager.getRole();
        binding.tvPerfilRole.setText("Tipo de Conta: " + role);
        exibirFotoPerfil(sessionManager.getProfilePhotoUri());

        RepositoryProvider.getUserRepository(this).getUsuarioById(sessionManager.getUserId(), new RepositoryCallback<Usuario>() {
            @Override
            public void onSuccess(Usuario usuario) {
                if (usuario != null) {
                    runOnUiThread(() -> {
                        if (usuario.fotoPerfilUri != null && !usuario.fotoPerfilUri.trim().isEmpty()) {
                            sessionManager.setProfilePhotoUri(usuario.fotoPerfilUri);
                            exibirFotoPerfil(usuario.fotoPerfilUri);
                        }
                        sessionManager.salvarPerfilProfissional(
                                usuario.empresa,
                                usuario.cargo,
                                usuario.cidade,
                                usuario.telefone,
                                usuario.linkedin,
                                usuario.bio);
                        atualizarResumoPerfilCorporativo();
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                // Mantém a foto local salva na sessão.
            }
        });
    }

    private void atualizarResumoPerfilCorporativo() {
        int percentual = sessionManager.getPercentualPerfilCompleto();
        binding.tvProfileCompletionPreview.setText("Perfil corporativo " + percentual + "% completo");

        String empresa = sessionManager.getEmpresa();
        String cargo = sessionManager.getCargo();
        String cidade = sessionManager.getCidade();
        if ((empresa == null || empresa.trim().isEmpty())
                && (cargo == null || cargo.trim().isEmpty())
                && (cidade == null || cidade.trim().isEmpty())) {
            binding.tvProfileCompanyPreview.setText("Complete empresa, cargo e cidade para melhorar o networking.");
            return;
        }

        StringBuilder resumo = new StringBuilder();
        if (cargo != null && !cargo.trim().isEmpty()) resumo.append(cargo.trim());
        if (empresa != null && !empresa.trim().isEmpty()) {
            if (resumo.length() > 0) resumo.append(" • ");
            resumo.append(empresa.trim());
        }
        if (cidade != null && !cidade.trim().isEmpty()) {
            if (resumo.length() > 0) resumo.append(" • ");
            resumo.append(cidade.trim());
        }
        binding.tvProfileCompanyPreview.setText(resumo.toString());
    }

    private void abrirGaleriaPerfil() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        selecionarFotoPerfil.launch(intent);
    }

    private void salvarFotoPerfil(Uri uri) {
        if (uri == null) return;
        binding.btnAlterarFotoPerfil.setEnabled(false);
        binding.btnAlterarFotoPerfil.setText("Salvando foto...");

        uploadImageToFirebase(uri, "profile_photos", new ImageUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                salvarFotoPerfilNoUsuario(imageUrl);
            }

            @Override
            public void onError(Exception e) {
                try {
                    String localPath = copiarImagemParaArquivoInterno(uri, "PERFIL_");
                    salvarFotoPerfilNoUsuario(localPath);
                } catch (Exception ex) {
                    runOnUiThread(() -> {
                        binding.btnAlterarFotoPerfil.setEnabled(true);
                        binding.btnAlterarFotoPerfil.setText("Alterar foto de perfil");
                        Toast.makeText(MainActivity.this, "Erro ao salvar foto de perfil", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void salvarFotoPerfilNoUsuario(String fotoUri) {
        RepositoryProvider.getUserRepository(this).updateFotoPerfil(sessionManager.getUserId(), fotoUri, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    sessionManager.setProfilePhotoUri(fotoUri);
                    exibirFotoPerfil(fotoUri);
                    atualizarFotoPerfilNosPosts(fotoUri);
                    binding.btnAlterarFotoPerfil.setEnabled(true);
                    binding.btnAlterarFotoPerfil.setText("Alterar foto de perfil");
                    Toast.makeText(MainActivity.this, "Foto de perfil atualizada!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    binding.btnAlterarFotoPerfil.setEnabled(true);
                    binding.btnAlterarFotoPerfil.setText("Alterar foto de perfil");
                    Toast.makeText(MainActivity.this, "Erro ao atualizar perfil", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void atualizarFotoPerfilNosPosts(String fotoUri) {
        if (feedFirestore == null) {
            feedFirestore = FirebaseFirestore.getInstance();
        }
        feedFirestore.collection("feed_posts")
                .whereEqualTo("autorId", sessionManager.getUserId())
                .get()
                .addOnSuccessListener(snapshots -> snapshots.forEach(doc ->
                        doc.getReference().update("autorFotoUri", fotoUri)));
    }

    private void exibirFotoPerfil(String fotoUri) {
        binding.tvPerfilAvatar.setText(gerarIniciais(sessionManager.getNome()));
        if (fotoUri != null && !fotoUri.trim().isEmpty()) {
            binding.imgPerfilFoto.setVisibility(View.VISIBLE);
            binding.tvPerfilAvatar.setVisibility(View.GONE);
            Glide.with(this).load(fotoUri).circleCrop().into(binding.imgPerfilFoto);
        } else {
            binding.imgPerfilFoto.setVisibility(View.GONE);
            binding.tvPerfilAvatar.setVisibility(View.VISIBLE);
        }
    }

    private String copiarImagemParaArquivoInterno(Uri uri, String prefixo) throws Exception {
        String fileName = prefixo + System.currentTimeMillis() + ".jpg";
        File file = new File(getFilesDir(), fileName);
        try (InputStream is = getContentResolver().openInputStream(uri);
             FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int read;
            while (is != null && (read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
        }
        return file.getAbsolutePath();
    }

    private void uploadImageToFirebase(Uri uri, String folder, ImageUploadCallback callback) {
        String fileName = folder + "/" + sessionManager.getUserId() + "_" + System.currentTimeMillis() + ".jpg";
        FirebaseStorage.getInstance()
                .getReference()
                .child(fileName)
                .putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception exception = task.getException();
                        if (exception != null) {
                            throw exception;
                        }
                        throw new RuntimeException("Falha ao enviar imagem");
                    }
                    return task.getResult().getStorage().getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> callback.onSuccess(downloadUri.toString()))
                .addOnFailureListener(callback::onError);
    }

    private String gerarIniciais(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            return "CE";
        }
        String[] partes = nome.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
        return (partes[0].substring(0, 1) + partes[partes.length - 1].substring(0, 1)).toUpperCase();
    }


    private void solicitarLoginParaAcao(String mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    private interface ImageUploadCallback {
        void onSuccess(String imageUrl);
        void onError(Exception e);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lista de publicações
    // ─────────────────────────────────────────────────────────────────────────

    private class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ViewHolder> {
        private List<FeedPost> posts;
        private final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt", "BR"));

        FeedAdapter(List<FeedPost> posts) {
            this.posts = posts;
        }

        void atualizar(List<FeedPost> novos) {
            this.posts = novos;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed_post, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FeedPost post = posts.get(position);
            String nome = post.autorNome != null && !post.autorNome.trim().isEmpty()
                    ? post.autorNome
                    : "Usuário";
            String role = post.autorRole != null ? post.autorRole : "PARTICIPANTE";
            boolean oficial = "OFICIAL".equalsIgnoreCase(post.tipo) || !"PARTICIPANTE".equalsIgnoreCase(role);

            holder.autor.setText(nome);
            holder.data.setText(post.createdAt > 0 ? formatter.format(new Date(post.createdAt)) : "Agora");
            holder.tipo.setText(oficial ? "OFICIAL" : "COMUNIDADE");
            holder.avatar.setText(gerarIniciais(nome));

            if (post.texto != null && !post.texto.trim().isEmpty()) {
                holder.texto.setText(post.texto);
                holder.texto.setVisibility(View.VISIBLE);
            } else {
                holder.texto.setVisibility(View.GONE);
            }

            if (post.autorFotoUri != null && !post.autorFotoUri.trim().isEmpty()) {
                holder.imgAvatar.setVisibility(View.VISIBLE);
                holder.avatar.setVisibility(View.GONE);
                Glide.with(holder.itemView).load(post.autorFotoUri).circleCrop().into(holder.imgAvatar);
            } else {
                holder.imgAvatar.setVisibility(View.GONE);
                holder.avatar.setVisibility(View.VISIBLE);
            }

            if (post.imagemUri != null && !post.imagemUri.trim().isEmpty()) {
                holder.imagem.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView).load(post.imagemUri).centerCrop().into(holder.imagem);
            } else {
                holder.imagem.setVisibility(View.GONE);
            }

            if (oficial) {
                holder.tipo.setBackgroundResource(R.drawable.bg_metric_pill_secondary);
                holder.tipo.setTextColor(getColor(R.color.cev_text_primary));
            } else {
                holder.tipo.setBackgroundResource(R.drawable.bg_chip_subtle);
                holder.tipo.setTextColor(getColor(R.color.cev_gold));
            }
        }

        @Override
        public int getItemCount() {
            return posts.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView avatar, autor, data, tipo, texto;
            ImageView imgAvatar, imagem;

            ViewHolder(View v) {
                super(v);
                avatar = v.findViewById(R.id.tv_feed_avatar);
                imgAvatar = v.findViewById(R.id.img_feed_avatar);
                autor = v.findViewById(R.id.tv_feed_autor);
                data = v.findViewById(R.id.tv_feed_data);
                tipo = v.findViewById(R.id.tv_feed_tipo);
                texto = v.findViewById(R.id.tv_feed_texto);
                imagem = v.findViewById(R.id.img_feed_imagem);
            }
        }
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
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_inbox, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChatItem item = items.get(position);
            holder.titulo.setText(item.outroUserNome);
            holder.evento.setText("Evento: " + item.eventoTitulo);
            holder.ultimaMensagem.setText(item.ultimaMensagem);
            holder.data.setText(formatarDataCurta(item.timestamp));
            holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        }

        private String formatarDataCurta(String timestamp) {
            if (timestamp == null || timestamp.trim().isEmpty()) {
                return "Agora";
            }
            String valor = timestamp.trim();
            if (valor.length() >= 10) {
                return valor.substring(8, 10) + "/" + valor.substring(5, 7);
            }
            return valor;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView titulo, evento, ultimaMensagem, data;

            ViewHolder(View v) {
                super(v);
                titulo = v.findViewById(R.id.tv_chat_titulo);
                evento = v.findViewById(R.id.tv_chat_evento);
                ultimaMensagem = v.findViewById(R.id.tv_chat_ultima_mensagem);
                data = v.findViewById(R.id.tv_chat_data);
            }
        }
    }
}
