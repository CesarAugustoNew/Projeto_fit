package com.projetofit.sessao;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessaoController {

    private final SessaoService sessaoService;

    public SessaoController(SessaoService sessaoService) {
        this.sessaoService = sessaoService;
    }

    /** 201 quando a sessão é gravada agora; 200 quando o mesmo {@code clientId} já tinha sido registrado. */
    @PostMapping("/api/sessoes")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<SessaoDTO> registrar(@Valid @RequestBody SessaoRequest req) {
        SessaoService.Resultado resultado = sessaoService.registrar(req);
        return ResponseEntity.status(resultado.criada() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(resultado.sessao());
    }
}
