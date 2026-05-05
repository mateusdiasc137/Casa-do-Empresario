package com.casaempresario.app.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.casaempresario.app.databinding.ActivityCreateEventBinding;
import com.casaempresario.app.util.SessionManager;

// ✅ IMPORTS DO BANCO LOCAL
import com.casaempresario.app.database.Evento;
import com.casaempresario.app.database.AppDatabase;

import java.util.Calendar;

public class CreateEventActivity extends AppCompatActivity {

    private ActivityCreateEventBinding binding;
    private SessionManager sessionManager;
    private Long eventoId;
    private Calendar dataSelecionada = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        // Recupera o ID. Se for -1, significa que é um NOVO evento.
        long idParam = getIntent().getLongExtra("eventoId", -1);
        eventoId = (idParam == -1) ? null : idParam;

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(eventoId != null ? "Editar Evento" : "Novo Evento");
        }

        binding.btnSalvar.setOnClickListener(v -> salvar());
        binding.etData.setOnClickListener(v -> selecionarData());
        binding.etData.setFocusable(false);

        // ✅ Se estiver editando, carrega os dados do SQLite
        if (eventoId != null) {
            carregarEventoDoBanco();
        }
    }

    private void selecionarData() {
        DatePickerDialog dateDialog = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    dataSelecionada.set(Calendar.YEAR, year);
                    dataSelecionada.set(Calendar.MONTH, month);
                    dataSelecionada.set(Calendar.DAY_OF_MONTH, day);

                    new TimePickerDialog(this, (tv, hour, min) -> {
                        dataSelecionada.set(Calendar.HOUR_OF_DAY, hour);
                        dataSelecionada.set(Calendar.MINUTE, min);

                        String texto = String.format("%02d/%02d/%04d às %02d:%02d",
                                day, month + 1, year, hour, min);
                        binding.etData.setText(texto);
                    }, dataSelecionada.get(Calendar.HOUR_OF_DAY),
                            dataSelecionada.get(Calendar.MINUTE), true).show();

                }, dataSelecionada.get(Calendar.YEAR),
                dataSelecionada.get(Calendar.MONTH),
                dataSelecionada.get(Calendar.DAY_OF_MONTH));

        dateDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dateDialog.show();
    }

    private void salvar() {
        String titulo = binding.etTitulo.getText().toString().trim();
        String descricao = binding.etDescricao.getText().toString().trim();
        String local = binding.etLocal.getText().toString().trim();
        String capStr = binding.etCapacidade.getText().toString().trim();

        if (titulo.isEmpty() || local.isEmpty() || binding.etData.getText().toString().isEmpty()) {
            Toast.makeText(this, "Preencha título, local e data", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer capacidade = capStr.isEmpty() ? null : Integer.parseInt(capStr);

        String dataISO = String.format("%04d-%02d-%02dT%02d:%02d:00",
                dataSelecionada.get(Calendar.YEAR),
                dataSelecionada.get(Calendar.MONTH) + 1,
                dataSelecionada.get(Calendar.DAY_OF_MONTH),
                dataSelecionada.get(Calendar.HOUR_OF_DAY),
                dataSelecionada.get(Calendar.MINUTE));

        setLoading(true);

        // ✅ Executa em background para não travar o app
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(this);
                Evento evento = new Evento();

                if (eventoId != null) {
                    evento.id = eventoId;
                }

                evento.titulo = titulo;
                evento.descricao = descricao;
                evento.dataEvento = dataISO;
                evento.local = local;
                evento.capacidadeMaxima = capacidade;
                evento.status = "AGENDADO";
                evento.criadoPor = 1L;

                if (eventoId == null) {
                    db.eventoDao().insert(evento);
                } else {
                    db.eventoDao().update(evento);
                }

                // Volta para a Thread principal para avisar o usuário e fechar
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "✅ Sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "❌ Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void carregarEventoDoBanco() {
        try {
            AppDatabase db = AppDatabase.getDatabase(this);
            Evento e = db.eventoDao().getEventoById(eventoId);

            if (e != null) {
                binding.etTitulo.setText(e.titulo);
                binding.etDescricao.setText(e.descricao);
                binding.etLocal.setText(e.local);
                if (e.capacidadeMaxima != null) {
                    binding.etCapacidade.setText(String.valueOf(e.capacidadeMaxima));
                }

                // Trata a exibição da data (YYYY-MM-DDTHH:MM:SS -> DD/MM/YYYY às HH:MM)
                if (e.dataEvento != null && e.dataEvento.contains("T")) {
                    String dataFormatada = e.dataEvento.replace("T", " às ").substring(0, 16);
                    binding.etData.setText(dataFormatada);
                }

                binding.btnSalvar.setText("ATUALIZAR EVENTO");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao carregar dados", Toast.LENGTH_SHORT).show();
        }
    }

    private void setLoading(boolean loading) {
        binding.btnSalvar.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}