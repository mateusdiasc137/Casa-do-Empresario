package com.casaempresario.app.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.casaempresario.app.database.Usuario;
import com.casaempresario.app.databinding.ActivityEditProfileBinding;
import com.casaempresario.app.repository.RepositoryCallback;
import com.casaempresario.app.repository.RepositoryProvider;
import com.casaempresario.app.util.SessionManager;

/**
 * Editor do perfil corporativo expandido.
 */
public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        binding.toolbarEditProfile.setNavigationOnClickListener(v -> finish());
        binding.btnSalvarPerfilCompleto.setOnClickListener(v -> salvarPerfil());
        carregarDadosAtuais();
    }

    private void carregarDadosAtuais() {
        binding.etEmpresa.setText(sessionManager.getEmpresa());
        binding.etCargo.setText(sessionManager.getCargo());
        binding.etCidade.setText(sessionManager.getCidade());
        binding.etTelefone.setText(sessionManager.getTelefone());
        binding.etLinkedin.setText(sessionManager.getLinkedin());
        binding.etBio.setText(sessionManager.getBio());
    }

    private void salvarPerfil() {
        if (!sessionManager.isLogado()) {
            Toast.makeText(this, "Entre para editar o perfil.", Toast.LENGTH_SHORT).show();
            return;
        }

        String empresa = texto(binding.etEmpresa.getText());
        String cargo = texto(binding.etCargo.getText());
        String cidade = texto(binding.etCidade.getText());
        String telefone = texto(binding.etTelefone.getText());
        String linkedin = texto(binding.etLinkedin.getText());
        String bio = texto(binding.etBio.getText());

        if (bio.length() > 240) {
            binding.etBio.setError("Use até 240 caracteres");
            binding.etBio.requestFocus();
            return;
        }

        setLoading(true);
        RepositoryProvider.getUserRepository(this).updatePerfilProfissional(
                sessionManager.getUserId(),
                empresa,
                cargo,
                cidade,
                telefone,
                linkedin,
                bio,
                new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        runOnUiThread(() -> {
                            sessionManager.salvarPerfilProfissional(empresa, cargo, cidade, telefone, linkedin, bio);
                            setLoading(false);
                            Toast.makeText(EditProfileActivity.this, "Perfil atualizado com sucesso.", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            sessionManager.salvarPerfilProfissional(empresa, cargo, cidade, telefone, linkedin, bio);
                            setLoading(false);
                            Toast.makeText(EditProfileActivity.this, "Perfil salvo localmente. Sincronização pendente.", Toast.LENGTH_LONG).show();
                            finish();
                        });
                    }
                });
    }

    private String texto(CharSequence valor) {
        return valor != null ? valor.toString().trim() : "";
    }

    private void setLoading(boolean loading) {
        binding.progressEditProfile.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSalvarPerfilCompleto.setEnabled(!loading);
        binding.btnSalvarPerfilCompleto.setText(loading ? "Salvando..." : "Salvar perfil");
    }
}
