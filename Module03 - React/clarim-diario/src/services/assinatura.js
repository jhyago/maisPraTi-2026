// src/services/assinatura.js
//
// Service da tela /assinar: só sabe conversar HTTP com os endpoints de
// plano e checkout. Nenhuma regra de negócio mora aqui — isso é trabalho
// do backend (CheckoutService.java).
import { api } from './api'   // a instância com o interceptor: o token vai sozinho

// PASSO 0: lista os planos ativos (Mensal, Anual...) para a tela desenhar
// os cartões. Rota pública (não precisa estar logado para VER os preços).
export async function listarPlanos() {
  const { data } = await api.get('/api/planos')
  return data
}

// PASSO 1: pede ao backend para abrir uma sessão de pagamento no Stripe
// para o plano escolhido. Esta chamada SIM exige login — o `api` já manda
// o token, e é a partir dele que o backend sabe QUEM está comprando
// (ver AssinaturaController.iniciarCheckout, que lê o usuário do token,
// nunca de um campo que o front poderia forjar).
export async function iniciarCheckout(planoId) {
  const { data } = await api.post('/api/assinaturas/checkout', { planoId })
  return data.url   // a página de pagamento do Stripe — o PASSO 2 é ir até lá
}
