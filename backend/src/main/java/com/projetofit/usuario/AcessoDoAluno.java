package com.projetofit.usuario;

import java.util.UUID;

/**
 * Decide se um usuário com papel {@link Papel#ALUNO} pode acessar a API. Implementado pelo módulo de alunos, para
 * que o pacote {@code usuario} não dependa dele.
 */
public interface AcessoDoAluno {

    /** {@code false} quando o aluno não pode acessar (sem cadastro ou inativo). */
    boolean liberar(UUID usuarioId);
}
