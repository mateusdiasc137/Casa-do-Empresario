package com.casaempresario.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.casaempresario.app.database.AppDatabase;
import com.casaempresario.app.database.Usuario;
import com.casaempresario.app.databinding.ActivityRegisterBinding;
import com.casaempresario.app.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Tela de Cadastro.
 * Cria um novo usuário com role "USER" no banco local (Room/SQLite).
 * Para criar um administrador, use o usuário padrão: admin@admin.com / admin123
 */
public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private SessionManager sessionManager;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getDatabase(this);
        sessionManager = new SessionManager(this);

        binding.btnCadastrar.setOnClickListener(v -> cadastrar());
        binding.btnJaTenhoConta.setOnClickListener(v -> finish()); // volta para o login
    }

    private void cadastrar() {
        String nome     = binding.etNome.getText().toString().trim();
        String email    = binding.etEmail.getText().toString().trim();
        String senha    = binding.etSenha.getText().toString();
        String confirmar = binding.etConfirmarSenha.getText().toString();

        // Validações
        if (nome.isEmpty()) {
            binding.etNome.setError("Informe seu nome");
            binding.etNome.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            binding.etEmail.setError("Informe o e-mail");
            binding.etEmail.requestFocus();
            return;
        }
        if (senha.length() < 6) {
            binding.etSenha.setError("Senha deve ter no mínimo 6 caracteres");
            binding.etSenha.requestFocus();
            return;
        }
        if (!senha.equals(confirmar)) {
            binding.etConfirmarSenha.setError("As senhas não coincidem");
            binding.etConfirmarSenha.requestFocus();
            return;
        }

        setLoading(true);

        new Thread(() -> {
            try {
                // Verifica se o e-mail já está cadastrado
                Usuario existente = db.usuarioDao().findByEmail(email);
                if (existente != null) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(this, "E-mail já cadastrado", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Cria o novo usuário como USER (não admin)
                Usuario novoUsuario = new Usuario();
                novoUsuario.nome     = nome;
                novoUsuario.email    = email;
                novoUsuario.senha    = senha;
                novoUsuario.role     = "USER";
                novoUsuario.criadoEm = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(new Date());

                long novoId = db.usuarioDao().insert(novoUsuario);

                runOnUiThread(() -> {
                    setLoading(false);
                    sessionManager.salvarSessao("local-session", nome, email, "USER", novoId);
                    Toast.makeText(this, "Bem-vindo, " + nome + "! ✅", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Erro ao criar conta: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void setLoading(boolean loading) {
        binding.btnCadastrar.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
