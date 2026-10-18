package com.projetofit.aluno;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alunos")
@PreAuthorize("hasRole('PERSONAL')")
public class AlunoController {

    private final AlunoService alunoService;

    public AlunoController(AlunoService alunoService) {
        this.alunoService = alunoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlunoDTO criar(@Valid @RequestBody NovoAlunoRequest req) {
        return alunoService.criar(req);
    }

    @GetMapping
    public List<AlunoDTO> listar(@RequestParam(required = false) StatusAluno status) {
        return alunoService.listar(status);
    }

    @GetMapping("/{id}")
    public AlunoDTO buscar(@PathVariable UUID id) {
        return alunoService.buscar(id);
    }

    @PutMapping("/{id}")
    public AlunoDTO atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarAlunoRequest req) {
        return alunoService.atualizar(id, req);
    }

    @PatchMapping("/{id}/status")
    public AlunoDTO alterarStatus(@PathVariable UUID id, @Valid @RequestBody AlterarStatusRequest req) {
        return alunoService.alterarStatus(id, req.status());
    }

    @PostMapping("/{id}/reenviar-convite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reenviarConvite(@PathVariable UUID id) {
        alunoService.reenviarConvite(id);
    }
}
