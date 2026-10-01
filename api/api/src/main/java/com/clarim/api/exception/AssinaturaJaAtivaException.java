package com.clarim.api.exception;

// Exceção de REGRA DE NEGÓCIO, não de erro técnico: o usuário não fez nada
// errado, só tentou comprar algo que já tem. Por isso ela existe separada
// da RecursoNaoEncontradoException — cada uma vira um HTTP status diferente
// lá no TratadorGlobalDeExcecoes (esta aqui vira 409 CONFLICT).
public class AssinaturaJaAtivaException extends RuntimeException {
    public AssinaturaJaAtivaException(String mensagem) {
        super(mensagem);
    }
}
