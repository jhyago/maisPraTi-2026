package com.clarim.api.service;

import com.clarim.api.model.Plano;
import com.clarim.api.model.StatusAssinatura;
import com.clarim.api.model.Usuario;
import com.clarim.api.repository.AssinaturaRepository;
import com.clarim.api.repository.PlanoRepository;
import com.clarim.api.repository.UsuarioRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class CheckoutService {
    private final UsuarioRepository usuarioRepository;
    private final PlanoRepository planoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final String frontUrl;

    public CheckoutService(UsuarioRepository usuarioRepository, PlanoRepository planoRepository, AssinaturaRepository assinaturaRepository, @Value("${app.front-url}") String frontUrl) {
        this.usuarioRepository = usuarioRepository;
        this.planoRepository = planoRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.frontUrl = frontUrl;
    }

    public String criarSessao(Long usuarioId, Long planoId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        Plano plano = planoRepository.findById(planoId).orElseThrow();

        boolean jaAssina = assinaturaRepository.existsByUsuarioIdAndStatusInAndPeriodoFimAfter(usuarioId, List.of(StatusAssinatura.ACTIVE, StatusAssinatura.TRIALING), OffsetDateTime.now());

        if(jaAssina) {
            return frontUrl;
        }

        try {
            String customerId = obterOuCriarCustomer(usuario);

            SessionCreateParams parametros = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(plano.getStripePriceId())
                            .setQuantity(1L)
                            .build())
                    .setSuccessUrl(frontUrl + "/assinatura/sucesso?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontUrl + "/assinatura/cancelada")
                    .setClientReferenceId(usuario.getId().toString())
                    .build();

            Session sessao = Session.create(parametros);

            return sessao.getUrl();
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }

    private String obterOuCriarCustomer(Usuario usuario) throws StripeException {
        if(usuario.getStripeCustomerId() != null) {
            return usuario.getStripeCustomerId();
        }

        CustomerCreateParams parametros = CustomerCreateParams.builder()
                .setEmail(usuario.getEmail())
                .setName(usuario.getNome())
                .putMetadata("usuarioId", usuario.getId().toString())
                .build();

        Customer customer = Customer.create(parametros);

        usuario.setStripeCustomerId(customer.getId());

        return customer.getId();
    }
}
