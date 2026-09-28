package com.clarim.api.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "O token do Google é Obrigatório!")
        String credential) { }
