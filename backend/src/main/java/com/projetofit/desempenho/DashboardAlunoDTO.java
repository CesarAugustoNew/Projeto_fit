package com.projetofit.desempenho;

import java.util.UUID;

public record DashboardAlunoDTO(UUID alunoId, String nome, int realizadosSemana, int planejadoSemana,
                                Integer diasSemTreinar, UUID fichaAtivaId, boolean fichaVencida, boolean sumiu) {
}
