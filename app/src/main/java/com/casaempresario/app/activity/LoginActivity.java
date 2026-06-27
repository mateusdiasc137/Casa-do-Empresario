package com.casaempresario.app.activity;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.casaempresario.app.R;
import com.casaempresario.app.database.Usuario;
import com.casaempresario.app.databinding.ActivityLoginBinding;
import com.casaempresario.app.repository.RepositoryCallback;
import com.casaempresario.app.repository.RepositoryProvider;
import com.casaempresario.app.util.SessionManager;

/**
 * Tela de Login.
 * Autentica o usuário consultando o banco configurado para o aplicativo.
 * As contas iniciais são carregadas durante a criação do banco e a tela de entrada
 * apresenta a identidade visual do CapiHub.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        // Animações visuais da tela de entrada
        binding.getRoot().setAlpha(0f);
        binding.getRoot().setTranslationY(24f);
        binding.getRoot().animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(420)
                .start();

        binding.ringOuter.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_cw));
        binding.ringInner.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_ccw));

        ObjectAnimator pulse = ObjectAnimator.ofFloat(binding.pulseDot, "alpha", 1f, 0.25f);
        pulse.setDuration(1000);
        pulse.setRepeatMode(ObjectAnimator.REVERSE);
        pulse.setRepeatCount(ObjectAnimator.INFINITE);
        pulse.start();

        ObjectAnimator floatMascot = ObjectAnimator.ofFloat(binding.logoCore, "translationY", 0f, -10f, 0f);
        floatMascot.setDuration(2600);
        floatMascot.setRepeatCount(ObjectAnimator.INFINITE);
        floatMascot.start();

        ObjectAnimator tiltMascot = ObjectAnimator.ofFloat(binding.mascotImage, "rotation", -2f, 2f, -2f);
        tiltMascot.setDuration(2800);
        tiltMascot.setRepeatCount(ObjectAnimator.INFINITE);
        tiltMascot.start();

        ObjectAnimator breatheMascot = ObjectAnimator.ofFloat(binding.logoCore, "scaleX", 1f, 1.03f, 1f);
        breatheMascot.setDuration(2200);
        breatheMascot.setRepeatCount(ObjectAnimator.INFINITE);
        breatheMascot.start();

        ObjectAnimator breatheMascotY = ObjectAnimator.ofFloat(binding.logoCore, "scaleY", 1f, 1.03f, 1f);
        breatheMascotY.setDuration(2200);
        breatheMascotY.setRepeatCount(ObjectAnimator.INFINITE);
        breatheMascotY.start();

        // Se já estiver logado, vai direto para a tela principal
        if (sessionManager.isLogado()) {
            irParaMain();
            return;
        }

        binding.btnEntrar.setOnClickListener(v -> fazerLogin());
        binding.btnCadastrar.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        binding.btnExplorarSemLogin.setOnClickListener(v -> {
            sessionManager.salvarVisitante();
            irParaMain();
        });
    }

    private void fazerLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String senha = binding.etSenha.getText().toString().trim();

        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha email e senha", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        RepositoryProvider.getUserRepository(this).login(email, senha, new RepositoryCallback<Usuario>() {
            @Override
            public void onSuccess(Usuario usuario) {
                runOnUiThread(() -> {
                    setLoading(false);
                    if (usuario != null) {
                        sessionManager.salvarSessao(
                                "session-token",
                                usuario.nome,
                                usuario.email,
                                usuario.role,
                                usuario.id);
                        sessionManager.setProfilePhotoUri(usuario.fotoPerfilUri);
                        irParaMain();
                    } else {
                        Toast.makeText(LoginActivity.this, "Email ou senha incorretos", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "Erro ao autenticar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void irParaMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void setLoading(boolean loading) {
        binding.btnEntrar.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnEntrar.setText(loading ? "Entrando..." : "ENTRAR");
    }
}