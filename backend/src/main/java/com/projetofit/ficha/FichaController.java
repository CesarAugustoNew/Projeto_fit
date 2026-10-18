package com.projetofit.ficha;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('PERSONAL')")
public class FichaController {

    private final FichaService fichaService;

    public FichaController(FichaService fichaService) {
        this.fichaService = fichaService;
    }

    @GetMapping("/api/alunos/{alunoId}/fichas")
    public List<FichaResumoDTO> listarDoAluno(@PathVariable UUID alunoId) {
        return fichaService.listarDoAluno(alunoId);
    }

    @PostMapping("/api/alunos/{alunoId}/fichas")
    @ResponseStatus(HttpStatus.CREATED)
    public FichaDTO criar(@PathVariable UUID alunoId, @Valid @RequestBody FichaRequest req) {
        return fichaService.criar(alunoId, req);
    }

    @GetMapping("/api/fichas/{id}")
    public FichaDTO buscar(@PathVariable UUID id) {
        return fichaService.buscar(id);
    }
}
