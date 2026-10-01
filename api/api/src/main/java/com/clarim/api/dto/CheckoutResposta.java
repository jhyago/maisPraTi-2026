package com.clarim.api.dto;

// A única coisa que o front precisa de volta: o link da página de
// pagamento hospedada pelo Stripe. O React só faz window.location.href = url
// (ver Assinar.jsx) — nenhum dado de cartão passa por aqui ou por nós.
public record CheckoutResposta(String url) {
}
