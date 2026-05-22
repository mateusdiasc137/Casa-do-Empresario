package com.casaempresario.app.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.casaempresario.app.R;
import com.casaempresario.app.databinding.ActivityPhotoPostBinding;
import com.casaempresario.app.model.EventPhoto;
import com.casaempresario.app.repository.RepositoryCallback;
import com.casaempresario.app.repository.RepositoryProvider;
import com.casaempresario.app.util.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

public class PhotoPostActivity extends AppCompatActivity {

    private ActivityPhotoPostBinding binding;
    private SessionManager sessionManager;
    private Long fotoId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhotoPostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        fotoId = getIntent().getLongExtra("fotoId", -1);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        carregarDetalhesFoto();
    }

    private void carregarDetalhesFoto() {
        if (fotoId == -1) {
            Toast.makeText(this, "Foto inválida", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        RepositoryProvider.getPhotoRepository(this).getFotoById(fotoId, new RepositoryCallback<EventPhoto>() {
            @Override
            public void onSuccess(EventPhoto foto) {
                runOnUiThread(() -> {
                    if (foto != null) {
                        preencherDados(foto);
                    } else {
                        Toast.makeText(PhotoPostActivity.this, "Foto não encontrada", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(PhotoPostActivity.this, "Erro ao carregar detalhes", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void preencherDados(EventPhoto foto) {
        String autor = foto.getUsuarioNome() != null && !foto.getUsuarioNome().isEmpty()
                ? foto.getUsuarioNome()
                : "Organizador";

        binding.tvAutorNome.setText(autor);
        binding.tvAvatarIniciais.setText(autor.substring(0, 1).toUpperCase());
        binding.tvPostData.setText("Publicado em " + (foto.getEnviadoEm() != null ? foto.getEnviadoEm() : "Data indisponível"));

        if (foto.getLegenda() != null && !foto.getLegenda().trim().isEmpty()) {
            binding.tvLegendaPost.setText(foto.getLegenda());
            binding.tvLegendaTitulo.setVisibility(View.VISIBLE);
        } else {
            binding.tvLegendaPost.setText("Sem legenda disponível.");
            binding.tvLegendaTitulo.setVisibility(View.GONE);
        }

        Glide.with(this)
                .load(foto.getUrlFoto())
                .placeholder(R.drawable.ic_photo_placeholder)
                .into(binding.imgFotoPost);

        // Somente organizadores podem excluir a foto
        boolean podeDeletar = sessionManager.isLogado() && sessionManager.isOrganizador();
        binding.btnExcluirPost.setVisibility(podeDeletar ? View.VISIBLE : View.GONE);
        binding.btnExcluirPost.setOnClickListener(v -> confirmarExclusao(foto));
    }

    private void confirmarExclusao(EventPhoto foto) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Remover Publicação")
                .setMessage("Deseja remover esta publicação permanentemente?")
                .setPositiveButton("Remover", (d, w) -> deletarFoto(foto))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deletarFoto(EventPhoto foto) {
        new Thread(() -> {
            try {
                if (foto.getUrlFoto() != null) {
                    File file = new File(foto.getUrlFoto());
                    if (file.exists()) file.delete();
                }

                RepositoryProvider.getPhotoRepository(PhotoPostActivity.this).deleteById(foto.getId(), new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        runOnUiThread(() -> {
                            Toast.makeText(PhotoPostActivity.this, "Publicação removida!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK); // Sinaliza para atualizar a lista
                            finish();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> Toast.makeText(PhotoPostActivity.this, "Erro ao remover publicação: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(PhotoPostActivity.this, "Erro ao excluir arquivo físico", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
