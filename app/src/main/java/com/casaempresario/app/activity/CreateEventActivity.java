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
import com.casaempresario.app.database.Evento;
import com.casaempresario.app.databinding.ActivityCreateEventBinding;
import com.casaempresario.app.repository.RepositoryCallback;
import com.casaempresario.app.repository.RepositoryProvider;
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

    // Seletor de Localização
    private static final int PICK_LOCATION = 1002;
    private Double selectedLatitude = null;
    private Double selectedLongitude = null;
    private boolean isProgrammaticChange = false;

    public static final String[] CATEGORIAS = {
            "Tecnologia",
            "Networking",
            "Workshop",
            "Palestra",
            "Congresso",
            "Outros"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCreateEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        if (!sessionManager.isOrganizador()) {
            Toast.makeText(this, "Acesso negado. Apenas organizadores podem gerenciar eventos.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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

        // Inicializa Dropdown de Categorias
        android.widget.ArrayAdapter<String> adapterCategorias = new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                CATEGORIAS
        );
        binding.etCategoria.setAdapter(adapterCategorias);

        // Eventos
        binding.btnSalvar.setOnClickListener(v -> salvar());

        binding.etData.setOnClickListener(v -> selecionarData());

        binding.etData.setFocusable(false);

        // Seletor de Localização
        binding.layoutLocal.setEndIconOnClickListener(v -> {
            Intent intent = new Intent(CreateEventActivity.this, LocationPickerActivity.class);
            String currentLocal = binding.etLocal.getText().toString().trim();
            if (!currentLocal.isEmpty()) {
                intent.putExtra("currentAddress", currentLocal);
            }
            if (selectedLatitude != null && selectedLongitude != null) {
                intent.putExtra("currentLatitude", selectedLatitude);
                intent.putExtra("currentLongitude", selectedLongitude);
            }
            startActivityForResult(intent, PICK_LOCATION);
        });

        binding.etLocal.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isProgrammaticChange) {
                    selectedLatitude = null;
                    selectedLongitude = null;
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Banner
        binding.btnSelecionarBanner.setOnClickListener(
                v -> selecionarBanner()
        );

        // Carrega evento para edição
        if (eventoId != null) {
            RepositoryProvider.getEventRepository(this).getEventoById(eventoId, new RepositoryCallback<Evento>() {
                @Override
                public void onSuccess(Evento e) {
                    if (e != null) {
                        boolean isCreator = sessionManager.isOrganizador() && e.criadoPor != null && e.criadoPor.equals(sessionManager.getUserId());
                        if (!isCreator) {
                            runOnUiThread(() -> {
                                Toast.makeText(CreateEventActivity.this, "Permissão negada: apenas o criador do evento pode editá-lo.", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                            return;
                        }
                        runOnUiThread(() -> carregarEventoDoBanco(e));
                    }
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(CreateEventActivity.this, "Erro ao validar evento", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            });
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

        if (requestCode == PICK_LOCATION && resultCode == RESULT_OK && data != null) {
             String address = data.getStringExtra("selectedAddress");
             double lat = data.getDoubleExtra("selectedLatitude", 0.0);
             double lng = data.getDoubleExtra("selectedLongitude", 0.0);

             if (address != null) {
                 isProgrammaticChange = true;
                 binding.etLocal.setText(address);
                 selectedLatitude = lat;
                 selectedLongitude = lng;
                 isProgrammaticChange = false;
                 Toast.makeText(this, "Local selecionado via mapa!", Toast.LENGTH_SHORT).show();
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

        String categoria =
                binding.etCategoria.getText().toString().trim();

        if (titulo.isEmpty()
                || local.isEmpty()
                || binding.etData.getText().toString().isEmpty()
                || categoria.isEmpty()) {

            Toast.makeText(
                    this,
                    "Preencha título, local, categoria e data",
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
                // Validação de endereço com Geocoder no background thread
                double latVal = 0.0;
                double lngVal = 0.0;
                if (selectedLatitude != null && selectedLongitude != null) {
                    latVal = selectedLatitude;
                    lngVal = selectedLongitude;
                } else {
                    try {
                        android.location.Geocoder geocoder = new android.location.Geocoder(CreateEventActivity.this, java.util.Locale.getDefault());
                        java.util.List<android.location.Address> addresses = geocoder.getFromLocationName(local, 1);
                        if (addresses == null || addresses.isEmpty()) {
                            runOnUiThread(() -> {
                                setLoading(false);
                                Toast.makeText(CreateEventActivity.this, "❌ Endereço não encontrado. Por favor, digite um endereço real válido.", Toast.LENGTH_LONG).show();
                            });
                            return;
                        }
                        android.location.Address address = addresses.get(0);
                        latVal = address.getLatitude();
                        lngVal = address.getLongitude();
                    } catch (Exception ex) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(CreateEventActivity.this, "⚠️ Não foi possível validar o endereço (verifique sua internet e se o endereço é real).", Toast.LENGTH_LONG).show();
                        });
                        return;
                    }
                }

                double finalLat = latVal;
                double finalLng = lngVal;

                if (eventoId != null) {
                    RepositoryProvider.getEventRepository(this).getEventoById(eventoId, new RepositoryCallback<Evento>() {
                        @Override
                        public void onSuccess(Evento original) {
                            Evento evento = new Evento();
                            evento.id = eventoId;
                            evento.status = (original != null) ? original.status : "AGENDADO";
                            evento.criadoPor = (original != null) ? original.criadoPor : sessionManager.getUserId();

                            evento.titulo = titulo;
                            evento.descricao = descricao;
                            evento.dataEvento = dataISO;
                            evento.local = local;
                            evento.capacidadeMaxima = capacidade;
                            evento.categoria = categoria;
                            evento.latitude = finalLat;
                            evento.longitude = finalLng;
                            evento.bannerUri = bannerUri;

                            RepositoryProvider.getEventRepository(CreateEventActivity.this).update(evento, new RepositoryCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    runOnUiThread(() -> {
                                        setLoading(false);
                                        Toast.makeText(CreateEventActivity.this, "✅ Evento atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                                        finish();
                                    });
                                }

                                @Override
                                public void onError(Exception e) {
                                    runOnUiThread(() -> {
                                        setLoading(false);
                                        Toast.makeText(CreateEventActivity.this, "❌ Erro ao atualizar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                                }
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() -> {
                                setLoading(false);
                                Toast.makeText(CreateEventActivity.this, "❌ Erro ao carregar evento original", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                } else {
                    Evento evento = new Evento();
                    evento.status = "AGENDADO";
                    evento.criadoPor = sessionManager.getUserId();
                    evento.titulo = titulo;
                    evento.descricao = descricao;
                    evento.dataEvento = dataISO;
                    evento.local = local;
                    evento.capacidadeMaxima = capacidade;
                    evento.categoria = categoria;
                    evento.latitude = finalLat;
                    evento.longitude = finalLng;
                    evento.bannerUri = bannerUri;

                    RepositoryProvider.getEventRepository(this).insert(evento, new RepositoryCallback<Long>() {
                        @Override
                        public void onSuccess(Long newId) {
                            runOnUiThread(() -> {
                                setLoading(false);
                                Toast.makeText(CreateEventActivity.this, "✅ Evento salvo com sucesso!", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() -> {
                                setLoading(false);
                                Toast.makeText(CreateEventActivity.this, "❌ Erro ao salvar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                }

            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(CreateEventActivity.this, "❌ Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

        }).start();
    }

    private void carregarEventoDoBanco(Evento e) {
        if (e != null) {

            binding.etTitulo.setText(e.titulo);

            binding.etDescricao.setText(e.descricao);

            isProgrammaticChange = true;
            binding.etLocal.setText(e.local);
            if (e.latitude != 0.0 || e.longitude != 0.0) {
                selectedLatitude = e.latitude;
                selectedLongitude = e.longitude;
            }
            isProgrammaticChange = false;

            if (e.categoria != null) {
                binding.etCategoria.setText(e.categoria, false);
            }

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
                        .load(bannerUri)
                        .into(binding.imgBanner);
            }

            binding.btnSalvar.setText("ATUALIZAR EVENTO");
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