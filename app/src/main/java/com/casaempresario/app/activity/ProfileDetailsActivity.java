package com.casaempresario.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.casaempresario.app.databinding.ActivityProfileDetailsBinding;
import com.casaempresario.app.util.SessionManager;

/**
 * Perfil corporativo expandido do usuário.
 */
public class ProfileDetailsActivity extends AppCompatActivity {

    private ActivityProfileDetailsBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        binding.toolbarProfileDetails.setNavigationOnClickListener(v -> finish());
        binding.btnEditarPerfilCompleto.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        preencherPerfil();
    }

    private void preencherPerfil() {
        if (!sessionManager.isLogado()) {
            binding.tvProfileDetailsName.setText("Visitante");
            binding.tvProfileDetailsEmail.setText("Entre para habilitar seu perfil corporativo");
            binding.tvProfileDetailsRole.setText("VISITANTE");
            binding.btnEditarPerfilCompleto.setText("Entrar para completar perfil");
            binding.btnEditarPerfilCompleto.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
            aplicarTextoVazio();
            return;
        }

        binding.tvProfileDetailsName.setText(sessionManager.getNome());
        binding.tvProfileDetailsEmail.setText(sessionManager.getEmail());
        binding.tvProfileDetailsRole.setText(sessionManager.getRole());
        binding.tvProfileAvatarLetters.setText(gerarIniciais(sessionManager.getNome()));

        String foto = sessionManager.getProfilePhotoUri();
        if (foto != null && !foto.trim().isEmpty()) {
            binding.imgProfileDetailsPhoto.setVisibility(View.VISIBLE);
            binding.tvProfileAvatarLetters.setVisibility(View.GONE);
            Glide.with(this).load(foto).circleCrop().into(binding.imgProfileDetailsPhoto);
        } else {
            binding.imgProfileDetailsPhoto.setVisibility(View.GONE);
            binding.tvProfileAvatarLetters.setVisibility(View.VISIBLE);
        }

        binding.tvProfileCompany.setText(valorOuPadrao(sessionManager.getEmpresa(), "Empresa não informada"));
        binding.tvProfilePosition.setText(valorOuPadrao(sessionManager.getCargo(), "Cargo não informado"));
        binding.tvProfileCity.setText(valorOuPadrao(sessionManager.getCidade(), "Cidade não informada"));
        binding.tvProfilePhone.setText(valorOuPadrao(sessionManager.getTelefone(), "Telefone não informado"));
        binding.tvProfileLinkedin.setText(valorOuPadrao(sessionManager.getLinkedin(), "LinkedIn não informado"));
        binding.tvProfileBio.setText(valorOuPadrao(sessionManager.getBio(), "Adicione uma breve apresentação profissional para facilitar conexões em eventos."));
        binding.tvProfileCompleteness.setText(calcularCompletudePerfil() + "% completo");
    }

    private void aplicarTextoVazio() {
        binding.tvProfileCompany.setText("Empresa não informada");
        binding.tvProfilePosition.setText("Cargo não informado");
        binding.tvProfileCity.setText("Cidade não informada");
        binding.tvProfilePhone.setText("Telefone não informado");
        binding.tvProfileLinkedin.setText("LinkedIn não informado");
        binding.tvProfileBio.setText("Entre para adicionar uma apresentação profissional.");
        binding.tvProfileCompleteness.setText("0% completo");
    }

    private int calcularCompletudePerfil() {
        int total = 7;
        int preenchidos = 0;
        if (!isBlank(sessionManager.getNome())) preenchidos++;
        if (!isBlank(sessionManager.getEmail())) preenchidos++;
        if (!isBlank(sessionManager.getEmpresa())) preenchidos++;
        if (!isBlank(sessionManager.getCargo())) preenchidos++;
        if (!isBlank(sessionManager.getCidade())) preenchidos++;
        if (!isBlank(sessionManager.getTelefone())) preenchidos++;
        if (!isBlank(sessionManager.getBio())) preenchidos++;
        return Math.round((preenchidos * 100f) / total);
    }

    private String valorOuPadrao(String valor, String padrao) {
        return isBlank(valor) ? padrao : valor;
    }

    private boolean isBlank(String valor) {
        return valor == null || valor.trim().isEmpty();
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
}
