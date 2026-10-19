package com.projetofit.desempenho;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DesempenhoController {

    private final DesempenhoService service;

    public DesempenhoController(DesempenhoService service) {
        this.service = service;
    }

    @GetMapping("/api/alunos/{alunoId}/desempenho/evolucao")
    @PreAuthorize("hasRole('PERSONAL')")
    public List<PontoEvolucaoDTO> evolucaoDoAluno(
            @PathVariable UUID alunoId,
            @RequestParam UUID exercicioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return service.evolucao(service.alunoDoPersonal(alunoId), exercicioId, de, ate);
    }

    @GetMapping("/api/alunos/{alunoId}/desempenho/exercicios")
    @PreAuthorize("hasRole('PERSONAL')")
    public List<ExercicioExecutadoDTO> exerciciosDoAluno(@PathVariable UUID alunoId) {
        return service.exercicios(service.alunoDoPersonal(alunoId));
    }

    @GetMapping("/api/me/desempenho/evolucao")
    @PreAuthorize("hasRole('ALUNO')")
    public List<PontoEvolucaoDTO> minhaEvolucao(
            @RequestParam UUID exercicioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return service.evolucao(service.alunoLogado(), exercicioId, de, ate);
    }

    @GetMapping("/api/me/desempenho/exercicios")
    @PreAuthorize("hasRole('ALUNO')")
    public List<ExercicioExecutadoDTO> meusExercicios() {
        return service.exercicios(service.alunoLogado());
    }
}
