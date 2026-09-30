package com.clarim.api.service;

import com.clarim.api.model.Assinatura;
import com.clarim.api.model.Plano;
import com.clarim.api.model.Usuario;
import com.clarim.api.repository.AssinaturaRepository;
import com.clarim.api.repository.PlanoRepository;
import com.clarim.api.repository.UsuarioRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StripeSincronizacaoService {
    private static final Logger logger = LoggerFactory.getLogger(StripeSincronizacaoService.class);

    private final UsuarioRepository usuarioRepository;
    private final PlanoRepository planoRepository;
    private final AssinaturaRepository assinaturaRepository;

    public StripeSincronizacaoService(UsuarioRepository usuarioRepository, PlanoRepository planoRepository, AssinaturaRepository assinaturaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.planoRepository = planoRepository;
        this.assinaturaRepository = assinaturaRepository;
    }

    public void processar(Event evento) {
        switch (evento.getType()) {
            case "checkout.session.completed" -> {
                Session session = (Session) extrairObjeto(evento);

                if(session.getSubscription() != null) {
                    sincronizar(session.getSubscription());
                }
            }

            case "customer.subscription.updated",
                 "customer.subscription.deleted" -> {
                    Subscription subscription = (Subscription) extrairObjeto(evento);
                    sincronizar(subscription.getId());
            }

            default -> logger.debug("Evento Ignorado: {}", evento.getType());
        }
    }

    private StripeObject extrairObjeto(Event evento) {
        EventDataObjectDeserializer leitor = evento.getDataObjectDeserializer();

        return leitor.getObject().orElseGet(() -> {
                try {
                    return leitor.deserializeUnsafe();
                } catch (StripeException e) {
                    logger.error("Erro ao deserializar objeto Stripe", e);
                    return null;
                }
        });
    }

    private void sincronizar(String subsctiptionId) {
        try {
            Subscription atual = Subscription.retrieve(subsctiptionId);

            Usuario usuario = usuarioRepository.findByStripeCustomerId(atual.getCustomer()).orElse(null);

            if(usuario == null) {
                logger.info("Assinatura {} de cliente desconhecido, ignorada", subsctiptionId);
                return;
            }

            Subscription item = atual.getItems().getData().get(0);

            Plano plano = planoRepository.findByStripePriceId(item.getPrice().getId()).orElseThrow(() -> new RuntimeException("Plano não encontrado"));

            Assinatura assinatura = assinaturaRepository.findByStripeSubscriptionId(atual.getId()).orElseGet(() -> {
                Assinatura novaAssinatura = new Assinatura();
                novaAssinatura.setStripeSubscriptionId(atual.getId());
                novaAssinatura.setUsuario(usuario);
                return novaAssinatura;
            });

            assinatura.setPlano(plano);
            assinaturaRepository.save(assinatura);
        } catch (StripeException e) {
            logger.error("Erro ao sincronizar assinatura {}", subsctiptionId, e);
        }
    }
}
