package com.casaempresario.app.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;

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
            binding.btnEditar.setVisibility(View.VISIBLE);
            binding.btnEditar.setOnClickListener(v -> {
                Intent intent = new Intent(this, CreateEventActivity.class);
                intent.putExtra("eventoId", eventoId);
                editarEventoLauncher.launch(intent);
            });

            // ✅ Lógica para o botão excluir evento (certifique-se de que o ID existe no XML)
            binding.btnExcluir.setVisibility(View.VISIBLE);
            binding.btnExcluir.setOnClickListener(v -> confirmarExclusaoEvento());
        }

        if (sessionManager.isLogado()) {
            binding.fabAdicionarFoto.setVisibility(View.VISIBLE);
            binding.fabAdicionarFoto.setOnClickListener(v -> verificarPermissao());
        }
    }

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
        binding.tvLocal.setText("📍 " + evento.local);
        binding.tvData.setText("📅 " + formatarData(evento.dataEvento));
        binding.tvDescricao.setText(evento.descricao != null ? evento.descricao : "Sem descrição");
        binding.tvStatus.setText(evento.status);

        if (evento.capacidadeMaxima != null) {
            binding.tvCapacidade.setText("👥 Capacidade: " + evento.capacidadeMaxima);
        }
    }

    private void carregarFotos() {
        List<EventPhoto> fotos = db.fotoDao().getFotosByEvento(eventoId);
        photoAdapter.atualizar(fotos);
        binding.tvSemFotos.setVisibility(fotos.isEmpty() ? View.VISIBLE : View.GONE);
        binding.tvTotalFotos.setText(fotos.size() + " fotos");
    }

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
        etLegenda.setHint("Adicione uma legenda");

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
                .setPositiveButton("Remover", (d, w) -> {
                    deletarFoto(fotoId);
                })
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
                runOnUiThread(() -> Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ✅ NOVO: Confirmar exclusão do evento completo
    private void confirmarExclusaoEvento() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Excluir Evento")
                .setMessage("Tem certeza? Isso apagará o evento e todas as suas fotos permanentemente.")
                .setPositiveButton("Excluir", (d, w) -> deletarEventoCompleto())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ✅ NOVO: Lógica de exclusão do evento + limpeza de arquivos
    private void deletarEventoCompleto() {
        new Thread(() -> {
            try {
                // 1. Deletar arquivos físicos de todas as fotos deste evento
                List<EventPhoto> fotos = db.fotoDao().getFotosByEvento(eventoId);
                for (EventPhoto f : fotos) {
                    if (f.getUrlFoto() != null) {
                        File file = new File(f.getUrlFoto());
                        if (file.exists()) file.delete();
                    }
                }
                // 2. Deletar o evento (O CASCADE no banco cuidará das fotos no SQLite)
                db.eventoDao().deleteById(eventoId);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Evento removido com sucesso!", Toast.LENGTH_SHORT).show();
                    finish(); // Volta para a MainActivity
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Erro ao excluir evento", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private String formatarData(String data) {
        if (data == null || !data.contains("T")) return data;
        try {
            String[] parts = data.split("T");
            String[] dateParts = parts[0].split("-");
            String time = parts[1].substring(0, 5);
            return dateParts[2] + "/" + dateParts[1] + "/" + dateParts[0] + " às " + time;
        } catch (Exception e) { return data; }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}