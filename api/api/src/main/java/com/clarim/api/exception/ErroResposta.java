package com.clarim.api.exception;

// O "formato padrão" de erro que a API devolve para o React.
// Por que um record com um campo só? Porque o front (veja Assinar.jsx)
// já espera ler `erro.response.data.mensagem` — então o contrato é
// combinado dos dois lados: o nome do campo aqui PRECISA ser "mensagem".
public record ErroResposta(String mensagem) {
}
