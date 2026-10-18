package com.projetofit.ficha;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Corpo para criar ou editar uma ficha; a ordem das divisões é a posição na lista. */
public record FichaRequest(
        @NotBlank @Size(max = 120) String nome,
        @Size(max = 500) String objetivo,
        @NotNull @Min(1) @Max(7) Integer frequenciaSemanal,
        @NotNull LocalDate dataInicio,
        LocalDate dataFim,
        @NotEmpty @Size(max = 10) List<@NotNull @Valid DivisaoRequest> divisoes) {
}
