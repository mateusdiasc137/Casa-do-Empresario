package com.casaempresario.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.casaempresario.app.database.AppDatabase;
import com.casaempresario.app.database.Usuario;
import com.casaempresario.app.databinding.ActivityLoginBinding;
import com.casaempresario.app.util.SessionManager;

/**
 * Tela de Login.
 * Autentica o usuário consultando o banco local (Room/SQLite).
 * Admin padrão criado automaticamente: admin@admin.com / admin123
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private SessionManager sessionManager;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getDatabase(this);
        sessionManager = new SessionManager(this);

        // Se já estiver logado, vai direto para a tela principal
        if (sessionManager.isLogado()) {
            irParaMain();
            return;
        }

        binding.btnEntrar.setOnClickListener(v -> fazerLogin());
        binding.btnCadastrar.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void fazerLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String senha = binding.etSenha.getText().toString().trim();

        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha email e senha", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        // Consulta o banco em background para não travar a UI
        new Thread(() -> {
            Usuario usuario = db.usuarioDao().login(email, senha);

            runOnUiThread(() -> {
                setLoading(false);
                if (usuario != null) {
                    // Salva a sessão localmente com os dados do usuário encontrado
                    sessionManager.salvarSessao(
                            "local-session",  // token simbólico (sem API)
                            usuario.nome,
                            usuario.email,
                            usuario.role,
                            usuario.id
                    );
                    irParaMain();
                } else {
                    Toast.makeText(this, "Email ou senha incorretos", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void irParaMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void setLoading(boolean loading) {
        binding.btnEntrar.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnEntrar.setText(loading ? "Entrando..." : "Entrar");
    }
}
