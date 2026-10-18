package com.projetofit.ficha;

import com.projetofit.exercicio.Exercicio;
import com.projetofit.exercicio.GrupoMuscular;
import java.math.BigDecimal;
import java.util.UUID;

public record ItemDivisaoDTO(UUID id, UUID exercicioId, String exercicioNome, GrupoMuscular grupoMuscular, int ordem,
                             int series, String repeticoes, BigDecimal cargaKg, Integer descansoSeg,
                             String observacao) {

    static ItemDivisaoDTO de(ItemDivisao i) {
        Exercicio e = i.getExercicio();
        return new ItemDivisaoDTO(i.getId(), e.getId(), e.getNome(), e.getGrupoMuscular(), i.getOrdem(),
                i.getSeries(), i.getRepeticoes(), i.getCargaKg(), i.getDescansoSeg(), i.getObservacao());
    }
}
