package com.projetofit.sessao;

import java.time.Instant;
import java.util.UUID;

/** Linha do histórico; {@code quantidadeExercicios} conta exercícios distintos. */
public record SessaoResumoDTO(UUID id, String divisaoNome, Instant iniciadaEm, Instant finalizadaEm,
                              int quantidadeExercicios, int quantidadeSeries) {

    public static SessaoResumoDTO de(Sessao s) {
        int exercicios = (int) s.getSeries().stream().map(serie -> serie.getExercicio().getId()).distinct().count();
        return new SessaoResumoDTO(s.getId(), s.getDivisaoNome(), s.getIniciadaEm(), s.getFinalizadaEm(),
                exercicios, s.getSeries().size());
    }
}
