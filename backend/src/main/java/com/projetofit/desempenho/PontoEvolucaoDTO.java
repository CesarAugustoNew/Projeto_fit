package com.projetofit.desempenho;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PontoEvolucaoDTO(LocalDate data, UUID sessaoId, BigDecimal cargaMaxima, BigDecimal volume) {
}
