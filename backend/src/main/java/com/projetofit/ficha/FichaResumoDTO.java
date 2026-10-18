package com.projetofit.ficha;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

public record FichaResumoDTO(UUID id, String nome, StatusFicha status, LocalDate dataInicio, LocalDate dataFim,
                             int frequenciaSemanal, int quantidadeDivisoes, boolean vencida,
                             LocalDate encerradaEm) {

    static FichaResumoDTO de(Ficha f, LocalDate hoje, ZoneId zona) {
        return new FichaResumoDTO(f.getId(), f.getNome(), f.getStatus(), f.getDataInicio(), f.getDataFim(),
                f.getFrequenciaSemanal(), f.getDivisoes().size(), f.vencida(hoje),
                FichaDTO.encerradaEm(f, zona));
    }
}
