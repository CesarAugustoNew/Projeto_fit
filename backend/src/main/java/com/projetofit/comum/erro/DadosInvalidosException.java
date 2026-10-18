package com.projetofit.comum.erro;

/** Erro 400: um campo é inválido por uma regra que a validação do request não alcança (ex.: depende do banco). */
public class DadosInvalidosException extends RuntimeException {

    private final String campo;

    public DadosInvalidosException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
