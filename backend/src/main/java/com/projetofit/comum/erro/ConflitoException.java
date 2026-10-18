package com.projetofit.comum.erro;

/** Erro 409: conflito de estado ou dado duplicado. */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String detalhe) {
        super(detalhe);
    }
}
