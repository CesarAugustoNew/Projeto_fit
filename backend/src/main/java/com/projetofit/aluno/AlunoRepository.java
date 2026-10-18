package com.projetofit.aluno;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlunoRepository extends JpaRepository<Aluno, UUID> {

    @EntityGraph(attributePaths = "usuario")
    List<Aluno> findByPersonalIdAndStatusInOrderByUsuarioNomeAsc(UUID personalId, Collection<StatusAluno> status);

    @EntityGraph(attributePaths = "usuario")
    Optional<Aluno> findByIdAndPersonalId(UUID id, UUID personalId);

    Optional<Aluno> findByUsuarioId(UUID usuarioId);
}
