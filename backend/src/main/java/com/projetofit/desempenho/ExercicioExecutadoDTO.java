package com.projetofit.desempenho;

import com.projetofit.exercicio.GrupoMuscular;
import java.util.UUID;

public record ExercicioExecutadoDTO(UUID id, String nome, GrupoMuscular grupoMuscular) {
}
