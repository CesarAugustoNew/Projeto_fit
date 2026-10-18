package com.projetofit.aluno;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AlunoDTO(UUID id, String nome, String email, String telefone, LocalDate dataNascimento,
                       String objetivo, String observacoes, StatusAluno status, Instant criadoEm) {

    static AlunoDTO de(Aluno aluno) {
        return new AlunoDTO(aluno.getId(), aluno.getUsuario().getNome(), aluno.getUsuario().getEmail(),
                aluno.getTelefone(), aluno.getDataNascimento(), aluno.getObjetivo(), aluno.getObservacoes(),
                aluno.getStatus(), aluno.getCriadoEm());
    }
}
