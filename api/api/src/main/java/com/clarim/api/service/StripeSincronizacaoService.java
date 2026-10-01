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
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import com.clarim.api.model.StatusAssinatura;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

// O "tradutor" entre o mundo do Stripe (eventos, Subscriptions, Customers)
// e o nosso mundo (Usuario, Plano, Assinatura). Chamado pelo WebhookController
// DEPOIS que a assinatura HMAC já foi validada — aqui dentro já confiamos
// que o evento é mesmo do Stripe.
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

    // O Stripe manda DEZENAS de tipos de evento (fatura paga, cartão
    // recusado, cliente atualizado...). Só nos importamos com os que mudam
    // o status de uma assinatura — o switch moderno (arrow, sem break)
    // deixa explícito que só estes 3 ramos fazem alguma coisa.
    public void processar(Event evento) {
        switch (evento.getType()) {
            // Disparado assim que o cliente termina de preencher o cartão na
            // tela do Stripe. É o primeiro sinal de "pagamento provavelmente
            // deu certo" — mas o dado mais confiável sobre o STATUS da
            // assinatura (ativa, em trial, etc.) vem do evento de baixo.
            case "checkout.session.completed" -> {
                Session session = (Session) extrairObjeto(evento);

                // Uma Checkout Session pode ser usada para pagamento avulso
                // (sem assinatura) — só seguimos se de fato criou uma subscription.
                if(session.getSubscription() != null) {
                    sincronizar(session.getSubscription());
                }
            }

            // Disparado sempre que o STATUS da assinatura muda no Stripe:
            // trial → active (pagou), active → past_due (cartão recusado),
            // active → canceled (cancelou). É o evento que mantemos como
            // "fonte da verdade" para liberar ou bloquear o conteúdo premium.
            case "customer.subscription.updated",
                 "customer.subscription.deleted" -> {
                    Subscription subscription = (Subscription) extrairObjeto(evento);
                    sincronizar(subscription.getId());
            }

            // Qualquer outro tipo de evento (ex.: invoice.paid) é ignorado de
            // propósito — não é erro, só não é algo que o Clarim precisa tratar.
            default -> logger.debug("Evento Ignorado: {}", evento.getType());
        }
    }

    // O corpo (payload) de um evento do Stripe vem "genérico" por natureza —
    // o SDK às vezes já consegue desserializar para o tipo concreto
    // (ex.: Session, Subscription) e às vezes não (ex.: quando a versão da
    // API do evento é diferente da versão que o SDK espera). Por isso o
    // getObject() devolve um Optional, com deserializeUnsafe() como plano B.
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

    // Este método é o que de fato GRAVA no nosso banco que "este usuário
    // tem acesso premium agora". Repare que ele NUNCA confia nos dados do
    // evento recebido — sempre busca o estado mais atual direto na API do
    // Stripe (Subscription.retrieve). Isso protege contra eventos antigos
    // que chegam fora de ordem (o Stripe não garante ordem de entrega).
    private void sincronizar(String subsctiptionId) {
        try {
            Subscription atual = Subscription.retrieve(subsctiptionId);

            // Ligamos a assinatura do Stripe a um usuário NOSSO pelo
            // stripeCustomerId que salvamos lá no CheckoutService.
            Usuario usuario = usuarioRepository.findByStripeCustomerId(atual.getCustomer()).orElse(null);

            if(usuario == null) {
                // Pode acontecer em ambiente de teste (webhooks disparados
                // manualmente, como fizemos com `stripe trigger`) ou se o
                // Customer foi criado fora do nosso fluxo normal. Melhor
                // logar e devolver 200 do que travar o processamento —
                // não é um erro do nosso sistema, é um evento que não é
                // "nosso".
                logger.info("Assinatura {} de cliente desconhecido, ignorada", subsctiptionId);
                return;
            }

            // getItems().getData() é a lista de "linhas" da assinatura —
            // no nosso caso, sempre uma só (um plano por assinatura). O tipo
            // é SubscriptionItem (não Subscription!): é o "item da linha",
            // que é quem carrega o Price efetivamente cobrado e, nesta
            // versão da API do Stripe, também a data de fim do período pago
            // (currentPeriodEnd migrou de Subscription para SubscriptionItem).
            SubscriptionItem item = atual.getItems().getData().get(0);

            // O preço cobrado no Stripe precisa corresponder a um Plano
            // cadastrado aqui (mesmo stripePriceId) — é assim que sabemos
            // SE é o plano Mensal ou o Anual, por exemplo.
            Plano plano = planoRepository.findByStripePriceId(item.getPrice().getId()).orElseThrow(() -> new RuntimeException("Plano não encontrado"));

            // "Upsert" manual: se já existe uma Assinatura para esse
            // stripeSubscriptionId (ex.: segunda atualização da mesma
            // assinatura), reaproveita a linha; senão, cria uma nova.
            // Isso faz o webhook ser seguro de reprocessar (idempotente)
            // mesmo que o Stripe reenvie o mesmo evento.
            Assinatura assinatura = assinaturaRepository.findByStripeSubscriptionId(atual.getId()).orElseGet(() -> {
                Assinatura novaAssinatura = new Assinatura();
                novaAssinatura.setStripeSubscriptionId(atual.getId());
                novaAssinatura.setUsuario(usuario);
                return novaAssinatura;
            });

            assinatura.setPlano(plano);

            // ESSENCIAL: é este status e esta data que o NoticiaService
            // consulta (existsByUsuarioIdAndStatusInAndPeriodoFimAfter) para
            // decidir se libera o texto premium. Sem estas duas linhas, a
            // Assinatura ficava salva com status/periodoFim nulos — e
            // NENHUM assinante, mesmo tendo pago, seria reconhecido como
            // tal. "active"/"trialing" do Stripe batem com os nomes do
            // nosso enum StatusAssinatura só com toUpperCase().
            assinatura.setStatus(StatusAssinatura.valueOf(atual.getStatus().toUpperCase()));
            assinatura.setPeriodoFim(OffsetDateTime.ofInstant(Instant.ofEpochSecond(item.getCurrentPeriodEnd()), ZoneOffset.UTC));

            assinaturaRepository.save(assinatura);
        } catch (StripeException e) {
            // Falha ao falar com o Stripe (rede, ID inexistente, etc.).
            // Logamos e NÃO relançamos: deixamos o WebhookController devolver
            // 200 mesmo assim, para o Stripe não ficar reentregando um evento
            // que provavelmente vai falhar de novo pelo mesmo motivo.
            logger.error("Erro ao sincronizar assinatura {}", subsctiptionId, e);
        }
    }
}
