package com.clarim.api.dto;

import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(@NotNull(message = "Escolha um plano") Long planoId) {}
