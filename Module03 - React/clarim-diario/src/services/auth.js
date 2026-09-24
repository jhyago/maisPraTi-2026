import { api } from './api'

export async function login (email, senha) {
    const { data } = await api.post('/api/auth/login', { email, senha } )
    return data
}

function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('usuario')
    setUsuario(null)
}

export async function cadastrar(nome, email, senha) {
    await api.post('/api/usuarios', {nome, email, senha})
}