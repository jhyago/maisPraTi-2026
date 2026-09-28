package com.clarim.api.service;

import com.clarim.api.model.Papel;
import com.clarim.api.model.Provider;
import com.clarim.api.model.Usuario;
import com.clarim.api.repository.UsuarioRepository;
import com.clarim.api.security.DadosGoogle;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GoogleAuthService {
    private final UsuarioRepository usuarioRepository;

    public GoogleAuthService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario entrarOuCadastrar(DadosGoogle google) {
        if(!google.emailVerified()) {
            throw new IllegalArgumentException("E-mail Google não verificado");
        }

        return usuarioRepository.findByProviderId(google.id())
                                .or(() -> vincularPorEmail(google))
                                .orElseGet(() -> criarConta(google));
    }

    private Optional<Usuario> vincularPorEmail(DadosGoogle google) {
        return usuarioRepository.findByEmail(google.email())
                .map(usuario -> {
                    usuario.setProviderId(google.id());
                    return usuario;
                });
    }

    private Usuario criarConta(DadosGoogle google) {
        Usuario usuario = new Usuario();
        usuario.setEmail(google.email());
        usuario.setNome(google.nome());
        usuario.setSenhaHash(null);
        usuario.setProvider(Provider.GOOGLE);
        usuario.setProviderId(google.id());
        usuario.setPapel(Papel.LEITOR);
        return usuarioRepository.save(usuario);
    }
}


