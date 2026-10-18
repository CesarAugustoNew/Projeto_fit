package com.projetofit.exercicio;

import com.projetofit.usuario.AoCriarPersonal;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Catálogo inicial de exercícios de cada personal (lido de {@code semente/exercicios.json}). */
@Component
public class SementeExercicios implements AoCriarPersonal {

    private record ItemDaSemente(String nome, GrupoMuscular grupoMuscular) {
    }

    private final ExercicioRepository exercicioRepository;
    private final List<ItemDaSemente> itens;

    public SementeExercicios(ExercicioRepository exercicioRepository, JsonMapper jsonMapper) {
        this.exercicioRepository = exercicioRepository;
        try (InputStream entrada = new ClassPathResource("semente/exercicios.json").getInputStream()) {
            this.itens = jsonMapper.readValue(entrada, new TypeReference<List<ItemDaSemente>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler semente/exercicios.json", e);
        }
    }

    @Override
    public void aoCriarPersonal(UUID personalId) {
        semearSeVazio(personalId);
    }

    /** Grava o catálogo inicial só se o personal ainda não tem nenhum exercício. */
    @Transactional
    public void semearSeVazio(UUID personalId) {
        if (exercicioRepository.countByPersonalId(personalId) > 0) {
            return;
        }
        exercicioRepository.saveAll(itens.stream()
                .map(item -> new Exercicio(personalId, item.nome(), item.grupoMuscular(), null, null))
                .toList());
    }
}
