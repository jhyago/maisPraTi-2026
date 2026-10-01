// ============================================================
//   SERVICE — camada que fala com a API das notícias
// ============================================================
// Boa prática: isolar as chamadas HTTP num "service". Assim os
// componentes não sabem NADA sobre axios/URLs — só chamam funções
// como listarNoticias(). Se a API mudar, mexemos só aqui.
import { api } from './api'
// ^ CORRIGIDO: este arquivo tinha sua PRÓPRIA instância do axios
// (`axios.create(...)` solta, sem o interceptor de token). Resultado:
// toda chamada feita por aqui saía SEM o cabeçalho Authorization — ou
// seja, mesmo um assinante pagante era tratado como visitante anônimo
// pelo backend (NoticiaService.temAcessoPremium via leitor == null), e
// NUNCA recebia o texto da matéria premium. Reaproveitar a instância
// `api` (a mesma usada no checkout) resolve isso: o token passa a ir
// em toda chamada de notícia também.

// async/await: a função "pausa" no await até a Promise resolver.
export async function listarNoticias() {
    const { data } = await api.get('/api/noticias') // desestrutura só o `data` da resposta
    return data
}

export async function buscarNoticia(id) {
    // PASSO 5 do fluxo de pagamento, do ponto de vista de QUEM LÊ: é esta
    // chamada que decide, matéria por matéria, se o texto vem completo ou
    // null (ver NoticiaResposta.java). O resultado depende do token ir
    // corretamente — por isso a correção acima importa tanto aqui.
    const { data } = await api.get(`/api/noticias/${id}`)
    return data
}
