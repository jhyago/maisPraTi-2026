package com.clarim.api.controller;

import com.clarim.api.service.StripeSincronizacaoService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// PASSO 4 do fluxo, e o mais importante para a SEGURANÇA do pagamento:
// é AQUI, e só aqui, que a assinatura é efetivamente ativada no nosso banco.
//
// Por quê não ativar já na volta do checkout (AssinaturaController)? Porque
// o navegador do usuário pode fechar a aba, perder internet, ou o usuário
// pode simplesmente nunca voltar — mas se ele PAGOU, o Stripe precisa nos
// avisar de um jeito que não dependa do navegador dele. O webhook é o
// servidor do Stripe chamando o NOSSO servidor diretamente, servidor-a-servidor.
@RestController
@RequestMapping("/api/webhook")
public class WebhookController {
    private static final Logger log = (Logger) LoggerFactory.getLogger(WebhookController.class);

    private final StripeSincronizacaoService sincronizacaoService;
    private final String segredoWebhook; // stripe.webhook-secret — NUNCA é a mesma chave do checkout

    public WebhookController(StripeSincronizacaoService sincronizacaoService, @Value("${stripe.webhook-secret}") String segredoWebhook) {
        this.sincronizacaoService = sincronizacaoService;
        this.segredoWebhook = segredoWebhook;
    }

    @PostMapping("/stripe")
    public ResponseEntity<Void> receber(
            // @RequestBody String, não um DTO: precisamos do texto CRU, byte a
            // byte, porque a assinatura abaixo é calculada sobre essa string
            // exata. Se o Spring desserializasse para um objeto e nós o
            // serializássemos de volta para validar, a formatação poderia
            // mudar (ordem de campos, espaços) e a assinatura não bateria mais.
            @RequestBody String corpo,
            // Cabeçalho que o STRIPE envia, nunca o nosso front. É a "prova"
            // de que quem está chamando este endpoint é o Stripe de verdade.
            @RequestHeader("Stripe-Signature") String assinatura) {
        Event evento;

        try {
            // Aqui está a trava de segurança do endpoint: sem ela, QUALQUER
            // pessoa na internet poderia mandar um POST falso para
            // /api/webhook/stripe fingindo "paguei!" e ganhar acesso premium
            // de graça. constructEvent recalcula o HMAC do corpo usando o
            // MESMO segredo configurado no Stripe e compara com o cabeçalho.
            evento = Webhook.constructEvent(corpo, assinatura, segredoWebhook);
        } catch (SignatureVerificationException e) {
            // Assinatura não bate (segredo errado, corpo alterado no meio do
            // caminho, ou requisição forjada) → rejeitamos com 400 e nem
            // chegamos a olhar o conteúdo do evento.
            log.error("Erro ao validar assinatura do webhook", e);
            return ResponseEntity.badRequest().build();
        }

        // Evento autêntico confirmado: agora sim, delega para a camada de
        // serviço decidir o que fazer com cada TIPO de evento.
        sincronizacaoService.processar(evento);

        // 200 OK é o "recibo de entrega" para o Stripe: "recebi, pode parar
        // de reenviar este evento". Se devolvêssemos erro aqui, o Stripe
        // ficaria retentando esta mesma notificação por até vários dias.
        return ResponseEntity.ok().build();
    }
}
