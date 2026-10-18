package com.projetofit.sessao;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoRepository extends JpaRepository<Sessao, UUID> {

    Optional<Sessao> findByClientId(UUID clientId);

    Page<Sessao> findByAlunoIdOrderByFinalizadaEmDesc(UUID alunoId, Pageable pageable);

    Optional<Sessao> findByIdAndAlunoId(UUID id, UUID alunoId);

    Optional<Sessao> findByIdAndAlunoPersonalId(UUID id, UUID personalId);

    /** Última sessão do aluno cuja divisão ainda existe e pertence à ficha. */
    Optional<Sessao> findFirstByAlunoIdAndDivisaoFichaIdOrderByFinalizadaEmDesc(UUID alunoId, UUID fichaId);
}
