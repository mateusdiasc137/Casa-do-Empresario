package com.casaempresario.app.activity;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.casaempresario.app.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private EditText etSearchAddress;
    private ImageButton btnSearch;
    private TextView tvPickedAddress;
    private MaterialButton btnConfirmLocation;

    private LatLng currentLatLng;
    private String currentAddressString = "";
    private Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        // Referência de views
        etSearchAddress = findViewById(R.id.et_search_address);
        btnSearch = findViewById(R.id.btn_search);
        tvPickedAddress = findViewById(R.id.tv_picked_address);
        btnConfirmLocation = findViewById(R.id.btn_confirm_location);

        geocoder = new Geocoder(this, Locale.getDefault());

        // Recuperar possíveis coordenadas pré-existentes
        double defaultLat = getIntent().getDoubleExtra("currentLatitude", -23.5505199);
        double defaultLng = getIntent().getDoubleExtra("currentLongitude", -46.6333094);
        currentLatLng = new LatLng(defaultLat, defaultLng);

        // Inicializar fragmento do mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Listener do botão de busca
        btnSearch.setOnClickListener(v -> buscarEndereco());

        // Listener do teclado na busca
        etSearchAddress.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                buscarEndereco();
                return true;
            }
            return false;
        });

        // Listener do botão de confirmação
        btnConfirmLocation.setOnClickListener(v -> confirmarLocal());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        // Configurações visuais do mapa
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        // Mover para coordenadas iniciais
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f));

        // Escutar quando o movimento da câmera para (idle)
        mMap.setOnCameraIdleListener(() -> {
            LatLng target = mMap.getCameraPosition().target;
            reverseGeocode(target);
        });
    }

    private void reverseGeocode(LatLng latLng) {
        tvPickedAddress.setText("Buscando endereço...");
        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                            sb.append(address.getAddressLine(i));
                            if (i < address.getMaxAddressLineIndex()) {
                                sb.append(", ");
                            }
                        }
                        currentAddressString = sb.toString();
                        currentLatLng = latLng;
                        tvPickedAddress.setText(currentAddressString);
                    } else {
                        currentAddressString = "Coordenadas: " + latLng.latitude + ", " + latLng.longitude;
                        currentLatLng = latLng;
                        tvPickedAddress.setText(currentAddressString);
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    currentAddressString = "Coordenadas: " + latLng.latitude + ", " + latLng.longitude;
                    currentLatLng = latLng;
                    tvPickedAddress.setText(currentAddressString);
                });
            }
        }).start();
    }

    private void buscarEndereco() {
        String query = etSearchAddress.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Digite um endereço para buscar", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocationName(query, 1);
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        LatLng foundLatLng = new LatLng(address.getLatitude(), address.getLongitude());
                        if (mMap != null) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(foundLatLng, 16f));
                        }
                    } else {
                        Toast.makeText(this, "Endereço não encontrado", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Erro ao buscar endereço (verifique sua internet)", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void confirmarLocal() {
        if (currentAddressString.isEmpty() || 
                currentAddressString.equals("Buscando endereço...") || 
                currentAddressString.equals("Arraste o mapa para selecionar...")) {
            Toast.makeText(this, "Por favor, selecione um endereço válido no mapa.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("selectedAddress", currentAddressString);
        resultIntent.putExtra("selectedLatitude", currentLatLng.latitude);
        resultIntent.putExtra("selectedLongitude", currentLatLng.longitude);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
