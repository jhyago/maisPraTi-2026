package com.clarim.api.service;

import com.clarim.api.exception.AssinaturaJaAtivaException;
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

// Esta classe é o PASSO 1 do fluxo de pagamento: o React pede "quero
// assinar o plano X" e aqui a gente monta a "sacola de compras" do
// Stripe (a Checkout Session) e devolve o link para onde o Stripe
// hospeda a tela de pagamento. Repare que em NENHUM momento guardamos
// cartão, CVV ou qualquer dado sensível — isso é o Stripe quem vê.
@Service
public class CheckoutService {
    private final UsuarioRepository usuarioRepository;
    private final PlanoRepository planoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final String frontUrl; // ex.: http://localhost:5173 — para onde o Stripe manda o navegador de volta

    public CheckoutService(UsuarioRepository usuarioRepository, PlanoRepository planoRepository, AssinaturaRepository assinaturaRepository, @Value("${app.front-url}") String frontUrl) {
        this.usuarioRepository = usuarioRepository;
        this.planoRepository = planoRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.frontUrl = frontUrl;
    }

    // Chamado pelo AssinaturaController.iniciarCheckout(). Devolve a URL
    // da página de pagamento do Stripe — o React só faz window.location.href = url.
    public String criarSessao(Long usuarioId, Long planoId) {
        // orElseThrow() sem mensagem aqui é aceitável: se o usuário ou o
        // plano não existem, é porque o próprio token JWT ou o botão
        // clicado no front estão com um ID inválido — um bug de
        // integração, não algo que o usuário final provoque digitando.
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        Plano plano = planoRepository.findById(planoId).orElseThrow();

        // Regra de negócio ANTES de falar com o Stripe: por que gastar uma
        // chamada de API externa (e criar um Customer duplicado lá!) se a
        // pessoa já paga? A consulta olha só para assinaturas "vigentes"
        // (ACTIVE ou TRIALING e dentro do período pago) — ver Assinatura.estaVigente().
        boolean jaAssina = assinaturaRepository.existsByUsuarioIdAndStatusInAndPeriodoFimAfter(usuarioId, List.of(StatusAssinatura.ACTIVE, StatusAssinatura.TRIALING), OffsetDateTime.now());

        if(jaAssina) {
            // ANTES isto devolvia silenciosamente a home (frontUrl) — o
            // usuário clicava "Assinar" e... nada acontecia, sem explicação.
            // Lançar a exceção deixa o TratadorGlobalDeExcecoes transformar
            // isso num 409 com mensagem, que o Assinar.jsx já sabia exibir.
            throw new AssinaturaJaAtivaException("Você já possui uma assinatura ativa.");
        }

        try {
            // O Stripe precisa de um "Customer" (cliente) do lado dele para
            // associar a assinatura. Criamos um só uma vez por usuário e
            // reaproveitamos o ID salvo (ver obterOuCriarCustomer abaixo).
            String customerId = obterOuCriarCustomer(usuario);

            // SessionCreateParams é um "builder": cada .setX() configura um
            // pedaço da sessão de checkout que o Stripe vai hospedar.
            SessionCreateParams parametros = SessionCreateParams.builder()
                    // SUBSCRIPTION (não PAYMENT): isso diz ao Stripe "cobre
                    // recorrentemente", não "cobre uma vez só".
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            // stripePriceId é o ID do preço CADASTRADO NO DASHBOARD do
                            // Stripe (ex.: price_123) — não confundir com o id do nosso Plano.
                            .setPrice(plano.getStripePriceId())
                            .setQuantity(1L)
                            .build())
                    // Para onde o NAVEGADOR volta depois de pagar/cancelar.
                    // {CHECKOUT_SESSION_ID} é um placeholder que o PRÓPRIO Stripe
                    // substitui pelo ID real da sessão ao redirecionar.
                    .setSuccessUrl(frontUrl + "/assinatura/sucesso?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontUrl + "/assinatura/cancelada")
                    // Guardamos nosso usuarioId "grudado" na sessão. Fica disponível
                    // depois nos eventos de webhook, útil para auditoria/depuração.
                    .setClientReferenceId(usuario.getId().toString())
                    .build();

            // Esta é a ÚNICA chamada de rede para o Stripe neste método:
            // pede para ele CRIAR a sessão de pagamento hospedada.
            Session sessao = Session.create(parametros);

            // sessao.getUrl() é o link para a tela de pagamento do Stripe.
            // A ativação de verdade da assinatura NÃO acontece aqui — ela só
            // é confirmada depois, quando o Stripe chama nosso webhook
            // (StripeSincronizacaoService). Até lá, ninguém foi cobrado.
            return sessao.getUrl();
        } catch (StripeException e) {
            // Qualquer falha de comunicação com o Stripe (chave inválida,
            // price inexistente, instabilidade da API) cai aqui.
            throw new RuntimeException(e);
        }
    }

    // "Buscar ou criar": idempotente — se o usuário já tem um Customer no
    // Stripe (de uma tentativa anterior), reaproveita; senão, cria um novo.
    private String obterOuCriarCustomer(Usuario usuario) throws StripeException {
        if(usuario.getStripeCustomerId() != null) {
            return usuario.getStripeCustomerId();
        }

        CustomerCreateParams parametros = CustomerCreateParams.builder()
                .setEmail(usuario.getEmail())
                .setName(usuario.getNome())
                // putMetadata guarda um par chave/valor LIVRE no objeto do Stripe.
                // Não afeta cobrança nenhuma — é só para conseguirmos, olhando
                // o painel do Stripe, saber "esse Customer é o usuário #42 aqui".
                .putMetadata("usuarioId", usuario.getId().toString())
                .build();

        Customer customer = Customer.create(parametros);

        // Guardamos o ID retornado para a PRÓXIMA chamada não criar outro
        // Customer duplicado. Atenção aqui: só fazer `usuario.setX(...)` NÃO
        // basta — como este método não roda dentro de uma transação do
        // Spring (não há @Transactional na cadeia de chamadas), a entidade
        // não é "gerenciada" de um jeito que gere UPDATE automático. Sem o
        // save() explícito abaixo, essa mudança se perdia silenciosamente:
        // a cada clique em "Assinar" nascia um Customer novo no Stripe para
        // o MESMO usuário. O save() é o que efetivamente grava no banco.
        usuario.setStripeCustomerId(customer.getId());
        usuarioRepository.save(usuario);

        return customer.getId();
    }
}
