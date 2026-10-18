package com.projetofit.aluno;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record NovoAlunoRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 20) String telefone,
        @Past LocalDate dataNascimento,
        @Size(max = 500) String objetivo,
        @Size(max = 2000) String observacoes) {

    /** Remove espaços em volta do e-mail antes da validação: {@code @Email} recusaria " ana@teste.com ". */
    public NovoAlunoRequest {
        email = email == null ? null : email.trim();
    }
}
