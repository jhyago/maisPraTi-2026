package com.clarim.api.dto;

import jakarta.validation.constraints.NotNull;

// O corpo que o React manda em POST /api/assinaturas/checkout. De
// propósito, só o ID do PLANO — o ID do USUÁRIO nunca vem do corpo da
// requisição, e sim do token JWT (ver AssinaturaController), para que
// ninguém possa comprar uma assinatura "em nome" de outra pessoa.
public record CheckoutRequest(@NotNull(message = "Escolha um plano") Long planoId) {}
