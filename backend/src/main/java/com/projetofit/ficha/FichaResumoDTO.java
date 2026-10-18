package com.projetofit.ficha;

import java.time.LocalDate;
import java.util.UUID;

public record FichaResumoDTO(UUID id, String nome, StatusFicha status, LocalDate dataInicio, LocalDate dataFim,
                             int frequenciaSemanal, int quantidadeDivisoes, boolean vencida) {

    static FichaResumoDTO de(Ficha f, LocalDate hoje) {
        return new FichaResumoDTO(f.getId(), f.getNome(), f.getStatus(), f.getDataInicio(), f.getDataFim(),
                f.getFrequenciaSemanal(), f.getDivisoes().size(), f.vencida(hoje));
    }
}
