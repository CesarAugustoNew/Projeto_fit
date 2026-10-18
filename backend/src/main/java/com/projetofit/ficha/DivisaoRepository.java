package com.projetofit.ficha;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DivisaoRepository extends JpaRepository<Divisao, UUID> {
}
