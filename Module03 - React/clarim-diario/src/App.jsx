// ============================================================
//   COMPONENTE RAIZ — monta o layout e decide QUAL página mostrar
// ============================================================
import { useState, useEffect } from 'react'
import { Routes, Route } from 'react-router-dom'
import Header from './components/Header/Header'
import Home from './pages/Home/Home'
import Materia from './pages/Materia/Materia'
import Cadastro from './pages/Cadastro/Cadastro'
import Login from './pages/Login/Login'
import RotaProtegida from './components/RotaProtegida'
import Footer from './components/Footer/Footer'
import Painel from './pages/Painel/Painel'
import Assinar from './pages/Assinar/Assinar'
import AssinaturaCancelada from './pages/Assinar/AssinaturaCancelada'
import './App.css'
import AssinaturaSucesso from './pages/Assinar/AssinaturaSucesso'

function App() {
  // useState com FUNÇÃO inicial (lazy init): esse código só roda UMA vez,
  // na primeira renderização — evita reler o localStorage a cada render.
  const [ tema, setTema ] = useState(() => {
    // 1º) O usuário já escolheu um tema antes? Respeitamos a escolha dele.
    const salvo = localStorage.getItem('tema')
    if (salvo) return salvo

    // 2º) Nunca escolheu? Perguntamos ao SISTEMA operacional a preferência.
    // Atenção: a media query é 'prefers-color-scheme' (prefers, com R).
    const preferenciaEscuro = window.matchMedia('(prefers-color-scheme: dark)').matches
    if (preferenciaEscuro) return 'dark'

    // 3º) Padrão de fallback: tema claro.
    return 'light'
  })

  // Atualização de estado baseada no valor ANTERIOR (t) — jeito seguro
  // quando o novo valor depende do atual.
  function alternarTema() {
    setTema(t => (t === 'light' ? 'dark' : 'light'))
  }

  // useEffect = "efeito colateral": sincroniza o React com o mundo externo.
  // Sempre que `tema` muda, escrevemos no <html> (CSS lê via [data-theme])
  // e persistimos no localStorage. O array [tema] é a lista de dependências:
  // o efeito só re-executa quando `tema` mudar.
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', tema)
    localStorage.setItem('tema', tema)
  }, [tema])

  // Fragment (<>...</>) agrupa vários elementos sem criar uma <div> extra.
  return (
    <>
      {/* Header e Footer ficam FORA das rotas: aparecem em todas as páginas.
          Passamos o tema e a função de alternar como props para o Header. */}
      <Header tema={tema} aoAlternarTema={alternarTema} />

      {/* <Routes> escolhe UMA <Route> conforme a URL atual do navegador */}
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/materia/:id" element={<Materia />} /> {/* :id é um parâmetro dinâmico */}
        <Route path="/cadastro" element={<Cadastro />} />
        <Route path="/login" element={<Login />} />
        {/* Rota protegida: RotaProtegida decide se mostra o Painel
            ou redireciona para /login quando não há usuário logado. */}
        <Route path='/painel' element={
          <RotaProtegida>
            <Painel />
          </RotaProtegida>
        } />
        {/* As 3 rotas do fluxo de pagamento. /assinar exige login (RotaProtegida)
            porque é de lá que se DISPARA a compra. Já /assinatura/sucesso e
            /assinatura/cancelada são públicas de propósito: quem navega até
            elas é o PRÓPRIO STRIPE redirecionando o navegador (ver
            CheckoutService.criarSessao, setSuccessUrl/setCancelUrl no backend),
            e isso acontece ANTES de sabermos se o usuário ainda está "logado"
            no sentido do React — a página só precisa exibir uma mensagem. */}
        <Route path="/assinar" element={<RotaProtegida><Assinar /></RotaProtegida>} />
        <Route path="/assinatura/sucesso" element={<AssinaturaSucesso />} />
        <Route path="/assinatura/cancelada" element={<AssinaturaCancelada />} />
      </Routes>

      <Footer />
    </>
  )
}

export default App