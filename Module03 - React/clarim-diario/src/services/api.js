import axios from 'axios'

// A ÚNICA instância do axios que deveria existir no projeto. Por quê só
// uma? Porque o checkout (e o paywall de notícias premium) só funcionam
// se o token JWT for enviado em TODA chamada autenticada — e isso é feito
// pelo interceptor abaixo, que só existe NESTA instância. Qualquer service
// que criar seu próprio `axios.create(...)` separado (como o antigo
// services/noticias.js fazia) perde esse interceptor e passa a chamar a
// API "deslogado" mesmo com o usuário logado.
export const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080'
})

// Roda ANTES de toda requisição sair. Se existe um token salvo (login
// feito), ele é anexado automaticamente no cabeçalho Authorization — é
// assim que o backend sabe QUEM está chamando (ver JwtAuthenticationFilter
// e @AuthenticationPrincipal no Java). Sem token, a requisição ainda sai,
// só que sem esse cabeçalho — vira uma chamada "anônima" do ponto de vista
// do Spring Security.
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token')
    if(token) {
        config.headers.Authorization = `Bearer ${token}`
    }

    return config
})

// Roda em TODA resposta, com sucesso ou erro. Aqui tratamos só o caso de
// token expirado/invalido: o backend responde 401 ("não autenticado"),
// e nós aproveitamos para limpar a sessão local e mandar o usuário pro
// login — em vez de deixar a tela presa num estado "logado só na aparência".
api.interceptors.response.use(
    (resposta) => resposta,
    (erro) => {
        if(erro.response?.status === 401) {
            localStorage.removeItem('token')
            localStorage.removeItem('usuario')
            window.location.href = "/login"
        }

        return Promise.reject(erro)
    }
)
