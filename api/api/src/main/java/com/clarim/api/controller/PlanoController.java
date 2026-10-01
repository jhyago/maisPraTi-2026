package com.clarim.api.controller;

import com.clarim.api.dto.PlanoResposta;
import com.clarim.api.repository.PlanoRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// PASSO 0 do fluxo: antes de checkout existir, o React precisa saber
// QUAIS planos oferecer na tela /assinar. Esta rota é pública de propósito
// (ver ConfiguracaoSeguranca: GET /api/planos → permitAll) — mostrar preço
// não exige estar logado.
@RestController
@RequestMapping("/api/planos")
public class PlanoController {

    private final PlanoRepository repository;

    public PlanoController(PlanoRepository repository) {
        this.repository = repository;
    }

    // GET /api/planos — público. Uma leitura simples, sem regra de
    // negócio: por isso fala direto com o repositório.
    @GetMapping
    public List<PlanoResposta> listar() {
        // findByAtivoTrue() devolve TODOS os planos ativos (ex.: Mensal e
        // Anual) — por isso é List<Plano>, não Optional<Plano>. Um
        // Optional aqui faria o Spring Data gerar uma query de "resultado
        // único" e explodir (NonUniqueResultException) assim que
        // houvesse mais de um plano ativo no banco.
        return repository.findByAtivoTrue().stream()
                .map(p -> new PlanoResposta(
                        p.getId(), p.getNome(),
                        p.getPrecoFormatado(),   // o método de conveniência da entidade
                        p.getIntervalo().name()))
                .toList();
    }
}
