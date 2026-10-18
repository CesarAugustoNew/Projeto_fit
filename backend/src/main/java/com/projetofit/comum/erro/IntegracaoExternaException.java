package com.projetofit.comum.erro;

/** Erro 502: erro ao se comunicar com um serviço externo. */
public class IntegracaoExternaException extends RuntimeException {

    public IntegracaoExternaException(String detalhe, Throwable causa) {
        super(detalhe, causa);
    }
}
