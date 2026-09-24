import axios from 'axios'

export const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080'
})

api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token')
    if(token) {
        config.headers.Authorization = `Bearer ${token}`
    }

    return config
})

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