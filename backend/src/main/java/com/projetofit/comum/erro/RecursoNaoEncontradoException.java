package com.projetofit.comum.erro;

/** Erro 404: recurso inexistente. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String detalhe) {
        super(detalhe);
    }
}
