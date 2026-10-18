package com.projetofit.ficha;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FichaDTO(UUID id, UUID alunoId, String nome, String objetivo, int frequenciaSemanal,
                       LocalDate dataInicio, LocalDate dataFim, StatusFicha status, boolean vencida,
                       List<DivisaoDTO> divisoes) {

    static FichaDTO de(Ficha f, LocalDate hoje) {
        return new FichaDTO(f.getId(), f.getAluno().getId(), f.getNome(), f.getObjetivo(), f.getFrequenciaSemanal(),
                f.getDataInicio(), f.getDataFim(), f.getStatus(), f.vencida(hoje),
                f.getDivisoes().stream().map(DivisaoDTO::de).toList());
    }
}
