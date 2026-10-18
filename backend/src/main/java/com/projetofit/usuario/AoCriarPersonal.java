package com.projetofit.usuario;

import java.util.UUID;

/**
 * Ouvinte da criação do personal. Chamado por {@link BootstrapPersonal} dentro da mesma transação que grava o
 * usuário: uma exceção aqui desfaz a criação.
 */
public interface AoCriarPersonal {

    void aoCriarPersonal(UUID personalId);
}
