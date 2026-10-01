package com.clarim.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// @RestControllerAdvice = "um @ExceptionHandler para o projeto inteiro".
// Sem esta classe, uma exceção lançada num @Service sobe sem tratamento
// até o Spring, que devolve um 500 genérico (ou, pior: se o dispatch
// de erro cair numa rota que o Spring Security não libera, o cliente
// recebe um 403 que não tem NADA a ver com o problema real — foi
// exatamente isso que aconteceu ao testar o checkout).
//
// Com isto aqui, a exceção é convertida ANTES de chegar nesse caminho,
// então o status HTTP e a mensagem fazem sentido para quem programou o front.
@RestControllerAdvice
public class TratadorGlobalDeExcecoes {

    // Ex.: GET /api/noticias/999 → notícia não existe → 404, não 500.
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> tratarRecursoNaoEncontrado(RecursoNaoEncontradoException excecao) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErroResposta(excecao.getMessage()));
    }

    // Ex.: usuário já assinante clica em "Assinar" de novo → 409 CONFLICT,
    // o status HTTP que diz "seu pedido conflita com o estado atual do servidor".
    @ExceptionHandler(AssinaturaJaAtivaException.class)
    public ResponseEntity<ErroResposta> tratarAssinaturaJaAtiva(AssinaturaJaAtivaException excecao) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErroResposta(excecao.getMessage()));
    }
}
