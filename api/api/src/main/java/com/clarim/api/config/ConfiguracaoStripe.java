package com.clarim.api.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoStripe {
    @Value("${stripe.secret-key}")
    private String chaveSecreta;

    @PostConstruct
    public void configurar() {
        if(chaveSecreta == null || chaveSecreta.isBlank()) {
            throw new IllegalStateException("Chave secreta do Stripe não configurada");
        }

        Stripe.apiKey = chaveSecreta;
    }
}
