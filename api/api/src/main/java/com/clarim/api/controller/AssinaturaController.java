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

@RestController
@RequestMapping("/api/assinaturas")
public class AssinaturaController {
    private final CheckoutService checkoutService;

    public AssinaturaController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout")
    public CheckoutResposta iniciarCheckout(@AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado, @RequestBody @Valid CheckoutRequest checkoutRequest) {
        String url = checkoutService.criarSessao(usuarioAutenticado.getUsuario().getId(), checkoutRequest.planoId());
        return new CheckoutResposta(url);
    }


}
