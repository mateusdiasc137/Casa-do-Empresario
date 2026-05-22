package com.casaempresario.app.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.casaempresario.app.R;
import com.casaempresario.app.adapter.ChatAdapter;
import com.casaempresario.app.database.Evento;
import com.casaempresario.app.database.Mensagem;
import com.casaempresario.app.database.Usuario;
import com.casaempresario.app.databinding.ActivityChatBinding;
import com.casaempresario.app.repository.RepositoryCallback;
import com.casaempresario.app.repository.RepositoryProvider;
import com.casaempresario.app.util.SessionManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private SessionManager sessionManager;
    private ChatAdapter adapter;
    private long userId;
    private long outroUserId;
    private long eventoId;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        userId = sessionManager.getUserId();
        outroUserId = getIntent().getLongExtra("outroUserId", -1);
        eventoId = getIntent().getLongExtra("eventoId", -1);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mensagens");
        }

        setupRecyclerView();
        carregarDadosCabecalho();
        carregarHistorico();

        binding.btnEnviar.setOnClickListener(v -> enviarMensagem());
    }

    private void setupRecyclerView() {
        adapter = new ChatAdapter(new ArrayList<>(), userId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.recyclerMensagens.setLayoutManager(layoutManager);
        binding.recyclerMensagens.setAdapter(adapter);
    }

    private void carregarDadosCabecalho() {
        RepositoryProvider.getUserRepository(this).getUsuarioById(outroUserId, new RepositoryCallback<Usuario>() {
            @Override
            public void onSuccess(Usuario outroUser) {
                runOnUiThread(() -> {
                    if (outroUser != null) {
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle(outroUser.nome);
                        }
                        binding.tvEventoOrganizador.setText("Contato: " + outroUser.nome);
                    }
                });

                RepositoryProvider.getEventRepository(ChatActivity.this).getEventoById(eventoId, new RepositoryCallback<Evento>() {
                    @Override
                    public void onSuccess(Evento evento) {
                        runOnUiThread(() -> {
                            if (evento != null) {
                                binding.tvEventoTitulo.setText(evento.titulo);
                                if (evento.bannerUri != null && !evento.bannerUri.isEmpty()) {
                                    Glide.with(ChatActivity.this)
                                            .load(evento.bannerUri)
                                            .into(binding.imgEventoMiniatura);
                                } else {
                                    binding.imgEventoMiniatura.setImageResource(R.drawable.ic_event_placeholder);
                                }
                            } else {
                                binding.cardEventoContext.setVisibility(View.GONE);
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> binding.cardEventoContext.setVisibility(View.GONE));
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                // Silencioso
            }
        });
    }

    private void carregarHistorico() {
        RepositoryProvider.getChatRepository(this).getChatThread(userId, outroUserId, eventoId, new RepositoryCallback<List<Mensagem>>() {
            @Override
            public void onSuccess(List<Mensagem> thread) {
                runOnUiThread(() -> {
                    adapter.atualizar(thread);
                    if (adapter.getItemCount() > 0) {
                        binding.recyclerMensagens.scrollToPosition(adapter.getItemCount() - 1);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Erro ao carregar mensagens", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void enviarMensagem() {
        String texto = binding.etMensagem.getText().toString().trim();
        if (texto.isEmpty()) return;

        binding.etMensagem.setText("");

        Mensagem msg = new Mensagem();
        msg.eventoId = eventoId;
        msg.remetenteId = userId;
        msg.destinatarioId = outroUserId;
        msg.texto = texto;
        msg.timestamp = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        RepositoryProvider.getChatRepository(this).insert(msg, new RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long result) {
                runOnUiThread(() -> {
                    carregarHistorico();
                    verificarESimularAutoResposta(texto);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Erro ao enviar mensagem", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void verificarESimularAutoResposta(String msgOriginal) {
        RepositoryProvider.getUserRepository(this).getUsuarioById(outroUserId, new RepositoryCallback<Usuario>() {
            @Override
            public void onSuccess(Usuario outroUser) {
                if (outroUser != null && "ORGANIZADOR".equals(outroUser.role)) {
                    handler.postDelayed(() -> {
                        Mensagem resposta = new Mensagem();
                        resposta.eventoId = eventoId;
                        resposta.remetenteId = outroUserId;
                        resposta.destinatarioId = userId;
                        resposta.texto = "Olá! Recebi sua mensagem sobre o evento. Te respondo em breve! 📅";
                        resposta.timestamp = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

                        RepositoryProvider.getChatRepository(ChatActivity.this).insert(resposta, new RepositoryCallback<Long>() {
                            @Override
                            public void onSuccess(Long result) {
                                runOnUiThread(() -> carregarHistorico());
                            }

                            @Override
                            public void onError(Exception e) {
                                // Silencioso
                            }
                        });
                    }, 1500);
                }
            }

            @Override
            public void onError(Exception e) {
                // Silencioso
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
