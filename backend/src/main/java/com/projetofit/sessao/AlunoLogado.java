package com.projetofit.sessao;

import com.projetofit.aluno.Aluno;
import com.projetofit.aluno.AlunoRepository;
import com.projetofit.comum.erro.RecursoNaoEncontradoException;
import com.projetofit.usuario.UsuarioAtual;
import org.springframework.stereotype.Component;

/** Resolve o aluno dono do token da requisição atual. */
@Component
public class AlunoLogado {

    private final AlunoRepository alunoRepository;
    private final UsuarioAtual usuarioAtual;

    public AlunoLogado(AlunoRepository alunoRepository, UsuarioAtual usuarioAtual) {
        this.alunoRepository = alunoRepository;
        this.usuarioAtual = usuarioAtual;
    }

    public Aluno obter() {
        return alunoRepository.findByUsuarioId(usuarioAtual.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));
    }
}
