// src/pages/Assinar/Assinar.jsx
//
// Esta página é o PASSO 0 + PASSO 1 do fluxo de pagamento, do ponto de
// vista de quem usa o site: mostra os planos disponíveis e, ao clicar,
// dispara a criação da sessão de checkout no Stripe. Ela só é alcançável
// por usuário LOGADO (ver App.jsx: <RotaProtegida><Assinar /></RotaProtegida>),
// porque o backend precisa saber QUEM está comprando.
import { useEffect, useState } from 'react'
import { listarPlanos, iniciarCheckout } from '../../services/assinatura'

function Assinar() {
  const [planos, setPlanos] = useState([])
  const [aviso, setAviso] = useState('')
  // Trava o botão enquanto esperamos o backend responder com a URL do
  // Stripe — evita duplo-clique criando duas sessões de checkout.
  const [redirecionando, setRedirecionando] = useState(false)

  // Busca os planos ao montar a página — o padrão da aula de API.
  // Roda uma única vez (array de dependências vazio: []).
  useEffect(() => {
    listarPlanos()
      .then(setPlanos)
      .catch(() => setAviso('Não foi possível carregar os planos.'))
  }, [])

  // Chamado pelo onClick do botão de cada plano.
  async function assinar(planoId) {
    setRedirecionando(true)
    try {
      // PASSO 1: pede ao NOSSO backend para abrir uma sessão de pagamento.
      // O backend fala com o Stripe por trás; aqui só recebemos a URL final.
      const url = await iniciarCheckout(planoId)

      // PASSO 2: saída do nosso SPA. window.location.href, e NÃO o
      // navigate() do React Router. O navigate só troca componentes
      // DENTRO do Clarim; a página do Stripe é outro site, outro domínio.
      // Para sair do app, é preciso uma navegação de verdade do navegador
      // (perde o estado do React de propósito — vamos para fora).
      window.location.href = url
    } catch (erro) {
      // Dois motivos comuns de falha aqui:
      //  - 409 CONFLICT: CheckoutService detectou que o usuário já tem
      //    assinatura vigente (AssinaturaJaAtivaException) — mensagem
      //    específica vem em erro.response.data.mensagem.
      //  - outro erro (ex.: 500 se o Stripe estiver fora do ar) → cai no
      //    texto genérico, já que não há uma `mensagem` estruturada.
      setAviso(erro.response?.data?.mensagem ?? 'Não foi possível iniciar o pagamento.')
      setRedirecionando(false)
      // Não há `finally` aqui de propósito: se o redirecionamento DEU
      // CERTO, a página vai navegar para fora do site mesmo — não faz
      // sentido "destravar o botão" de uma tela que já está saindo.
    }
  }

  return (
    <main className="container">
      <h1>Assine o Clarim</h1>
      {aviso && <p className="aviso">{aviso}</p>}
      {planos.map((plano) => (
        <article key={plano.id} className="card">
          <h3>{plano.nome}</h3>
          <p>{plano.precoFormatado} / {plano.intervalo === 'MONTH' ? 'mês' : 'ano'}</p>
          <button disabled={redirecionando} onClick={() => assinar(plano.id)}>
            {redirecionando ? 'Abrindo o pagamento…' : 'Assinar'}
          </button>
        </article>
      ))}
    </main>
  )
}

export default Assinar
