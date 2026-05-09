package com.casaempresario.app.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.casaempresario.app.database.AppDatabase;
import com.casaempresario.app.database.Evento;
import com.casaempresario.app.databinding.ActivityCreateEventBinding;
import com.casaempresario.app.util.SessionManager;

import java.util.Calendar;

public class CreateEventActivity extends AppCompatActivity {

    private ActivityCreateEventBinding binding;
    private SessionManager sessionManager;

    private Long eventoId;

    private Calendar dataSelecionada = Calendar.getInstance();

    // Banner
    private static final int PICK_BANNER = 1001;
    private String bannerUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCreateEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        // Recupera ID do evento
        long idParam = getIntent().getLongExtra("eventoId", -1);
        eventoId = (idParam == -1) ? null : idParam;

        // Toolbar
        setSupportActionBar(binding.toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            getSupportActionBar().setTitle(
                    eventoId != null
                            ? "Editar Evento"
                            : "Novo Evento"
            );
        }

        // Eventos
        binding.btnSalvar.setOnClickListener(v -> salvar());

        binding.etData.setOnClickListener(v -> selecionarData());

        binding.etData.setFocusable(false);

        // Banner
        binding.btnSelecionarBanner.setOnClickListener(
                v -> selecionarBanner()
        );

        // Carrega evento para edição
        if (eventoId != null) {
            carregarEventoDoBanco();
        }
    }

    private void selecionarBanner() {

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");

        startActivityForResult(intent, PICK_BANNER);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_BANNER
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {

            try {

                Uri uri = data.getData();

                String fileName =
                        "banner_" + System.currentTimeMillis() + ".jpg";

                File file =
                        new File(getFilesDir(), fileName);

                InputStream inputStream =
                        getContentResolver().openInputStream(uri);

                FileOutputStream outputStream =
                        new FileOutputStream(file);

                byte[] buffer = new byte[1024];

                int length;

                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                outputStream.close();
                inputStream.close();

                // salva caminho REAL
                bannerUri = file.getAbsolutePath();

                Glide.with(this)
                        .load(file)
                        .into(binding.imgBanner);

            } catch (Exception e) {

                Toast.makeText(
                        this,
                        "Erro ao carregar banner",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private void selecionarData() {

        DatePickerDialog dateDialog = new DatePickerDialog(
                this,
                (view, year, month, day) -> {

                    dataSelecionada.set(Calendar.YEAR, year);
                    dataSelecionada.set(Calendar.MONTH, month);
                    dataSelecionada.set(Calendar.DAY_OF_MONTH, day);

                    new TimePickerDialog(
                            this,
                            (tv, hour, min) -> {

                                dataSelecionada.set(
                                        Calendar.HOUR_OF_DAY,
                                        hour
                                );

                                dataSelecionada.set(
                                        Calendar.MINUTE,
                                        min
                                );

                                String texto = String.format(
                                        "%02d/%02d/%04d às %02d:%02d",
                                        day,
                                        month + 1,
                                        year,
                                        hour,
                                        min
                                );

                                binding.etData.setText(texto);

                            },
                            dataSelecionada.get(Calendar.HOUR_OF_DAY),
                            dataSelecionada.get(Calendar.MINUTE),
                            true
                    ).show();

                },
                dataSelecionada.get(Calendar.YEAR),
                dataSelecionada.get(Calendar.MONTH),
                dataSelecionada.get(Calendar.DAY_OF_MONTH)
        );

        dateDialog.getDatePicker()
                .setMinDate(System.currentTimeMillis());

        dateDialog.show();
    }

    private void salvar() {

        String titulo =
                binding.etTitulo.getText().toString().trim();

        String descricao =
                binding.etDescricao.getText().toString().trim();

        String local =
                binding.etLocal.getText().toString().trim();

        String capStr =
                binding.etCapacidade.getText().toString().trim();

        if (titulo.isEmpty()
                || local.isEmpty()
                || binding.etData.getText().toString().isEmpty()) {

            Toast.makeText(
                    this,
                    "Preencha título, local e data",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Integer capacidade =
                capStr.isEmpty()
                        ? null
                        : Integer.parseInt(capStr);

        String dataISO = String.format(
                "%04d-%02d-%02dT%02d:%02d:00",
                dataSelecionada.get(Calendar.YEAR),
                dataSelecionada.get(Calendar.MONTH) + 1,
                dataSelecionada.get(Calendar.DAY_OF_MONTH),
                dataSelecionada.get(Calendar.HOUR_OF_DAY),
                dataSelecionada.get(Calendar.MINUTE)
        );

        setLoading(true);

        new Thread(() -> {

            try {

                AppDatabase db =
                        AppDatabase.getDatabase(this);

                Evento evento = new Evento();

                if (eventoId != null) {

                    Evento original =
                            db.eventoDao().getEventoById(eventoId);

                    evento.id = eventoId;

                    evento.status =
                            original != null
                                    ? original.status
                                    : "AGENDADO";

                    evento.criadoPor =
                            original != null
                                    ? original.criadoPor
                                    : sessionManager.getUserId();

                } else {

                    evento.status = "AGENDADO";

                    evento.criadoPor =
                            sessionManager.getUserId();
                }

                evento.titulo = titulo;
                evento.descricao = descricao;
                evento.dataEvento = dataISO;
                evento.local = local;
                evento.capacidadeMaxima = capacidade;

                // Banner
                evento.bannerUri = bannerUri;

                if (eventoId == null) {
                    db.eventoDao().insert(evento);
                } else {
                    db.eventoDao().update(evento);
                }

                runOnUiThread(() -> {

                    setLoading(false);

                    Toast.makeText(
                            this,
                            "✅ Evento salvo com sucesso!",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    setLoading(false);

                    Toast.makeText(
                            this,
                            "❌ Erro: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }

        }).start();
    }

    private void carregarEventoDoBanco() {

        try {

            AppDatabase db =
                    AppDatabase.getDatabase(this);

            Evento e =
                    db.eventoDao().getEventoById(eventoId);

            if (e != null) {

                binding.etTitulo.setText(e.titulo);

                binding.etDescricao.setText(e.descricao);

                binding.etLocal.setText(e.local);

                if (e.capacidadeMaxima != null) {

                    binding.etCapacidade.setText(
                            String.valueOf(e.capacidadeMaxima)
                    );
                }

                if (e.dataEvento != null
                        && e.dataEvento.contains("T")) {

                    String dataFormatada =
                            e.dataEvento
                                    .replace("T", " às ")
                                    .substring(0, 16);

                    binding.etData.setText(dataFormatada);
                }

                // Banner
                bannerUri = e.bannerUri;

                if (bannerUri != null
                        && !bannerUri.isEmpty()) {

                    Glide.with(this)
                            .load(Uri.parse(bannerUri))
                            .into(binding.imgBanner);
                }

                binding.btnSalvar.setText("ATUALIZAR EVENTO");
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erro ao carregar dados",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void setLoading(boolean loading) {

        binding.btnSalvar.setEnabled(!loading);

        binding.progressBar.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    @Override
    public boolean onSupportNavigateUp() {

        onBackPressed();

        return true;
    }
}