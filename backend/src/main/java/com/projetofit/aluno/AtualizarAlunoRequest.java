package com.projetofit.aluno;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AtualizarAlunoRequest(
        @NotBlank @Size(max = 120) String nome,
        @Size(max = 20) String telefone,
        @Past LocalDate dataNascimento,
        @Size(max = 500) String objetivo,
        @Size(max = 2000) String observacoes) {
}
