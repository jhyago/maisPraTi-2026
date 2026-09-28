package com.clarim.api.security;

public record DadosGoogle(
        String id,
        String email,
        boolean emailVerified,
        String nome,
        String fotoUrl
) {}
