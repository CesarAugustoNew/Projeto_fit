package com.projetofit.sessao;

import com.projetofit.ficha.FichaDTO;
import java.util.UUID;

public record FichaAtivaDTO(FichaDTO ficha, UUID proximaDivisaoId) {
}
