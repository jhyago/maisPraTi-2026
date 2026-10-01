// src/pages/Assinar/AssinaturaSucesso.jsx
//
// PASSO 3 do fluxo: para onde o Stripe manda o navegador de volta depois
// de um pagamento BEM-SUCEDIDO (ver CheckoutService.criarSessao,
// .setSuccessUrl(frontUrl + "/assinatura/sucesso?session_id=...")).
import { Link } from 'react-router-dom'

function AssinaturaSucesso() {
  // Repare no que esta página NÃO faz: ela não ativa nada, não chama
  // a API, não confia em nada da URL (nem olha o `session_id` da query
  // string). Qualquer pessoa pode digitar este endereço de propósito ou
  // por acaso. A ativação real acontece no PASSO 4 — o webhook do Stripe
  // chamando StripeSincronizacaoService lá no backend, servidor-a-servidor,
  // de um jeito que o visitante não controla. Por isso a mensagem é
  // honesta: "em instantes", não "pronto, liberado".
  return (
    <main className="container">
      <h1>Pagamento recebido!</h1>
      <p>Sua assinatura será ativada em instantes. Obrigado por apoiar o jornalismo do Clarim.</p>
      <Link to="/">Voltar à capa</Link>
    </main>
  )
}

export default AssinaturaSucesso
