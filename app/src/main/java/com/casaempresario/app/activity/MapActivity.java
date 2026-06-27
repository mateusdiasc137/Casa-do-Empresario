package com.casaempresario.app.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.content.IntentSender;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.casaempresario.app.R;
import com.casaempresario.app.database.Evento;
import com.casaempresario.app.databinding.ActivityMapBinding;
import com.casaempresario.app.repository.RepositoryCallback;
import com.casaempresario.app.repository.RepositoryProvider;
import com.casaempresario.app.util.SessionManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityMapBinding binding;
    private SessionManager sessionManager;
    private GoogleMap mMap;
    private static final int REQUEST_LOCATION_MAP = 4201;
    private static final int REQUEST_CHECK_SETTINGS = 4202;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.getRoot().setAlpha(0f);
        binding.getRoot().setTranslationY(18f);
        binding.getRoot().animate().alpha(1f).translationY(0f).setDuration(300).start();

        sessionManager = new SessionManager(this);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mapa de Interesses");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Em vez de carregar o mapa imediatamente, iniciamos o fluxo de localização
        iniciarFluxoDeLocalizacao();
    }

    private void iniciarFluxoDeLocalizacao() {
        boolean granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (granted) {
            verificarAtivacaoGPS();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_MAP
            );
        }
    }

    private void carregarMapa() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        binding.progressBar.setVisibility(View.GONE);

        // Habilita controles padrão da interface do mapa do Google
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);

        // Configura listener de clique nativo nos marcadores
        mMap.setOnMarkerClickListener(marker -> {
            Long eventId = (Long) marker.getTag();
            if (eventId != null) {
                Intent intent = new Intent(MapActivity.this, EventDetailActivity.class);
                intent.putExtra("eventoId", eventId);
                startActivity(intent);
            }
            return false;
        });

        habilitarMinhaLocalizacao();
        carregarMarcadores();
    }

    private void habilitarMinhaLocalizacao() {
        if (mMap == null) return;

        boolean granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (granted) {
            try {
                mMap.setMyLocationEnabled(true);
                centralizarUsuarioSePossivel();
            } catch (SecurityException ignored) { }
        }
    }

    private void verificarAtivacaoGPS() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> {
            // O GPS já está ativado
            carregarMapa();
        });

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                // O GPS está desativado, solicita ao usuário para ativar com um clique
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(MapActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    carregarMapa();
                }
            } else {
                carregarMapa();
            }
        });
    }

    private void centralizarUsuarioSePossivel() {
        if (mMap == null) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(this, location -> {
                        if (location != null && mMap != null) {
                            LatLng userPosition = new LatLng(location.getLatitude(), location.getLongitude());
                            
                            // Apenas anima a câmera para a bolinha azul se não houver evento em foco
                            if (getIntent().getLongExtra("focusEventoId", -1) == -1) {
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 15));
                            }
                        }
                    });
        } catch (Exception ignored) { }
    }

    private void carregarMarcadores() {
        if (mMap == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);

        long userId = sessionManager.getUserId();
        long focusId = getIntent().getLongExtra("focusEventoId", -1);

        RepositoryProvider.getInterestRepository(this).getEventosDeInteresse(userId, new RepositoryCallback<List<Evento>>() {
            @Override
            public void onSuccess(List<Evento> eventos) {
                if (focusId != -1) {
                    RepositoryProvider.getEventRepository(MapActivity.this).getEventoById(focusId, new RepositoryCallback<Evento>() {
                        @Override
                        public void onSuccess(Evento focusEvento) {
                            runOnUiThread(() -> {
                                binding.progressBar.setVisibility(View.GONE);
                                plotarNoMapa(eventos, focusEvento);
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() -> {
                                binding.progressBar.setVisibility(View.GONE);
                                plotarNoMapa(eventos, null);
                            });
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        binding.progressBar.setVisibility(View.GONE);
                        plotarNoMapa(eventos, null);
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(MapActivity.this, "Erro ao carregar eventos de interesse", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void plotarNoMapa(List<Evento> eventos, Evento finalFocus) {
        if (mMap == null) return;
        mMap.clear();

        List<Evento> listaEventos = eventos != null ? new ArrayList<>(eventos) : new ArrayList<>();

        if (listaEventos.isEmpty() && finalFocus == null) {
            Toast.makeText(this, "Nenhum evento para exibir no mapa", Toast.LENGTH_SHORT).show();
            return;
        }

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasPoints = false;

        // Adiciona o evento em foco se ele não estiver na lista de interesses
        boolean focusJaNaLista = false;
        if (finalFocus != null) {
            for (Evento e : listaEventos) {
                if (e.id == finalFocus.id) {
                    focusJaNaLista = true;
                    break;
                }
            }
            if (!focusJaNaLista) {
                listaEventos.add(finalFocus);
            }
        }

        for (Evento e : listaEventos) {
            double lat = e.latitude;
            double lng = e.longitude;

            // Fallback determinístico para Viçosa se as coordenadas forem 0
            if (lat == 0.0 && lng == 0.0) {
                lat = -20.7582 + (0.005 * Math.sin(e.id * 1.5));
                lng = -42.8821 + (0.005 * Math.cos(e.id * 1.5));
            }

            LatLng position = new LatLng(lat, lng);
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(e.titulo)
                    .snippet(e.local));

            if (marker != null) {
                marker.setTag(e.id);
                if (finalFocus != null && e.id == finalFocus.id) {
                    marker.showInfoWindow(); // Exibe o título do evento em foco
                }
            }

            boundsBuilder.include(position);
            hasPoints = true;
        }

        // Se há um evento em foco, centraliza a câmera exatamente nele!
        if (finalFocus != null) {
            double lat = finalFocus.latitude;
            double lng = finalFocus.longitude;
            if (lat == 0.0 && lng == 0.0) {
                lat = -20.7582 + (0.005 * Math.sin(finalFocus.id * 1.5));
                lng = -42.8821 + (0.005 * Math.cos(finalFocus.id * 1.5));
            }
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 16));
        } else if (hasPoints) {
            try {
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120));
            } catch (IllegalStateException boundsEx) {
                // Fallback caso a view ainda não esteja completamente renderizada na tela
                if (!listaEventos.isEmpty()) {
                    Evento primeiro = listaEventos.get(0);
                    double lat = primeiro.latitude;
                    double lng = primeiro.longitude;
                    if (lat == 0.0 && lng == 0.0) {
                        lat = -20.7582 + (0.005 * Math.sin(primeiro.id * 1.5));
                        lng = -42.8821 + (0.005 * Math.cos(primeiro.id * 1.5));
                    }
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 16));
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null) {
            carregarMarcadores();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_MAP) {
            boolean granted = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }
            if (granted) {
                verificarAtivacaoGPS();
            } else {
                carregarMapa();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "A localização (GPS) não foi ativada", Toast.LENGTH_SHORT).show();
            }
            // Independente do resultado, agora carregamos o mapa
            carregarMapa();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
