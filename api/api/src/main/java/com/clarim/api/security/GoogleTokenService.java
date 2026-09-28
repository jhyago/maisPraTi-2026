package com.clarim.api.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoogleTokenService {
    private final GoogleIdTokenVerifier verificador;

    public GoogleTokenService(@Value("${google.client-id}") String clientId) {
        this.verificador = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(List.of(clientId))
                .build();
    }

    public DadosGoogle validar(String idToken) {
        try {
            GoogleIdToken token = verificador.verify(idToken);
            if(token == null) {
                throw new IllegalArgumentException("Token inválido");
            }

            GoogleIdToken.Payload claims = token.getPayload();

            return new DadosGoogle(
                    claims.getSubject(),
                    claims.getEmail(),
                    Boolean.TRUE.equals(claims.getEmailVerified()),
                    (String) claims.get("name"),
                    (String) claims.get("picture")
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Token inválido");
        }
    }
}
