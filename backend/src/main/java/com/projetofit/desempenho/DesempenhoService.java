package com.projetofit.desempenho;

import com.projetofit.aluno.AlunoRepository;
import com.projetofit.comum.erro.DadosInvalidosException;
import com.projetofit.comum.erro.RecursoNaoEncontradoException;
import com.projetofit.ficha.FichaRepository;
import com.projetofit.sessao.AlunoLogado;
import com.projetofit.sessao.SessaoRepository;
import com.projetofit.usuario.UsuarioAtual;
import com.projetofit.desempenho.CalculoAderencia.PeriodoFicha;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DesempenhoService {

    private final DesempenhoConsultas consultas;
    private final AlunoRepository alunoRepository;
    private final FichaRepository fichaRepository;
    private final SessaoRepository sessaoRepository;
    private final AlunoLogado alunoLogado;
    private final UsuarioAtual usuarioAtual;
    private final Clock clock;

    public DesempenhoService(DesempenhoConsultas consultas, AlunoRepository alunoRepository,
                             FichaRepository fichaRepository, SessaoRepository sessaoRepository,
                             AlunoLogado alunoLogado, UsuarioAtual usuarioAtual, Clock clock) {
        this.consultas = consultas;
        this.alunoRepository = alunoRepository;
        this.fichaRepository = fichaRepository;
        this.sessaoRepository = sessaoRepository;
        this.alunoLogado = alunoLogado;
        this.usuarioAtual = usuarioAtual;
        this.clock = clock;
    }

    /** Um ponto por sessão que tem o exercício; padrão: últimos 6 meses até hoje. */
    @Transactional(readOnly = true)
    public List<PontoEvolucaoDTO> evolucao(UUID alunoId, UUID exercicioId, LocalDate de, LocalDate ate) {
        LocalDate fim = ate != null ? ate : LocalDate.now(clock);
        LocalDate inicio = de != null ? de : fim.minusMonths(6);
        if (inicio.isAfter(fim)) {
            throw new DadosInvalidosException("de", "A data inicial deve ser anterior ou igual à final.");
        }
        return consultas.evolucao(alunoId, exercicioId,
                        inicio.atStartOfDay(clock.getZone()).toInstant(),
                        fim.plusDays(1).atStartOfDay(clock.getZone()).toInstant())
                .stream()
                .map(p -> new PontoEvolucaoDTO(p.finalizadaEm().atZone(clock.getZone()).toLocalDate(),
                        p.sessaoId(), p.cargaMaxima(), p.volume()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExercicioExecutadoDTO> exercicios(UUID alunoId) {
        return consultas.exerciciosExecutados(alunoId);
    }

    /** Últimas {@code semanas} semanas (segunda a domingo), a última é a atual. */
    @Transactional(readOnly = true)
    public List<SemanaAderenciaDTO> aderencia(UUID alunoId, int semanas) {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate primeiraSegunda = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .minusWeeks(semanas - 1L);
        Instant de = primeiraSegunda.atStartOfDay(clock.getZone()).toInstant();
        Instant ate = hoje.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).plusDays(1)
                .atStartOfDay(clock.getZone()).toInstant();
        List<Instant> finalizacoes = sessaoRepository.findFinalizadasEmEntre(alunoId, de, ate);
        List<PeriodoFicha> fichas = fichaRepository.findByAlunoIdOrderByDataInicioDescCriadoEmDesc(alunoId).stream()
                .map(f -> new PeriodoFicha(f.getCriadoEm(), f.getEncerradaEm(), f.getFrequenciaSemanal()))
                .toList();
        return CalculoAderencia.calcular(hoje, semanas, clock.getZone(), finalizacoes, fichas);
    }

    /** Valida que o aluno é do personal logado e devolve o seu id. */
    @Transactional(readOnly = true)
    public UUID alunoDoPersonal(UUID alunoId) {
        return alunoRepository.findByIdAndPersonalId(alunoId, usuarioAtual.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."))
                .getId();
    }

    /** Id do aluno dono do token da requisição. */
    @Transactional(readOnly = true)
    public UUID alunoLogado() {
        return alunoLogado.obter().getId();
    }
}
