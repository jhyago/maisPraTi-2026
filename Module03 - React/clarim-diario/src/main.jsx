// ============================================================
//   PONTO DE ENTRADA DA APLICAÇÃO (o "liga" do React)
// ============================================================
// Este é o PRIMEIRO arquivo JavaScript que roda no navegador.
// É aqui que o React "entra" na página HTML e assume o controle.

import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext.jsx'
import App from './App.jsx'

// CSS importado no JS: o Vite junta tudo no build final.
// A ordem importa — variables define as cores/tokens usados pelos outros.
import './styles/global.css'
import './styles/variables.css'
import './styles/forms.css'
import { GoogleOAuthProvider } from '@react-oauth/google'

// createRoot conecta o React à <div id="root"> que existe no index.html.
// Tudo que o React desenha vai "morar" dentro dessa div.
console.log("Client ID: ", import.meta.env.VITE_GOOGLE_CLIENT_ID)
createRoot(document.getElementById('root')).render(
  // Cada componente que ENVOLVE outro adiciona uma "camada" de recurso.
  // Pense em cebola: o App fica no centro, cercado por essas camadas.
  <StrictMode>            {/* modo de desenvolvimento: avisa sobre práticas arriscadas */}
    <GoogleOAuthProvider clientId={import.meta.env.VITE_GOOGLE_CLIENT_ID}>
        <BrowserRouter>       {/* habilita as rotas/URLs (react-router) na árvore toda */}
          <AuthProvider>      {/* disponibiliza o usuário logado para qualquer componente */}
            <App />           {/* nosso app de fato */}
          </AuthProvider>
      </BrowserRouter>
    </GoogleOAuthProvider>
  </StrictMode>
)
