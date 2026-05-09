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
import com.casaempresario.app.database.AppDatabase;
import com.casaempresario.app.database.Evento;
import com.casaempresario.app.databinding.ActivityEventDetailBinding;
import com.casaempresario.app.model.EventPhoto;
import com.casaempresario.app.util.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
    private AppDatabase db;

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

        db = AppDatabase.getDatabase(this);
        sessionManager = new SessionManager(this);
        eventoId = getIntent().getLongExtra("eventoId", -1);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupFotos();
        carregarEvento();
        carregarFotos();

        if (sessionManager.isAdmin()) {
            // Botão editar evento
            binding.btnEditar.setVisibility(View.VISIBLE);
            binding.btnEditar.setOnClickListener(v -> {
                Intent intent = new Intent(this, CreateEventActivity.class);
                intent.putExtra("eventoId", eventoId);
                editarEventoLauncher.launch(intent);
            });

            // Botão alterar status
            binding.tvStatus.setVisibility(View.VISIBLE);
            binding.tvStatus.setOnClickListener(v -> mostrarDialogStatus());

            // Botão excluir evento
            binding.btnExcluir.setVisibility(View.VISIBLE);
            binding.btnExcluir.setOnClickListener(v -> confirmarExclusaoEvento());
        }

        // Qualquer usuário logado pode adicionar fotos
        if (sessionManager.isLogado()) {
            binding.fabAdicionarFoto.setVisibility(View.VISIBLE);
            binding.fabAdicionarFoto.setOnClickListener(v -> verificarPermissao());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Alteração de status
    // ─────────────────────────────────────────────────────────────────────────

    private void mostrarDialogStatus() {
        // Descobre qual é o status atual para marcá-lo no dialog
        Evento eventoAtual = db.eventoDao().getEventoById(eventoId);
        String statusAtual = (eventoAtual != null && eventoAtual.status != null)
                ? eventoAtual.status : "AGENDADO";

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
        new Thread(() -> {
            try {
                db.eventoDao().updateStatus(eventoId, novoStatus);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Status atualizado!", Toast.LENGTH_SHORT).show();
                    carregarEvento(); // Atualiza a tela com o novo status
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Erro ao atualizar status", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Carregar dados do evento
    // ─────────────────────────────────────────────────────────────────────────

    private void setupFotos() {
        photoAdapter = new PhotoAdapter(new ArrayList<>(), sessionManager, this::confirmarDeletar);
        binding.recyclerFotos.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerFotos.setAdapter(photoAdapter);
    }

    private void carregarEvento() {
        Evento eventoDb = db.eventoDao().getEventoById(eventoId);
        if (eventoDb != null) {
            preencherDados(eventoDb);
        }
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

        // ─────────────────────────────────────
        // CARREGAR BANNER
        // ─────────────────────────────────────

        if (evento.bannerUri != null
                && !evento.bannerUri.isEmpty()) {

            Glide.with(this)
                    .load(new File(evento.bannerUri))
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
    }

    private void carregarFotos() {
        List<EventPhoto> fotos = db.fotoDao().getFotosByEvento(eventoId);
        photoAdapter.atualizar(fotos);
        binding.tvSemFotos.setVisibility(fotos.isEmpty() ? View.VISIBLE : View.GONE);
        binding.tvTotalFotos.setText(fotos.size() + " fotos");
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
                foto.setEnviadoEm(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));

                db.fotoDao().insert(foto);

                runOnUiThread(() -> {
                    binding.progressUpload.setVisibility(View.GONE);
                    Toast.makeText(this, "Foto salva com sucesso!", Toast.LENGTH_SHORT).show();
                    carregarFotos();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    binding.progressUpload.setVisibility(View.GONE);
                    Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
        new Thread(() -> {
            try {
                EventPhoto foto = db.fotoDao().getFotoById(fotoId);
                if (foto != null) {
                    if (foto.getUrlFoto() != null) {
                        File file = new File(foto.getUrlFoto());
                        if (file.exists()) file.delete();
                    }
                    db.fotoDao().deleteById(fotoId);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Foto excluída!", Toast.LENGTH_SHORT).show();
                        carregarFotos();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
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
        new Thread(() -> {
            try {
                // Apaga os arquivos físicos das fotos antes de remover do banco
                List<EventPhoto> fotos = db.fotoDao().getFotosByEvento(eventoId);
                for (EventPhoto f : fotos) {
                    if (f.getUrlFoto() != null) {
                        File file = new File(f.getUrlFoto());
                        if (file.exists()) file.delete();
                    }
                }
                // O CASCADE do Room remove as fotos do banco junto com o evento
                db.eventoDao().deleteById(eventoId);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Evento removido com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Erro ao excluir evento", Toast.LENGTH_SHORT).show());
            }
        }).start();
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
}