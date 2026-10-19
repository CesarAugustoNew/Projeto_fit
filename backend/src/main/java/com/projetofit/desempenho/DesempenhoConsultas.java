package com.projetofit.desempenho;

import com.projetofit.sessao.SerieExecutada;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Consultas de leitura do desempenho; não grava nada. */
public interface DesempenhoConsultas extends Repository<SerieExecutada, UUID> {

    @Query("select new com.projetofit.desempenho.PontoEvolucaoBruto(s.id, s.finalizadaEm, max(se.cargaKg), "
            + "sum(se.repeticoes * coalesce(se.cargaKg, 0))) "
            + "from SerieExecutada se join se.sessao s "
            + "where s.aluno.id = :alunoId and se.exercicio.id = :exercicioId "
            + "and s.finalizadaEm >= :de and s.finalizadaEm < :ate "
            + "group by s.id, s.finalizadaEm order by s.finalizadaEm")
    List<PontoEvolucaoBruto> evolucao(@Param("alunoId") UUID alunoId, @Param("exercicioId") UUID exercicioId,
                                      @Param("de") Instant de, @Param("ate") Instant ate);

    @Query("select distinct new com.projetofit.desempenho.ExercicioExecutadoDTO(e.id, e.nome, e.grupoMuscular) "
            + "from SerieExecutada se join se.exercicio e "
            + "where se.sessao.aluno.id = :alunoId order by e.nome")
    List<ExercicioExecutadoDTO> exerciciosExecutados(@Param("alunoId") UUID alunoId);
}
