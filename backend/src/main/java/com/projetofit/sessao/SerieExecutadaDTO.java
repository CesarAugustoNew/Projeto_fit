package com.projetofit.sessao;

import java.math.BigDecimal;
import java.util.UUID;

public record SerieExecutadaDTO(UUID exercicioId, String exercicioNome, UUID itemDivisaoId, int ordem,
                                int numeroSerie, int repeticoes, BigDecimal cargaKg) {

    public static SerieExecutadaDTO de(SerieExecutada s) {
        return new SerieExecutadaDTO(s.getExercicio().getId(), s.getExercicio().getNome(),
                s.getItemDivisao() == null ? null : s.getItemDivisao().getId(),
                s.getOrdem(), s.getNumeroSerie(), s.getRepeticoes(), s.getCargaKg());
    }
}
