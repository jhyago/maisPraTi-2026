import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { buscarCep } from '../../services/viacep'
import { cadastrar } from '../../services/auth'

function Cadastro() {
  // UM estado-objeto para o formulário inteiro (em vez de 7 useState)
  const [form, setForm] = useState({
    nome: '', email: '', cep: '',
    logradouro: '', numero: '', bairro: '', cidade: '', uf: '',
  })
  const [aviso, setAviso] = useState('')   // mensagens de erro/status
  const navigate = useNavigate()

  // UMA função genérica atualiza QUALQUER campo:
  function atualizarCampo(e) {
    const { id, value } = e.target          // qual input disparou, e seu valor
    // Regra sagrada: NUNCA modifique o estado — crie um NOVO objeto.
    // ...f copia todos os campos atuais; [id]: value é uma "chave
    // computada": se id for "email", sobrescreve o campo email.
    setForm(f => ({ ...f, [id]: value }))
  }

  // onBlur do CEP: busca na ViaCEP e preenche o endereço NO ESTADO
  async function preencherEndereco() {
    if (!form.cep) return
    try {
      setAviso('Buscando CEP…')             // feedback de carregamento
      const end = await buscarCep(form.cep)
      setForm(f => ({
        ...f,
        logradouro: end.logradouro,
        bairro: end.bairro,
        cidade: end.localidade,             // atenção: a ViaCEP chama cidade de "localidade"
        uf: end.uf,
      }))
      setAviso('')
    } catch (erro) {
      setAviso(erro.message)                // "CEP não encontrado." etc.
    }
  }

  async function enviar(e) {
    e.preventDefault()                       // o velho conhecido!
    try {
      await cadastrar (form.nome, form.email, form.senha)
      navigate('/login')
    } catch (erro) {
      console.log(erro)
    }
  }

  return (
    <main className="container">
      {/* onSubmit no form = addEventListener('submit') do JS puro */}
      <form className="formulario" onSubmit={enviar}>
        <h1>Assine o Clarim</h1>

        <label htmlFor="nome">Nome completo</label>
        {/* O TRIO do componente controlado:
            id      → diz à atualizarCampo QUAL campo é
            value   → o input EXIBE o que está no estado
            onChange→ cada tecla ATUALIZA o estado */}
        <input id="nome" value={form.nome} onChange={atualizarCampo} required />

        <label htmlFor="email">E-mail</label>
        <input id="email" type="email" value={form.email} onChange={atualizarCampo} required />
        
        <label htmlFor="senha">Senha</label>
        <input type="password" id='senha' value={form.senha} onChange={atualizarCampo} minLength={8} required />

        <label htmlFor="cep">CEP</label>
        {/* onBlur: ao sair do campo, consulta a ViaCEP */}
        <input id="cep" value={form.cep} onChange={atualizarCampo}
               onBlur={preencherEndereco} placeholder="00000-000" required />

        <label htmlFor="logradouro">Rua</label>
        <input id="logradouro" value={form.logradouro} onChange={atualizarCampo} />

        <label htmlFor="bairro">Bairro</label>
        <input id="bairro" value={form.bairro} onChange={atualizarCampo} />

        <label htmlFor="cidade">Cidade</label>
        <input id="cidade" value={form.cidade} onChange={atualizarCampo} />

        <label htmlFor="uf">UF</label>
        <input id="uf" value={form.uf} onChange={atualizarCampo} maxLength={2} />

        {/* renderização condicional: o aviso só existe se houver texto */}
        {aviso && <p className="aviso">{aviso}</p>}

        <button type="submit">Assinar</button>
      </form>
    </main>
  )
}

export default Cadastro