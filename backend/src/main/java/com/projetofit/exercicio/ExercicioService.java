package com.projetofit.exercicio;

import com.projetofit.comum.erro.ConflitoException;
import com.projetofit.comum.erro.RecursoNaoEncontradoException;
import com.projetofit.usuario.UsuarioAtual;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Catálogo de exercícios do personal autenticado. Toda consulta filtra por {@code personal_id = usuarioAtual.id()}. */
@Service
public class ExercicioService {

    private static final Logger log = LoggerFactory.getLogger(ExercicioService.class);
    private static final String NOME_REPETIDO = "Já existe um exercício com este nome.";

    private final ExercicioRepository exercicioRepository;
    private final UsuarioAtual usuarioAtual;

    public ExercicioService(ExercicioRepository exercicioRepository, UsuarioAtual usuarioAtual) {
        this.exercicioRepository = exercicioRepository;
        this.usuarioAtual = usuarioAtual;
    }

    /** Sem {@code ativo}, só os ativos; sem {@code grupo}, todos os grupos. */
    @Transactional(readOnly = true)
    public List<ExercicioDTO> listar(Boolean ativo, GrupoMuscular grupo) {
        boolean filtroAtivo = ativo == null || ativo;
        UUID personalId = usuarioAtual.id();
        List<Exercicio> exercicios = grupo == null
                ? exercicioRepository.findByPersonalIdAndAtivoOrderByNomeAsc(personalId, filtroAtivo)
                : exercicioRepository.findByPersonalIdAndAtivoAndGrupoMuscularOrderByNomeAsc(
                        personalId, filtroAtivo, grupo);
        return exercicios.stream().map(ExercicioDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public ExercicioDTO buscar(UUID id) {
        return ExercicioDTO.de(doPersonal(id));
    }

    @Transactional
    public ExercicioDTO criar(NovoExercicioRequest req) {
        UUID personalId = usuarioAtual.id();
        String nome = req.nome().trim();
        if (exercicioRepository.existsByPersonalIdAndNomeIgnoreCase(personalId, nome)) {
            throw new ConflitoException(NOME_REPETIDO);
        }
        try {
            Exercicio exercicio = exercicioRepository.saveAndFlush(new Exercicio(personalId, nome,
                    req.grupoMuscular(), vazioParaNulo(req.videoUrl()), vazioParaNulo(req.observacao())));
            return ExercicioDTO.de(exercicio);
        } catch (DataIntegrityViolationException e) {
            log.warn("Violação de integridade ao gravar o exercício; respondendo como nome repetido", e);
            throw new ConflitoException(NOME_REPETIDO);
        }
    }

    @Transactional
    public ExercicioDTO atualizar(UUID id, AtualizarExercicioRequest req) {
        Exercicio exercicio = doPersonal(id);
        String nome = req.nome().trim();
        if (exercicioRepository.existsByPersonalIdAndNomeIgnoreCaseAndIdNot(usuarioAtual.id(), nome, id)) {
            throw new ConflitoException(NOME_REPETIDO);
        }
        exercicio.atualizar(nome, req.grupoMuscular(), vazioParaNulo(req.videoUrl()),
                vazioParaNulo(req.observacao()), req.ativo());
        return ExercicioDTO.de(exercicio);
    }

    /** O exercício {@code id} do personal autenticado; o de outro personal é tratado como inexistente. */
    private Exercicio doPersonal(UUID id) {
        return exercicioRepository.findByIdAndPersonalId(id, usuarioAtual.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Exercício não encontrado."));
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
