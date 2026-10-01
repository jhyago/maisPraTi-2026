package com.clarim.api.controller;

import com.clarim.api.dto.NoticiaRequest;
import com.clarim.api.dto.NoticiaResponse;
import com.clarim.api.dto.NoticiaResposta;
import com.clarim.api.dto.NoticiaResumo;
import com.clarim.api.model.Usuario;
import com.clarim.api.security.UsuarioAutenticado;
import com.clarim.api.service.NoticiaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/noticias")
public class NoticiaController {

    private final NoticiaService noticiaService;

    public NoticiaController(NoticiaService noticiaService) {
        this.noticiaService = noticiaService;
    }

    @GetMapping
    public List<NoticiaResumo> listar() {
        return noticiaService.listarTodas();
    }

    @GetMapping("/{id}")
    public NoticiaResposta buscarPorId(
            @PathVariable Long id,
            // Numa rota PÚBLICA, quem acessa sem token não tem identidade.
            // Nesse caso o @AuthenticationPrincipal entrega null, e não erro.
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {

        Usuario leitor = (autenticado != null) ? autenticado.getUsuario() : null;
        return noticiaService.buscarPorId(id, leitor);
    }

    @PostMapping("/criar")
    public ResponseEntity<NoticiaResponse> criar(@RequestBody @Valid NoticiaRequest noticia) {
        NoticiaResponse criada = noticiaService.criar(noticia);
        return ResponseEntity.ok(criada);
    }
}