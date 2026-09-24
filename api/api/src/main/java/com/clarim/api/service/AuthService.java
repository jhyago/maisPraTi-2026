package com.clarim.api.service;

import com.clarim.api.dto.LoginResposta;
import com.clarim.api.model.Papel;
import com.clarim.api.model.Provider;
import com.clarim.api.model.Usuario;
import com.clarim.api.repository.UsuarioRepository;
import com.clarim.api.security.GoogleTokenService;
import com.clarim.api.security.JwtService;
import com.clarim.api.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

@Service
public class AuthService {
    private final GoogleTokenService googleTokenService;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public AuthService(GoogleTokenService googleTokenService, UsuarioRepository usuarioRepository, JwtService jwtService) {
        this.googleTokenService = googleTokenService;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    public LoginResposta loginGoogle(String idToken) {
        GoogleIdToken.Payload dados = googleTokenService.validar(idToken);

        String sub = dados.getSubject();
        String email = dados.getEmail();
        String nome = (String) dados.get("name");
        boolean emailVerificado = Boolean.TRUE.equals(dados.getEmailVerified());

        Usuario usuario = usuarioRepository.findByProviderAndProviderId(Provider.GOOGLE, sub)
                .orElseGet(() -> vincularOuCriar(sub, email, nome, emailVerificado));

        String token = jwtService.gerarToken(new UsuarioAutenticado(usuario));
        return new LoginResposta(token, usuario.getNome(), usuario.getPapel().name());

    }

    private Usuario vincularOuCriar(String sub, String email, String nome, boolean emailVerificado) {
        var existente = usuarioRepository.findByEmail(email);

        if (existente.isPresent()) {
            Usuario usuario = existente.get();
            usuario.setProviderId(sub);
            return usuario;
        }

        Usuario novo = new Usuario();
        novo.setNome(nome);
        novo.setEmail(email);
        novo.setSenhaHash(null);
        novo.setPapel(Papel.LEITOR);
        novo.setProvider(Provider.GOOGLE);
        novo.setProviderId(sub);
        return usuarioRepository.save(novo);
    }
}
