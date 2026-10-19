package com.projetofit.sessao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessaoRepository extends JpaRepository<Sessao, UUID> {

    Optional<Sessao> findByClientId(UUID clientId);

    Page<Sessao> findByAlunoId(UUID alunoId, Pageable pageable);

    Optional<Sessao> findByIdAndAlunoId(UUID id, UUID alunoId);

    Optional<Sessao> findByIdAndAlunoPersonalId(UUID id, UUID personalId);

    /** Última sessão do aluno cuja divisão ainda existe e pertence à ficha. */
    Optional<Sessao> findFirstByAlunoIdAndDivisaoFichaIdOrderByFinalizadaEmDesc(UUID alunoId, UUID fichaId);

    @Query("select s.finalizadaEm from Sessao s where s.aluno.id = :alunoId "
            + "and s.finalizadaEm >= :de and s.finalizadaEm < :ate")
    List<Instant> findFinalizadasEmEntre(@Param("alunoId") UUID alunoId, @Param("de") Instant de,
                                         @Param("ate") Instant ate);

    @Query("select s.aluno.id, max(s.finalizadaEm) from Sessao s where s.aluno.personalId = :personalId "
            + "group by s.aluno.id")
    List<Object[]> ultimaSessaoPorAluno(@Param("personalId") UUID personalId);

    @Query("select s.aluno.id, count(s) from Sessao s where s.aluno.personalId = :personalId "
            + "and s.finalizadaEm >= :de and s.finalizadaEm < :ate group by s.aluno.id")
    List<Object[]> sessoesPorAlunoEntre(@Param("personalId") UUID personalId, @Param("de") Instant de,
                                        @Param("ate") Instant ate);
}
