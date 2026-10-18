package com.projetofit.exercicio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NovoExercicioRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotNull GrupoMuscular grupoMuscular,
        @Size(max = 500)
        @Pattern(regexp = "^https?://\\S+$", flags = Pattern.Flag.CASE_INSENSITIVE, message = "Informe um link começando com http:// ou https://.")
        String videoUrl,
        @Size(max = 1000) String observacao) {
}
