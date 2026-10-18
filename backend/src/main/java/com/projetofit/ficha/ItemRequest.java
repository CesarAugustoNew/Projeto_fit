package com.projetofit.ficha;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/** Um exercício da divisão; {@code id} só vem na edição de um item existente. */
public record ItemRequest(
        UUID id,
        @NotNull UUID exercicioId,
        @NotNull @Min(1) @Max(20) Integer series,
        @NotBlank @Pattern(regexp = "^\\d{1,3}(-\\d{1,3})?$",
                message = "Use um número (ex.: 10) ou uma faixa (ex.: 8-12).") String repeticoes,
        @DecimalMin("0") @Digits(integer = 4, fraction = 2) BigDecimal cargaKg,
        @Min(0) @Max(3600) Integer descansoSeg,
        @Size(max = 500) String observacao) {
}
