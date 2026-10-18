package com.projetofit.aluno;

import com.projetofit.usuario.AcessoDoAluno;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decide se um usuário com papel {@code ALUNO} pode acessar a API, conforme o status do seu cadastro. O primeiro
 * acesso de um aluno {@link StatusAluno#CONVIDADO} (convite aceito) o torna {@link StatusAluno#ATIVO}.
 */
@Component
public class AlunoAcesso implements AcessoDoAluno {

    private final AlunoRepository alunoRepository;

    public AlunoAcesso(AlunoRepository alunoRepository) {
        this.alunoRepository = alunoRepository;
    }

    /** {@code false} sem cadastro de aluno ou com aluno {@link StatusAluno#INATIVO}. */
    @Override
    @Transactional
    public boolean liberar(UUID usuarioId) {
        return alunoRepository.findByUsuarioId(usuarioId)
                .map(aluno -> switch (aluno.getStatus()) {
                    case INATIVO -> false;
                    case ATIVO -> true;
                    case CONVIDADO -> {
                        aluno.registrarPrimeiroAcesso();
                        yield true;
                    }
                })
                .orElse(false);
    }
}
