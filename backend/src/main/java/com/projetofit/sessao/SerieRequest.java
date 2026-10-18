package com.projetofit.sessao;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** Uma série feita no treino; {@code itemDivisaoId} liga a série ao item da ficha, quando ele ainda existe. */
public record SerieRequest(
        UUID itemDivisaoId,
        @NotNull UUID exercicioId,
        @NotNull @Min(1) @Max(50) Integer numeroSerie,
        @NotNull @Min(0) @Max(999) Integer repeticoes,
        @DecimalMin("0") @Digits(integer = 4, fraction = 2) BigDecimal cargaKg) {
}
