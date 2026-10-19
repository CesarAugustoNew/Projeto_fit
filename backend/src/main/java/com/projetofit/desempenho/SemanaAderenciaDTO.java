package com.projetofit.desempenho;

import java.time.LocalDate;

public record SemanaAderenciaDTO(LocalDate inicio, LocalDate fim, int realizados, int planejado,
                                 Integer percentual) {
}
