package com.projetofit.ficha;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FichaRepository extends JpaRepository<Ficha, UUID> {

    Optional<Ficha> findByIdAndAlunoPersonalId(UUID id, UUID personalId);

    List<Ficha> findByAlunoIdOrderByDataInicioDescCriadoEmDesc(UUID alunoId);

    Optional<Ficha> findByAlunoIdAndStatus(UUID alunoId, StatusFicha status);

    List<Ficha> findByAlunoPersonalIdAndStatus(UUID personalId, StatusFicha status);

    /** Fichas dos alunos do personal que não foram encerradas antes de {@code desde}. */
    @Query("select f from Ficha f where f.aluno.personalId = :personalId "
            + "and (f.encerradaEm is null or f.encerradaEm >= :desde)")
    List<Ficha> findNaoEncerradasAntesDe(@Param("personalId") UUID personalId, @Param("desde") Instant desde);
}
