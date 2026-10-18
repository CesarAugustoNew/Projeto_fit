package com.projetofit.sessao;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ALUNO')")
public class TreinoDoAlunoController {

    private final TreinoDoAlunoService treinoDoAlunoService;

    public TreinoDoAlunoController(TreinoDoAlunoService treinoDoAlunoService) {
        this.treinoDoAlunoService = treinoDoAlunoService;
    }

    /** 200 com a ficha ativa, ou 204 quando o aluno não tem ficha ativa. */
    @GetMapping("/api/me/ficha-ativa")
    public ResponseEntity<FichaAtivaDTO> fichaAtiva() {
        return treinoDoAlunoService.fichaAtiva()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
