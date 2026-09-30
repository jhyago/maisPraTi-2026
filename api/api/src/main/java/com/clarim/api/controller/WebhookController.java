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


@RestController
@RequestMapping("/api/webhook")
public class WebhookController {
    private static final Logger log = (Logger) LoggerFactory.getLogger(WebhookController.class);

    private final StripeSincronizacaoService sincronizacaoService;
    private final String segredoWebhook;

    public WebhookController(StripeSincronizacaoService sincronizacaoService, @Value("${stripe.webhook-secret") String segredoWebhook) {
        this.sincronizacaoService = sincronizacaoService;
        this.segredoWebhook = segredoWebhook;
    }

    @PostMapping("/stripe")
    public ResponseEntity<Void> receber (@RequestBody String corpo, @RequestHeader("Stripe-Signature") String assinatura) {
        Event evento;

        try {
            evento = Webhook.constructEvent(corpo, assinatura, segredoWebhook);
        } catch (SignatureVerificationException e) {
            log.error("Erro ao validar assinatura do webhook", e);
            return ResponseEntity.badRequest().build();
        }

        sincronizacaoService.processar(evento);
        return ResponseEntity.ok().build();
    }
}
