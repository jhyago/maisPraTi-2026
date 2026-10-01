// ============================================================
//   HEADER — cabeçalho fixo do jornal (navegação, sessão, tema)
// ============================================================
import { Link } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import './Header.css'

// Recebe `tema` e `aoAlternarTema` por PROPS, vindos do App.jsx.
// Repare: o Header não CONTROLA o tema, apenas EXIBE e AVISA quando
// o usuário quer trocar — o estado "mora" no App ("lifting state up").
function Header({ tema, aoAlternarTema }) {

  // Já a informação de sessão vem do CONTEXTO (não por props), pois
  // vários componentes precisam dela em pontos distantes da árvore.
  const { usuario, logout } = useAuth()

  // Data de hoje formatada em português para dar clima de jornal impresso.
  const hoje = new Date().toLocaleDateString('pt-BR', {
    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
  })

  return (
    <header className="cabecalho">
      <div className="cabecalho__faixa">
        <span>Edição de Nova York</span>
        <span>{hoje}</span>

        {/* Operador ternário: SE há usuário, mostra saudação + Sair;
            SENÃO, mostra o link de Entrar. */}
        {usuario ? (
          <span className='cabecalho__sessao'>
            Olá, {usuario.nome} -  
            <Link to='/painel'> Painel </Link>
            <button className='cabecalho__sair' onClick={logout}> - Sair</button>
          </span>
        ) : (
          <Link to='/login' className='cabecalho__entrar'>Entrar</Link>
        )}

        {/* onClick chama a função recebida por prop; quem realmente
            troca o tema é o App. O texto do botão reflete o tema atual. */}
        <button className="cabecalho__tema" onClick={aoAlternarTema}>
          {tema === 'light' ? '🌙 Escuro' : '☀️ Claro'}
        </button>
      </div>

      <Link to="/" className="cabecalho__logo-link">
        <h1 className="cabecalho__titulo">O CLARIM DIÁRIO</h1>
      </Link>
      <p className="cabecalho__lema">A verdade doa a quem doer — inclusive a certos aracnídeos</p>

      <nav className="cabecalho__menu">
        <Link to="/">Capa</Link>
        <a href="#">Cidade</a>
        <a href="#">Ameaças Urbanas</a>
        <a href="#">Opinião do Editor</a>
        {/* CORRIGIDO: apontava para /cadastro (criar CONTA grátis) — a
            página de planos/pagamento é /assinar (ver App.jsx). Com o link
            errado, não existia NENHUM caminho de navegação até o checkout;
            era preciso digitar a URL manualmente. */}
        <Link to="/assinar">Assine</Link>
      </nav>
    </header>
  )
}

export default Header