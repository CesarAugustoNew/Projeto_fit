package com.projetofit.exercicio;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercicios")
@PreAuthorize("hasRole('PERSONAL')")
public class ExercicioController {

    private final ExercicioService exercicioService;

    public ExercicioController(ExercicioService exercicioService) {
        this.exercicioService = exercicioService;
    }

    @GetMapping
    public List<ExercicioDTO> listar(@RequestParam(required = false) Boolean ativo,
                                     @RequestParam(required = false) GrupoMuscular grupo) {
        return exercicioService.listar(ativo, grupo);
    }

    @GetMapping("/{id}")
    public ExercicioDTO buscar(@PathVariable UUID id) {
        return exercicioService.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExercicioDTO criar(@Valid @RequestBody NovoExercicioRequest req) {
        return exercicioService.criar(req);
    }

    @PutMapping("/{id}")
    public ExercicioDTO atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarExercicioRequest req) {
        return exercicioService.atualizar(id, req);
    }
}
