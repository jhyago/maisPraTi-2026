// src/pages/Assinar/AssinaturaCancelada.jsx
//
// Rota espelho da de sucesso: para onde o Stripe manda o navegador se o
// usuário clicar em "voltar"/"cancelar" NA TELA DO STRIPE (ver
// CheckoutService.criarSessao, .setCancelUrl(frontUrl + "/assinatura/cancelada")).
// Nenhum evento de webhook é disparado neste caso — não houve cobrança,
// então não há nada para o backend sincronizar.
import { Link } from 'react-router-dom'

function AssinaturaCancelada() {
  return (
    <main className="container">
      <h1>Pagamento não concluído</h1>
      <p>Nenhuma cobrança foi feita. Você pode tentar de novo quando quiser.</p>
      <Link to="/assinar">Ver os planos</Link>
    </main>
  )
}

export default AssinaturaCancelada
