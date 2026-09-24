package com.clarim.api.controller;

import com.clarim.api.dto.GoogleLoginRequest;
import com.clarim.api.dto.LoginRequest;
import com.clarim.api.dto.LoginResposta;
import com.clarim.api.security.JwtService;
import com.clarim.api.security.UsuarioAutenticado;
import com.clarim.api.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")

public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthService authService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, AuthService authService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResposta login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.senha()));

            UsuarioAutenticado usuarioAutenticado = (UsuarioAutenticado) authentication.getPrincipal();

            String token = jwtService.gerarToken(usuarioAutenticado);

            return new LoginResposta(
                    token,
                    usuarioAutenticado.getUsuario().getNome(),
                    usuarioAutenticado.getUsuario().getPapel().name()
            );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao autenticar usuário");
        }
    }

    @PostMapping("/google")
    public LoginResposta loginGoogle(@RequestBody GoogleLoginRequest googleLoginRequest) {
        return authService.loginGoogle(googleLoginRequest.credential());

    }
}
