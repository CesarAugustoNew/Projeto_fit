package com.projetofit.desempenho;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    private final DashboardService dashboardService;

    public DesempenhoController(DesempenhoService service, DashboardService dashboardService) {
        this.service = service;
        this.dashboardService = dashboardService;
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

    @GetMapping("/api/alunos/{alunoId}/desempenho/aderencia")
    @PreAuthorize("hasRole('PERSONAL')")
    public List<SemanaAderenciaDTO> aderenciaDoAluno(
            @PathVariable UUID alunoId,
            @RequestParam(defaultValue = "8") @Min(1) @Max(26) int semanas) {
        return service.aderencia(service.alunoDoPersonal(alunoId), semanas);
    }

    @GetMapping("/api/me/desempenho/aderencia")
    @PreAuthorize("hasRole('ALUNO')")
    public List<SemanaAderenciaDTO> minhaAderencia(
            @RequestParam(defaultValue = "8") @Min(1) @Max(26) int semanas) {
        return service.aderencia(service.alunoLogado(), semanas);
    }

    @GetMapping("/api/dashboard")
    @PreAuthorize("hasRole('PERSONAL')")
    public List<DashboardAlunoDTO> dashboard() {
        return dashboardService.dashboard();
    }
}
