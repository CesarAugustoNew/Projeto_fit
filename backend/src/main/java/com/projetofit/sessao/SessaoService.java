package com.projetofit.sessao;

import com.projetofit.aluno.Aluno;
import com.projetofit.comum.erro.ConflitoException;
import com.projetofit.comum.erro.DadosInvalidosException;
import com.projetofit.exercicio.Exercicio;
import com.projetofit.exercicio.ExercicioRepository;
import com.projetofit.ficha.Divisao;
import com.projetofit.ficha.DivisaoRepository;
import com.projetofit.ficha.ItemDivisao;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Sessões de treino do aluno autenticado. */
@Service
public class SessaoService {

    private static final Logger log = LoggerFactory.getLogger(SessaoService.class);
    /** Folga para o relógio do celular adiantado em relação ao do servidor. */
    private static final Duration TOLERANCIA_RELOGIO = Duration.ofMinutes(5);

    public record Resultado(SessaoDTO sessao, boolean criada) {
    }

    private final AlunoLogado alunoLogado;
    private final SessaoRepository sessaoRepository;
    private final DivisaoRepository divisaoRepository;
    private final ExercicioRepository exercicioRepository;
    private final Clock clock;
    private final TransactionTemplate escrita;
    private final TransactionTemplate leitura;

    public SessaoService(AlunoLogado alunoLogado, SessaoRepository sessaoRepository,
                         DivisaoRepository divisaoRepository, ExercicioRepository exercicioRepository, Clock clock,
                         PlatformTransactionManager transactionManager) {
        this.alunoLogado = alunoLogado;
        this.sessaoRepository = sessaoRepository;
        this.divisaoRepository = divisaoRepository;
        this.exercicioRepository = exercicioRepository;
        this.clock = clock;
        this.escrita = new TransactionTemplate(transactionManager);
        this.leitura = new TransactionTemplate(transactionManager);
        this.leitura.setReadOnly(true);
    }

    /**
     * Grava a sessão enviada pelo celular; reenviar o mesmo {@code clientId} devolve a sessão já gravada
     * ({@code criada = false}). Sem {@code @Transactional} de propósito: a gravação roda em {@link TransactionTemplate}
     * para que, se dois envios simultâneos do mesmo {@code clientId} colidirem no índice único, a releitura aconteça
     * numa transação nova, fora da que falhou.
     */
    public Resultado registrar(SessaoRequest req) {
        try {
            return escrita.execute(status -> gravar(req));
        } catch (DataIntegrityViolationException e) {
            log.warn("Violação de integridade ao gravar a sessão {}; relendo pelo clientId", req.clientId(), e);
            return leitura.execute(status -> {
                Sessao existente = sessaoRepository.findByClientId(req.clientId()).orElseThrow(() -> e);
                return jaRegistrada(existente, alunoLogado.obter());
            });
        }
    }

    private Resultado gravar(SessaoRequest req) {
        Aluno aluno = alunoLogado.obter();
        Optional<Sessao> existente = sessaoRepository.findByClientId(req.clientId());
        if (existente.isPresent()) {
            return jaRegistrada(existente.get(), aluno);
        }
        validarHorarios(req);
        Divisao divisao = divisaoDoAluno(req.divisaoId(), aluno);
        String divisaoNome = divisao != null ? divisao.getNome() : req.divisaoNome().trim();
        Map<UUID, Exercicio> exercicios = exerciciosDoPersonal(req.series(), aluno.getPersonalId());

        Sessao sessao = new Sessao(req.clientId(), aluno, divisao, divisaoNome, req.iniciadaEm(),
                req.finalizadaEm(), textoOuNulo(req.observacao()));
        for (int i = 0; i < req.series().size(); i++) {
            SerieRequest serie = req.series().get(i);
            Exercicio exercicio = exercicios.get(serie.exercicioId());
            if (exercicio == null) {
                throw new DadosInvalidosException("series[" + i + "].exercicioId", "Exercício inválido.");
            }
            sessao.adicionarSerie(exercicio, itemDaDivisao(divisao, serie.itemDivisaoId()), i + 1,
                    serie.numeroSerie(), serie.repeticoes(), serie.cargaKg());
        }
        return new Resultado(SessaoDTO.de(sessaoRepository.saveAndFlush(sessao)), true);
    }

    private static Resultado jaRegistrada(Sessao sessao, Aluno aluno) {
        if (!sessao.getAluno().getId().equals(aluno.getId())) {
            throw new ConflitoException("Sessão já registrada.");
        }
        return new Resultado(SessaoDTO.de(sessao), false);
    }

    private void validarHorarios(SessaoRequest req) {
        if (req.finalizadaEm().isBefore(req.iniciadaEm())) {
            throw new DadosInvalidosException("finalizadaEm", "O fim do treino deve ser depois do início.");
        }
        if (req.finalizadaEm().isAfter(Instant.now(clock).plus(TOLERANCIA_RELOGIO))) {
            throw new DadosInvalidosException("finalizadaEm",
                    "O horário de fim está no futuro. Confira o relógio do celular.");
        }
    }

    /**
     * A divisão informada, que precisa ser de uma ficha (ativa ou encerrada) do aluno. Divisão não encontrada
     * (removida enquanto o aluno treinava sem internet) devolve {@code null}: a sessão é gravada só com o nome.
     */
    private Divisao divisaoDoAluno(UUID divisaoId, Aluno aluno) {
        if (divisaoId == null) {
            return null;
        }
        return divisaoRepository.findById(divisaoId)
                .map(divisao -> {
                    if (!divisao.getFicha().getAluno().getId().equals(aluno.getId())) {
                        throw new DadosInvalidosException("divisaoId", "Divisão inválida.");
                    }
                    return divisao;
                })
                .orElse(null);
    }

    /** Exercícios do personal do aluno (ativos ou não) citados nas séries, por id. */
    private Map<UUID, Exercicio> exerciciosDoPersonal(List<SerieRequest> series, UUID personalId) {
        Set<UUID> ids = series.stream().map(SerieRequest::exercicioId).collect(Collectors.toSet());
        return exercicioRepository.findAllById(ids).stream()
                .filter(e -> e.getPersonalId().equals(personalId))
                .collect(Collectors.toMap(Exercicio::getId, Function.identity()));
    }

    /** O item só é ligado se ainda existir na divisão da sessão. */
    private static ItemDivisao itemDaDivisao(Divisao divisao, UUID itemId) {
        if (divisao == null || itemId == null) {
            return null;
        }
        return divisao.getItens().stream().filter(i -> itemId.equals(i.getId())).findFirst().orElse(null);
    }

    private static String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
