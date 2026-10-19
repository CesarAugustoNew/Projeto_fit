package com.projetofit.ficha;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FichaRepository extends JpaRepository<Ficha, UUID> {

    Optional<Ficha> findByIdAndAlunoPersonalId(UUID id, UUID personalId);

    List<Ficha> findByAlunoIdOrderByDataInicioDescCriadoEmDesc(UUID alunoId);

    Optional<Ficha> findByAlunoIdAndStatus(UUID alunoId, StatusFicha status);

    List<Ficha> findByAlunoPersonalIdAndStatus(UUID personalId, StatusFicha status);
}
