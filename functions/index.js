const { onDocumentCreated } = require("firebase-functions/v2/firestore");
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