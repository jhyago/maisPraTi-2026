package com.clarim.api.dto;

import java.time.OffsetDateTime;

public record NoticiaResposta(
        Long id, String titulo, String slug, String resumo,
        String texto,          // null quando a notícia está bloqueada
        String categoria, String autor, boolean premium,
        OffsetDateTime publicadaEm,
        boolean bloqueada      // NOVO: avisa o React para mostrar o convite
) { }