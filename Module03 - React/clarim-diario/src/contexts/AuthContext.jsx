// ============================================================
//   CONTEXT API — compartilhando o "usuário logado" com o app todo
// ============================================================
// Problema que o Context resolve: sem ele, para o Header saber quem
// está logado, teríamos que passar o `usuario` por props através de
// TODOS os componentes no caminho (o chamado "prop drilling").
// O Context cria um "canal direto": quem quiser o dado, se inscreve.

import { createContext, useContext, useState } from "react";
import { login as loginNaApi, loginComGoogle as loginGoogle} from '../services/auth'
// createContext cria o "canal". O valor null é o padrão caso alguém
// tente ler o contexto sem um Provider por cima (tratamos isso no useAuth).
const AuthContext = createContext(null);

// O Provider é o componente que GUARDA o estado e o DISPONIBILIZA.
// `children` são todos os componentes que ele envolve (veja o main.jsx).
export function AuthProvider({ children }) {
    // Lazy init: tentamos "reidratar" o usuário salvo no localStorage,
    // para que o login sobreviva a um F5 (refresh) da página.
    const [usuario, setUsuario] = useState(() => {
        const salvo = localStorage.getItem('usuario')
        return salvo ? JSON.parse(salvo) : null   // JSON.parse: texto → objeto
    })

    // ⚠️ Didático: em produção NUNCA valide senha no front-end assim.
    // A verificação de credenciais deve acontecer no back-end/API.
    //
    // RELEVANTE PARA O CHECKOUT: é o `dados.token` salvo aqui que o
    // interceptor de services/api.js vai anexar em TODA chamada futura —
    // inclusive POST /api/assinaturas/checkout. Sem ter passado por aqui
    // (ou por iniciarSessao, no caso do login via Google) antes, o botão
    // "Assinar" chamaria a API sem token e receberia 401/403.
    async function login(email, senha) {
        const dados = await loginNaApi(email, senha)

        localStorage.setItem('token', dados.token)
        localStorage.setItem('usuario', JSON.stringify({
            nome: dados.nome,
            papel: dados.papel
        }))

        setUsuario({ nome: dados.nome, papel: dados.papel})
    }

    // Mesma ideia do login() de senha, só que reaproveitada pelo fluxo do
    // Google (loginComGoogle abaixo) — por isso foi extraída: os dois
    // caminhos terminam gravando o MESMO par token/usuario no localStorage.
    function iniciarSessao(dados) {
        localStorage.setItem('token', dados.token)
        const resumo = { nome: dados.nome, papel: dados.papel }
        localStorage.setItem('usuario', JSON.stringify(resumo))
        setUsuario(resumo)
    }

    async function loginComGoogle(credential) {
        iniciarSessao(await loginGoogle(credential))
    }

    function logout() {
        setUsuario(null)
        localStorage.removeItem('usuario')
        // CORRIGIDO: faltava remover o 'token'. Sem esta linha, o JWT antigo
        // continuava salvo no localStorage, e o interceptor do services/api.js
        // (que lê exatamente a chave 'token') seguia anexando
        // `Authorization: Bearer <token antigo>` em TODA requisição seguinte —
        // inclusive um checkout — mesmo com a tela mostrando "deslogado".
        // Numa máquina compartilhada, a pessoa seguinte a usar o site podia
        // acabar autenticada como quem saiu, até o token expirar (60 min).
        localStorage.removeItem('token')
    }

    // value = o "pacote" que fica disponível para quem consumir o contexto.
    return (
        <AuthContext.Provider value={{ usuario, login, logout, loginComGoogle }}>
            {children}
        </AuthContext.Provider>
    )
}

// Hook customizado: em vez de cada componente chamar useContext(AuthContext),
// eles chamam useAuth(). Além de mais legível, centralizamos aqui a checagem
// de uso correto (evita bugs difíceis quando falta o Provider).
export function useAuth() {
    const contexto = useContext(AuthContext)
    if (!contexto) {
        throw new Error('useAuth deve ser usado dentro de <AuthProvider>')
    }

    return contexto
}
