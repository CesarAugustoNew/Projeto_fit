package com.projetofit.exercicio;

import java.util.UUID;

public record ExercicioDTO(UUID id, String nome, GrupoMuscular grupoMuscular, String videoUrl, String observacao,
                           boolean ativo) {

    static ExercicioDTO de(Exercicio e) {
        return new ExercicioDTO(e.getId(), e.getNome(), e.getGrupoMuscular(), e.getVideoUrl(), e.getObservacao(),
                e.isAtivo());
    }
}
