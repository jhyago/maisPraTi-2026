// ============================================================
//   PÁGINA LOGIN — formulário controlado + autenticação
// ============================================================
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'

function Login() {
    // Aqui usamos UM useState por campo (compare com o Cadastro.jsx, que
    // usa um único estado-objeto). Ambos funcionam; este é mais simples
    // quando há poucos campos.
    const [email, setEmail] = useState('')
    const [senha, setSenha] = useState('')
    const [aviso, setAviso] = useState('')

    // Pegamos a função `login` do contexto de autenticação.
    const { login } = useAuth()

    // useNavigate devolve uma função para mudar de página VIA CÓDIGO
    // (sem o usuário clicar num link).
    const navigate = useNavigate()

    async function enviar(e) {
        e.preventDefault()   // impede o recarregamento padrão do <form>
        try {
            await login(email, senha)  // pode lançar erro se as credenciais falharem
            navigate('/')        // deu certo → volta para a capa
        } catch (erro) {
            setAviso(erro.message)  // exibe a mensagem de erro na tela
        }
    }

    return (
        <main className='container'>
            {/* onSubmit dispara ao enviar o form (botão submit ou Enter) */}
            <form className='formulario' onSubmit={enviar}>
                <h1>Entrar no Clarim</h1>
                <label htmlFor="email">E-mail</label>
                {/* Input controlado: value vem do estado e onChange atualiza o estado */}
                <input type="email" id='email' value={email} onChange={(e) => setEmail(e.target.value)} required />

                <label htmlFor="senha">Senha</label>
                <input type="password" id='senha' value={senha} onChange={(e) => setSenha(e.target.value)} required />

                {/* Renderização condicional: o aviso só aparece se houver texto */}
                {aviso && <p className='aviso'>{aviso}</p>}

                <button type='submit'>Entrar</button>

                <p className='rodape-form'>
                    Ainda não é assinante? <Link to='/cadastro'>Assine o Clarim</Link>
                </p>
            </form>
        </main>
    )
}

export default Login