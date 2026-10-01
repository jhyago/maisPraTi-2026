package com.clarim.api.controller;

import com.clarim.api.dto.CheckoutRequest;
import com.clarim.api.dto.CheckoutResposta;
import com.clarim.api.service.CheckoutService;
import com.clarim.api.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// PASSO 1 do fluxo de pagamento, visto pelo navegador: o React chama
// POST /api/assinaturas/checkout e esta classe é a porta de entrada.
// Ela não tem NENHUMA regra de negócio — só traduz "requisição HTTP"
// para "chamada de método Java" e devolve a resposta. Toda a lógica
// de verdade mora no CheckoutService (separação de responsabilidades:
// Controller fala HTTP, Service fala regra de negócio).
@RestController
@RequestMapping("/api/assinaturas")
public class AssinaturaController {
    private final CheckoutService checkoutService;

    public AssinaturaController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout")
    public CheckoutResposta iniciarCheckout(
            // Esta rota NÃO está na lista de permitAll() do
            // ConfiguracaoSeguranca — por isso o Spring Security já garante,
            // antes mesmo de chegar aqui, que existe um token válido.
            // @AuthenticationPrincipal injeta o usuário que o
            // JwtAuthenticationFilter colocou no SecurityContext.
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            // @Valid dispara as anotações do record CheckoutRequest (ex.: @NotNull
            // no planoId) ANTES do método rodar — corpo malformado nem chega aqui.
            @RequestBody @Valid CheckoutRequest checkoutRequest) {

        // Note que o ID do usuário vem do TOKEN (usuarioAutenticado), nunca
        // do corpo da requisição. Se viesse do corpo, qualquer pessoa
        // poderia mandar o ID de OUTRO usuário e comprar assinatura "para ele".
        String url = checkoutService.criarSessao(usuarioAutenticado.getUsuario().getId(), checkoutRequest.planoId());

        // Devolve só a URL — é só isso que o React precisa para redirecionar
        // o navegador para a tela de pagamento do Stripe.
        return new CheckoutResposta(url);
    }
}
