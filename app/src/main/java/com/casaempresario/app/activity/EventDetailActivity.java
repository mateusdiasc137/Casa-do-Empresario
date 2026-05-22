package com.casaempresario.app.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;
import java.io.File;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import android.net.Uri;
import com.bumptech.glide.Glide;
import com.casaempresario.app.adapter.PhotoAdapter;
import com.casaempresario.app.database.Evento;
import com.casaempresario.app.database.Interesse;
import com.casaempresario.app.databinding.ActivityEventDetailBinding;
import com.casaempresario.app.model.EventPhoto;
import com.casaempresario.app.repository.RepositoryCallback;
import com.casaempresario.app.repository.RepositoryProvider;
import com.casaempresario.app.util.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.provider.CalendarContract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventDetailActivity extends AppCompatActivity {

    private ActivityEventDetailBinding binding;
    private SessionManager sessionManager;
    private PhotoAdapter photoAdapter;
    private Long eventoId;
    private Evento currentEvento;

    // Labels e valores dos status disponíveis
    private static final String[] STATUS_LABELS = {
            "📅 Agendado",
            "▶️ Em andamento",
            "✅ Concluído",
            "❌ Cancelado"
    };
    private static final String[] STATUS_VALORES = {
            "AGENDADO",
            "EM_ANDAMENTO",
            "CONCLUIDO",
            "CANCELADO"
    };

    private final ActivityResultLauncher<Intent> selecionarFoto =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    mostrarDialogLegenda(uri);
                }
            });

    private final ActivityResultLauncher<Intent> editarEventoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    carregarEvento();
                }
            });

    private final ActivityResultLauncher<Intent> verDetalheFotoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    carregarFotos();
                }
            });

    private final ActivityResultLauncher<String> pedirPermissao =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) abrirGaleria();
                else Toast.makeText(this, "Permissão necessária", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEventDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        eventoId = getIntent().getLongExtra("eventoId", -1);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupFotos();
        carregarEvento();
        carregarFotos();

        // Botão editar evento
        binding.btnEditar.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateEventActivity.class);
            intent.putExtra("eventoId", eventoId);
            editarEventoLauncher.launch(intent);
        });

        // Botão alterar status
        binding.tvStatus.setOnClickListener(v -> {
            if (currentEvento != null && sessionManager.isOrganizador() && currentEvento.criadoPor != null && currentEvento.criadoPor.equals(sessionManager.getUserId())) {
                mostrarDialogStatus();
            }
        });

        // Botão excluir evento
        binding.btnExcluir.setOnClickListener(v -> confirmarExclusaoEvento());

        // Apenas Organizadores podem adicionar fotos
        if (sessionManager.isLogado() && sessionManager.isOrganizador()) {
            binding.fabAdicionarFoto.setVisibility(View.VISIBLE);
            binding.fabAdicionarFoto.setOnClickListener(v -> verificarPermissao());
        } else {
            binding.fabAdicionarFoto.setVisibility(View.GONE);
        }

        setupAcoes();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Alteração de status
    // ─────────────────────────────────────────────────────────────────────────

    private void mostrarDialogStatus() {
        // Descobre qual é o status atual para marcá-lo no dialog
        String statusAtual = (currentEvento != null && currentEvento.status != null)
                ? currentEvento.status : "AGENDADO";

        int indiceSelecionado = 0;
        for (int i = 0; i < STATUS_VALORES.length; i++) {
            if (STATUS_VALORES[i].equals(statusAtual)) {
                indiceSelecionado = i;
                break;
            }
        }

        final int[] escolhido = {indiceSelecionado};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Alterar Status do Evento")
                .setSingleChoiceItems(STATUS_LABELS, indiceSelecionado, (dialog, which) -> {
                    escolhido[0] = which;
                })
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    String novoStatus = STATUS_VALORES[escolhido[0]];
                    if (!novoStatus.equals(statusAtual)) {
                        salvarNovoStatus(novoStatus);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void salvarNovoStatus(String novoStatus) {
        if (currentEvento == null || !sessionManager.isOrganizador() || currentEvento.criadoPor == null || !currentEvento.criadoPor.equals(sessionManager.getUserId())) {
            Toast.makeText(this, "Permissão negada: apenas o criador do evento pode alterar o status.", Toast.LENGTH_SHORT).show();
            return;
        }
        RepositoryProvider.getEventRepository(this).updateStatus(eventoId, novoStatus, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if ("CONCLUIDO".equals(novoStatus) || "CANCELADO".equals(novoStatus)) {
                    RepositoryProvider.getInterestRepository(EventDetailActivity.this).deleteByEvento(eventoId, new RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void interestResult) {
                            runOnUiThread(() -> {
                                Toast.makeText(EventDetailActivity.this, "Status atualizado e interesses limpos!", Toast.LENGTH_SHORT).show();
                                checkInterest(); // Recarrega estado local do coração/interesse
                                carregarEvento(); // Atualiza a tela com o novo status
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() -> {
                                Toast.makeText(EventDetailActivity.this, "Status atualizado (erro ao limpar interesses)", Toast.LENGTH_SHORT).show();
                                checkInterest();
                                carregarEvento();
                            });
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(EventDetailActivity.this, "Status atualizado!", Toast.LENGTH_SHORT).show();
                        carregarEvento(); // Atualiza a tela com o novo status
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(EventDetailActivity.this, "Erro ao atualizar status", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Carregar dados do evento
    // ─────────────────────────────────────────────────────────────────────────

    private void setupFotos() {
        photoAdapter = new PhotoAdapter(new ArrayList<>(), sessionManager, this::confirmarDeletar, this::abrirVisualizacaoPost);
        binding.recyclerFotos.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerFotos.setAdapter(photoAdapter);
    }

    private void abrirVisualizacaoPost(EventPhoto foto) {
        Intent intent = new Intent(this, PhotoPostActivity.class);
        intent.putExtra("fotoId", foto.getId());
        verDetalheFotoLauncher.launch(intent);
    }

    private void carregarEvento() {
        RepositoryProvider.getEventRepository(this).getEventoById(eventoId, new RepositoryCallback<Evento>() {
            @Override
            public void onSuccess(Evento eventoDb) {
                runOnUiThread(() -> {
                    if (eventoDb != null) {
                        currentEvento = eventoDb;
                        preencherDados(eventoDb);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(EventDetailActivity.this, "Erro ao carregar evento", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void preencherDados(Evento evento) {

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(evento.titulo);
        }

        binding.tvTitulo.setText(evento.titulo);

        binding.tvLocal.setText(
                "📍 " + evento.local
        );

        binding.tvData.setText(
                "📅 " + formatarData(evento.dataEvento)
        );

        binding.tvDescricao.setText(
                evento.descricao != null
                        ? evento.descricao
                        : "Sem descrição"
        );

        binding.tvStatus.setText(evento.status);

        // Categoria
        if (evento.categoria != null && !evento.categoria.trim().isEmpty()) {
            binding.tvCategoria.setText("🏷️ " + evento.categoria);
            binding.tvCategoria.setVisibility(View.VISIBLE);
        } else {
            binding.tvCategoria.setVisibility(View.GONE);
        }

        // Botão de Mapa Interativo
        if (evento.latitude != 0.0 || evento.longitude != 0.0) {
            binding.btnVerMapa.setVisibility(View.VISIBLE);
            binding.btnVerMapa.setOnClickListener(v -> {
                Intent intent = new Intent(this, MapActivity.class);
                intent.putExtra("focusEventoId", eventoId);
                startActivity(intent);
            });
        } else {
            binding.btnVerMapa.setVisibility(View.GONE);
        }

        // ─────────────────────────────────────
        // CARREGAR BANNER
        // ─────────────────────────────────────

        if (evento.bannerUri != null
                && !evento.bannerUri.isEmpty()) {

            Glide.with(this)
                    .load(evento.bannerUri)
                    .into(binding.imgCapa);

        } else {

            binding.imgCapa.setImageResource(
                    com.casaempresario.app.R.drawable.ic_event_placeholder
            );
        }

        // ─────────────────────────────────────
        // Cor do status
        // ─────────────────────────────────────

        int color;

        switch (evento.status != null
                ? evento.status
                : "") {

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

        binding.tvStatus.setTextColor(color);

        if (evento.capacidadeMaxima != null) {

            binding.tvCapacidade.setText(
                    "👥 Capacidade: "
                            + evento.capacidadeMaxima
            );
        }

        // Regra de edição/exclusão/status para o criador do evento (Organizador)
        boolean isCreator = sessionManager.isOrganizador() && evento.criadoPor != null && evento.criadoPor.equals(sessionManager.getUserId());
        if (isCreator) {
            binding.btnEditar.setVisibility(View.VISIBLE);
            binding.btnExcluir.setVisibility(View.VISIBLE);
            binding.tvStatus.setClickable(true);
            binding.tvStatus.setFocusable(true);
        } else {
            binding.btnEditar.setVisibility(View.GONE);
            binding.btnExcluir.setVisibility(View.GONE);
            binding.tvStatus.setClickable(false);
            binding.tvStatus.setFocusable(false);
        }

        // Falar com Organizador só é visível para participantes (PARTICIPANTE) e se não for o próprio criador do evento
        boolean isParticipant = "PARTICIPANTE".equals(sessionManager.getRole());
        if (isParticipant && evento.criadoPor != null && !evento.criadoPor.equals(sessionManager.getUserId())) {
            binding.btnChat.setVisibility(View.VISIBLE);
        } else {
            binding.btnChat.setVisibility(View.GONE);
        }

        // Bloqueio visual se o evento estiver concluído ou cancelado
        boolean finalizado = "CONCLUIDO".equals(evento.status) || "CANCELADO".equals(evento.status);
        if (finalizado) {
            binding.btnInteresse.setEnabled(false);
            binding.btnInteresse.setAlpha(0.5f);
            binding.btnAgenda.setEnabled(false);
            binding.btnAgenda.setAlpha(0.5f);
            binding.btnCompartilhar.setEnabled(false);
            binding.btnCompartilhar.setAlpha(0.5f);
        } else {
            binding.btnInteresse.setEnabled(true);
            binding.btnInteresse.setAlpha(1.0f);
            binding.btnAgenda.setEnabled(true);
            binding.btnAgenda.setAlpha(1.0f);
            binding.btnCompartilhar.setEnabled(true);
            binding.btnCompartilhar.setAlpha(1.0f);
        }
    }

    private void carregarFotos() {
        RepositoryProvider.getPhotoRepository(this).getFotosByEvento(eventoId, new RepositoryCallback<List<EventPhoto>>() {
            @Override
            public void onSuccess(List<EventPhoto> fotos) {
                runOnUiThread(() -> {
                    photoAdapter.atualizar(fotos);
                    binding.tvSemFotos.setVisibility(fotos.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.tvTotalFotos.setText(fotos.size() + " fotos");
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(EventDetailActivity.this, "Erro ao carregar fotos", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fotos
    // ─────────────────────────────────────────────────────────────────────────

    private void verificarPermissao() {
        String permissao = android.os.Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permissao) == PackageManager.PERMISSION_GRANTED) {
            abrirGaleria();
        } else {
            pedirPermissao.launch(permissao);
        }
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        selecionarFoto.launch(intent);
    }

    private void mostrarDialogLegenda(Uri uri) {
        android.widget.EditText etLegenda = new android.widget.EditText(this);
        etLegenda.setHint("Adicione uma legenda (opcional)");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Salvar Foto")
                .setView(etLegenda)
                .setPositiveButton("Salvar", (d, w) -> {
                    String legenda = etLegenda.getText().toString().trim();
                    salvarFotoLocal(uri, legenda);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void salvarFotoLocal(Uri uri, String legenda) {
        if (!sessionManager.isLogado() || !sessionManager.isOrganizador()) {
            runOnUiThread(() -> Toast.makeText(this, "Apenas organizadores podem adicionar fotos", Toast.LENGTH_SHORT).show());
            return;
        }
        binding.progressUpload.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String fileName = "FOTO_" + timeStamp + ".jpg";
                File file = new File(getFilesDir(), fileName);

                try (InputStream is = getContentResolver().openInputStream(uri);
                     FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                }

                EventPhoto foto = new EventPhoto();
                foto.setEventoId(eventoId);
                foto.setUrlFoto(file.getAbsolutePath());
                foto.setLegenda(legenda);
                foto.setUsuarioNome(sessionManager.getNome());
                foto.setEnviadoEm(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));

                RepositoryProvider.getPhotoRepository(EventDetailActivity.this).insert(foto, new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        runOnUiThread(() -> {
                            binding.progressUpload.setVisibility(View.GONE);
                            Toast.makeText(EventDetailActivity.this, "Foto salva com sucesso!", Toast.LENGTH_SHORT).show();
                            carregarFotos();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            binding.progressUpload.setVisibility(View.GONE);
                            Toast.makeText(EventDetailActivity.this, "Erro ao salvar foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    binding.progressUpload.setVisibility(View.GONE);
                    Toast.makeText(EventDetailActivity.this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void confirmarDeletar(Long fotoId) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Remover Foto")
                .setMessage("Deseja remover esta foto permanentemente do dispositivo?")
                .setPositiveButton("Remover", (d, w) -> deletarFoto(fotoId))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deletarFoto(Long fotoId) {
        RepositoryProvider.getPhotoRepository(this).getFotoById(fotoId, new RepositoryCallback<EventPhoto>() {
            @Override
            public void onSuccess(EventPhoto foto) {
                if (foto != null) {
                    new Thread(() -> {
                        try {
                            if (foto.getUrlFoto() != null) {
                                File file = new File(foto.getUrlFoto());
                                if (file.exists()) file.delete();
                            }
                            RepositoryProvider.getPhotoRepository(EventDetailActivity.this).deleteById(fotoId, new RepositoryCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(EventDetailActivity.this, "Foto excluída!", Toast.LENGTH_SHORT).show();
                                        carregarFotos();
                                    });
                                }

                                @Override
                                public void onError(Exception e) {
                                    runOnUiThread(() -> Toast.makeText(EventDetailActivity.this, "Erro ao excluir foto: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                }
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(EventDetailActivity.this, "Erro ao excluir arquivo físico", Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(EventDetailActivity.this, "Erro ao buscar foto: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Excluir evento
    // ─────────────────────────────────────────────────────────────────────────

    private void confirmarExclusaoEvento() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Excluir Evento")
                .setMessage("Tem certeza? Isso apagará o evento e todas as suas fotos permanentemente.")
                .setPositiveButton("Excluir", (d, w) -> deletarEventoCompleto())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deletarEventoCompleto() {
        if (currentEvento == null || !sessionManager.isOrganizador() || currentEvento.criadoPor == null || !currentEvento.criadoPor.equals(sessionManager.getUserId())) {
            Toast.makeText(this, "Permissão negada: apenas o criador do evento pode excluí-lo.", Toast.LENGTH_SHORT).show();
            return;
        }
        RepositoryProvider.getPhotoRepository(this).getFotosByEvento(eventoId, new RepositoryCallback<List<EventPhoto>>() {
            @Override
            public void onSuccess(List<EventPhoto> fotos) {
                new Thread(() -> {
                    try {
                        for (EventPhoto f : fotos) {
                            if (f.getUrlFoto() != null) {
                                File file = new File(f.getUrlFoto());
                                if (file.exists()) file.delete();
                            }
                        }
                        
                        RepositoryProvider.getEventRepository(EventDetailActivity.this).deleteById(eventoId, new RepositoryCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                runOnUiThread(() -> {
                                    Toast.makeText(EventDetailActivity.this, "Evento removido com sucesso!", Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                            }

                            @Override
                            public void onError(Exception e) {
                                runOnUiThread(() -> Toast.makeText(EventDetailActivity.this, "Erro ao excluir evento: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(EventDetailActivity.this, "Erro ao excluir fotos locais", Toast.LENGTH_SHORT).show());
                    }
                }).start();
            }

            @Override
            public void onError(Exception e) {
                // Se falhar a busca por fotos (ex: sem conexão no Firebase), prossegue deletando o evento
                RepositoryProvider.getEventRepository(EventDetailActivity.this).deleteById(eventoId, new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        runOnUiThread(() -> {
                            Toast.makeText(EventDetailActivity.this, "Evento removido com sucesso!", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }

                    @Override
                    public void onError(Exception ex) {
                        runOnUiThread(() -> Toast.makeText(EventDetailActivity.this, "Erro ao excluir evento: " + ex.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                });
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilitários
    // ─────────────────────────────────────────────────────────────────────────

    private String formatarData(String data) {
        if (data == null || !data.contains("T")) return data;
        try {
            String[] parts = data.split("T");
            String[] dateParts = parts[0].split("-");
            String time = parts[1].substring(0, 5);
            return dateParts[2] + "/" + dateParts[1] + "/" + dateParts[0] + " às " + time;
        } catch (Exception e) {
            return data;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AÇÕES DO EVENTO (Interesse, Agenda, Chat, Compartilhar)
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isInterested = false;

    private void setupAcoes() {
        if (!sessionManager.isLogado()) {
            binding.layoutAcoes.setVisibility(View.GONE);
            return;
        }

        checkInterest();

        binding.btnInteresse.setOnClickListener(v -> toggleInteresse());
        binding.btnAgenda.setOnClickListener(v -> adicionarAAgenda());
        binding.btnChat.setOnClickListener(v -> abrirChatComOrganizador());
        binding.btnCompartilhar.setOnClickListener(v -> compartilharEvento());
    }

    private void checkInterest() {
        RepositoryProvider.getInterestRepository(this).getInteresse(sessionManager.getUserId(), eventoId, new RepositoryCallback<Interesse>() {
            @Override
            public void onSuccess(Interesse interesse) {
                isInterested = (interesse != null);
                runOnUiThread(() -> {
                    if (isInterested) {
                        binding.imgInteresse.setImageResource(com.casaempresario.app.R.drawable.ic_heart_filled);
                        binding.tvInteresse.setText("Gostei!");
                        binding.tvInteresse.setTextColor(0xFFE91E63);
                    } else {
                        binding.imgInteresse.setImageResource(com.casaempresario.app.R.drawable.ic_heart_outline);
                        binding.tvInteresse.setText("Interesse");
                        binding.tvInteresse.setTextColor(0xFF555555);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                // Silencioso
            }
        });
    }

    private void toggleInteresse() {
        if (currentEvento != null && ("CONCLUIDO".equals(currentEvento.status) || "CANCELADO".equals(currentEvento.status))) {
            Toast.makeText(this, "Ação não permitida: evento finalizado.", Toast.LENGTH_SHORT).show();
            return;
        }
        long userId = sessionManager.getUserId();
        if (isInterested) {
            RepositoryProvider.getInterestRepository(this).deleteByUsuarioAndEvento(userId, eventoId, new RepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    runOnUiThread(() -> {
                        isInterested = false;
                        binding.imgInteresse.setImageResource(com.casaempresario.app.R.drawable.ic_heart_outline);
                        binding.tvInteresse.setText("Interesse");
                        binding.tvInteresse.setTextColor(0xFF555555);
                        Toast.makeText(EventDetailActivity.this, "Interesse removido", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> Toast.makeText(EventDetailActivity.this, "Erro ao remover interesse", Toast.LENGTH_SHORT).show());
                }
            });
        } else {
            Interesse interesse = new Interesse();
            interesse.usuarioId = userId;
            interesse.eventoId = eventoId;
            interesse.criadoEm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            RepositoryProvider.getInterestRepository(this).insert(interesse, new RepositoryCallback<Long>() {
                @Override
                public void onSuccess(Long newId) {
                    runOnUiThread(() -> {
                        isInterested = true;
                        binding.imgInteresse.setImageResource(com.casaempresario.app.R.drawable.ic_heart_filled);
                        binding.tvInteresse.setText("Gostei!");
                        binding.tvInteresse.setTextColor(0xFFE91E63);
                        Toast.makeText(EventDetailActivity.this, "Interesse marcado! ❤️", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> Toast.makeText(EventDetailActivity.this, "Erro ao marcar interesse", Toast.LENGTH_SHORT).show());
                }
            });
        }
    }

    private void adicionarAAgenda() {
        if (currentEvento == null) return;
        if ("CONCLUIDO".equals(currentEvento.status) || "CANCELADO".equals(currentEvento.status)) {
            Toast.makeText(this, "Ação não permitida: evento finalizado.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            long startTimeMillis = System.currentTimeMillis();
            if (currentEvento.dataEvento != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault());
                Date d = sdf.parse(currentEvento.dataEvento);
                if (d != null) {
                    startTimeMillis = d.getTime();
                }
            }

            Intent intent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, currentEvento.titulo)
                    .putExtra(CalendarContract.Events.DESCRIPTION, currentEvento.descricao)
                    .putExtra(CalendarContract.Events.EVENT_LOCATION, currentEvento.local)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTimeMillis)
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startTimeMillis + (60 * 60 * 1000));

            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao abrir agenda nativa", Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirChatComOrganizador() {
        if (currentEvento == null || currentEvento.criadoPor == null) return;
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("eventoId", eventoId);
        intent.putExtra("outroUserId", currentEvento.criadoPor);
        startActivity(intent);
    }

    private void compartilharEvento() {
        if (currentEvento == null) return;
        if ("CONCLUIDO".equals(currentEvento.status) || "CANCELADO".equals(currentEvento.status)) {
            Toast.makeText(this, "Ação não permitida: evento finalizado.", Toast.LENGTH_SHORT).show();
            return;
        }
        String texto = "Olha só este evento incrível na Casa do Empresário!\n\n" +
                "🏆 *" + currentEvento.titulo + "*\n" +
                "📅 Data: " + formatarData(currentEvento.dataEvento) + "\n" +
                "📍 Local: " + currentEvento.local + "\n\n" +
                (currentEvento.descricao != null ? currentEvento.descricao : "");

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, currentEvento.titulo);
        intent.putExtra(Intent.EXTRA_TEXT, texto);
        startActivity(Intent.createChooser(intent, "Compartilhar Evento"));
    }
}