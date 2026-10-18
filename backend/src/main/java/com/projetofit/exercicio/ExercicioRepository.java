package com.projetofit.exercicio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExercicioRepository extends JpaRepository<Exercicio, UUID> {

    List<Exercicio> findByPersonalIdAndAtivoOrderByNomeAsc(UUID personalId, boolean ativo);

    List<Exercicio> findByPersonalIdAndAtivoAndGrupoMuscularOrderByNomeAsc(
            UUID personalId, boolean ativo, GrupoMuscular grupoMuscular);

    Optional<Exercicio> findByIdAndPersonalId(UUID id, UUID personalId);

    boolean existsByPersonalIdAndNomeIgnoreCase(UUID personalId, String nome);

    boolean existsByPersonalIdAndNomeIgnoreCaseAndIdNot(UUID personalId, String nome, UUID id);

    long countByPersonalId(UUID personalId);
}
