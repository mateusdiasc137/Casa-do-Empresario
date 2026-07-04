const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

// Dispara quando uma nova mensagem é criada no Firestore
exports.enviarNotificacaoMensagem = onDocumentCreated(
  "mensagens/{mensagemId}",
  async (event) => {
    const mensagem = event.data.data();
    if (!mensagem) return;

    const destinatarioId = String(mensagem.destinatarioId);
    const remetenteId = String(mensagem.remetenteId);

    // Busca o token FCM do destinatário
    const tokenDoc = await getFirestore()
      .collection("fcm_tokens")
      .doc(destinatarioId)
      .get();

    if (!tokenDoc.exists) {
      console.log("Destinatário sem token FCM:", destinatarioId);
      return;
    }

    const token = tokenDoc.data().token;
    if (!token) return;

    // Busca o nome do remetente
    let remetenteNome = "Alguém";
    const usuariosSnapshot = await getFirestore()
      .collection("usuarios")
      .where("id", "==", Number(remetenteId))
      .limit(1)
      .get();

    if (!usuariosSnapshot.empty) {
      const userData = usuariosSnapshot.docs[0].data();
      if (userData.nome) remetenteNome = userData.nome;
    }

    // Envia a notificação push
    const payload = {
      token: token,
      data: {
        tipo: "mensagem",
        remetenteNome: remetenteNome,
        remetenteId: remetenteId,
        texto: mensagem.texto || "Nova mensagem",
        eventoId: String(mensagem.eventoId || ""),
      },
      android: {
        priority: "high",
      },
    };

    try {
      await getMessaging().send(payload);
      console.log("Notificação enviada para usuário", destinatarioId);
    } catch (error) {
      console.error("Erro ao enviar notificação:", error);
    }
  }
);

// Helper function to send multicast notifications
async function sendToAllTokens(payload, excludeUserId = null) {
  const tokensSnapshot = await getFirestore().collection("fcm_tokens").get();
  if (tokensSnapshot.empty) return;

  const tokens = [];
  tokensSnapshot.forEach((doc) => {
    if (excludeUserId && doc.id === String(excludeUserId)) return;
    const token = doc.data().token;
    if (token) tokens.push(token);
  });

  if (tokens.length === 0) return;

  const message = {
    ...payload,
    tokens: tokens,
  };

  try {
    const response = await getMessaging().sendEachForMulticast(message);
    console.log(response.successCount + " mensagens enviadas com sucesso.");
  } catch (error) {
    console.error("Erro ao enviar mensagens multicast:", error);
  }
}

// Dispara quando um novo evento é criado
exports.enviarNotificacaoNovoEvento = onDocumentCreated(
  "eventos/{eventoId}",
  async (event) => {
    const evento = event.data.data();
    if (!evento) return;

    const payload = {
      data: {
        tipo: "evento",
        titulo: String(evento.titulo || "Novo evento publicado"),
        local: String(evento.local || ""),
        eventoId: String(evento.id || event.params.eventoId || ""),
      },
      android: {
        priority: "high",
      },
    };

    await sendToAllTokens(payload, evento.criadoPor);
  }
);

// Dispara quando um evento é atualizado (para notificar interessados)
exports.enviarNotificacaoAtualizacaoEvento = onDocumentUpdated(
  "eventos/{eventoId}",
  async (event) => {
    const eventoAntes = event.data.before.data();
    const eventoDepois = event.data.after.data();

    if (!eventoAntes || !eventoDepois) return;
    
    // Verifica se algum dado importante mudou
    const statusMudou = eventoAntes.status !== eventoDepois.status;
    const tituloMudou = eventoAntes.titulo !== eventoDepois.titulo;
    const localMudou = eventoAntes.local !== eventoDepois.local;
    const dataMudou = eventoAntes.dataEvento !== eventoDepois.dataEvento;
    const dataFimMudou = eventoAntes.dataFimEvento !== eventoDepois.dataFimEvento;
    const descricaoMudou = eventoAntes.descricao !== eventoDepois.descricao;

    if (!statusMudou && !tituloMudou && !localMudou && !dataMudou && !dataFimMudou && !descricaoMudou) {
      return;
    }

    let mensagemNotificacao = "O evento foi atualizado.";
    if (statusMudou) {
      let statusFormatado = eventoDepois.status ? eventoDepois.status.replace(/_/g, ' ') : 'atualizado';
      mensagemNotificacao = `O status do evento mudou para: ${statusFormatado}`;
    } else if (localMudou) {
      mensagemNotificacao = `O local do evento foi alterado.`;
    } else if (dataMudou || dataFimMudou) {
      mensagemNotificacao = `O horário/data do evento foi atualizado.`;
    } else if (tituloMudou || descricaoMudou) {
      mensagemNotificacao = `Detalhes do evento foram alterados.`;
    }

    const eventoId = String(eventoDepois.id || event.params.eventoId || "");
    
    // Busca os usuários interessados neste evento
    const interessesSnapshot = await getFirestore()
      .collection("interesses")
      .where("eventoId", "==", Number(eventoId))
      .get();
      
    if (interessesSnapshot.empty) return;

    const tokens = [];
    for (const doc of interessesSnapshot.docs) {
      const usuarioId = doc.data().usuarioId;
      if (!usuarioId) continue;
      
      const tokenDoc = await getFirestore().collection("fcm_tokens").doc(String(usuarioId)).get();
      if (tokenDoc.exists && tokenDoc.data().token) {
        tokens.push(tokenDoc.data().token);
      }
    }

    if (tokens.length === 0) return;

    const payload = {
      tokens: tokens,
      data: {
        tipo: "evento_atualizacao",
        titulo: String(eventoDepois.titulo || "Evento atualizado"),
        status: String(eventoDepois.status || "atualizado"),
        mensagemCustomizada: String(mensagemNotificacao),
        eventoId: String(eventoDepois.id || event.params.eventoId || ""),
      },
      android: {
        priority: "high",
      },
    };

    try {
      const response = await getMessaging().sendEachForMulticast(payload);
      console.log(`Atualização do evento enviada. Sucessos:`, response.successCount);
    } catch (error) {
      console.error("Erro ao enviar atualização do evento:", error);
    }
  }
);

// Dispara quando um novo comunicado é criado no feed
exports.enviarNotificacaoFeed = onDocumentCreated(
  "feed_posts/{postId}",
  async (event) => {
    const post = event.data.data();
    if (!post) return;
    
    // Notifica apenas se for um post oficial
    if (post.tipo !== "OFICIAL") return;

    const payload = {
      data: {
        tipo: "feed",
        texto: post.texto || "Novo comunicado oficial",
        postId: event.params.postId,
      },
      android: {
        priority: "high",
      },
    };

    await sendToAllTokens(payload, post.autorId);
  }
);

