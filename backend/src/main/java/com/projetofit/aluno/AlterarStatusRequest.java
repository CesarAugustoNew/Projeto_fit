package com.projetofit.aluno;

import jakarta.validation.constraints.NotNull;

public record AlterarStatusRequest(@NotNull StatusAluno status) {
}
