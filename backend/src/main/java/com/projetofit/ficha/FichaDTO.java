package com.projetofit.ficha;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record FichaDTO(UUID id, UUID alunoId, String nome, String objetivo, int frequenciaSemanal,
                       LocalDate dataInicio, LocalDate dataFim, StatusFicha status, boolean vencida,
                       LocalDate encerradaEm, List<DivisaoDTO> divisoes) {

    /** {@code encerradaEm} é a data, no fuso {@code zona}, em que a ficha foi encerrada ({@code null} se ativa). */
    public static FichaDTO de(Ficha f, LocalDate hoje, ZoneId zona) {
        return new FichaDTO(f.getId(), f.getAluno().getId(), f.getNome(), f.getObjetivo(), f.getFrequenciaSemanal(),
                f.getDataInicio(), f.getDataFim(), f.getStatus(), f.vencida(hoje),
                encerradaEm(f, zona),
                f.getDivisoes().stream()
                        .sorted(Comparator.comparingInt(Divisao::getOrdem))
                        .map(DivisaoDTO::de).toList());
    }

    static LocalDate encerradaEm(Ficha f, ZoneId zona) {
        return f.getEncerradaEm() == null ? null : LocalDate.ofInstant(f.getEncerradaEm(), zona);
    }
}
