package com.projetofit.desempenho;

import com.projetofit.aluno.Aluno;
import com.projetofit.aluno.AlunoRepository;
import com.projetofit.aluno.StatusAluno;
import com.projetofit.desempenho.CalculoAderencia.PeriodoFicha;
import com.projetofit.ficha.Ficha;
import com.projetofit.ficha.FichaRepository;
import com.projetofit.ficha.StatusFicha;
import com.projetofit.sessao.SessaoRepository;
import com.projetofit.usuario.UsuarioAtual;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private static final int DIAS_ATE_SUMIR = 7;

    private final AlunoRepository alunoRepository;
    private final FichaRepository fichaRepository;
    private final SessaoRepository sessaoRepository;
    private final UsuarioAtual usuarioAtual;
    private final Clock clock;

    public DashboardService(AlunoRepository alunoRepository, FichaRepository fichaRepository,
                            SessaoRepository sessaoRepository, UsuarioAtual usuarioAtual, Clock clock) {
        this.alunoRepository = alunoRepository;
        this.fichaRepository = fichaRepository;
        this.sessaoRepository = sessaoRepository;
        this.usuarioAtual = usuarioAtual;
        this.clock = clock;
    }

    /** Alunos ativos do personal logado: quem "sumiu" primeiro, depois por nome. */
    @Transactional(readOnly = true)
    public List<DashboardAlunoDTO> dashboard() {
        UUID personalId = usuarioAtual.id();
        LocalDate hoje = LocalDate.now(clock);
        LocalDate segunda = CalculoAderencia.segundaDaSemana(hoje);
        Instant de = segunda.atStartOfDay(clock.getZone()).toInstant();
        Instant ate = segunda.plusWeeks(1).atStartOfDay(clock.getZone()).toInstant();

        List<Aluno> alunos = alunoRepository.findByPersonalIdAndStatusInOrderByUsuarioNomeAsc(
                personalId, List.of(StatusAluno.ATIVO));
        Map<UUID, Ficha> fichasAtivas = fichaRepository.findByAlunoPersonalIdAndStatus(personalId, StatusFicha.ATIVA)
                .stream().collect(Collectors.toMap(f -> f.getAluno().getId(), Function.identity()));
        Map<UUID, List<PeriodoFicha>> periodos = fichaRepository.findNaoEncerradasAntesDe(personalId, de)
                .stream().collect(Collectors.groupingBy(f -> f.getAluno().getId(),
                        Collectors.mapping(f -> new PeriodoFicha(f.getCriadoEm(), f.getEncerradaEm(),
                                f.getFrequenciaSemanal()), Collectors.toList())));
        Map<UUID, Instant> ultimas = new HashMap<>();
        sessaoRepository.ultimaSessaoPorAluno(personalId)
                .forEach(l -> ultimas.put((UUID) l[0], (Instant) l[1]));
        Map<UUID, Long> daSemana = new HashMap<>();
        sessaoRepository.sessoesPorAlunoEntre(personalId, de, ate)
                .forEach(l -> daSemana.put((UUID) l[0], (Long) l[1]));

        return alunos.stream()
                .map(aluno -> resumir(aluno, fichasAtivas.get(aluno.getId()), ultimas.get(aluno.getId()),
                        CalculoAderencia.planejadoNaSemana(periodos.getOrDefault(aluno.getId(), List.of()), de, ate),
                        daSemana.getOrDefault(aluno.getId(), 0L).intValue(), hoje))
                .sorted(Comparator.comparing((DashboardAlunoDTO d) -> !d.sumiu()))
                .toList();
    }

    private DashboardAlunoDTO resumir(Aluno aluno, Ficha ficha, Instant ultimaSessao, int planejado,
                                      int realizados, LocalDate hoje) {
        Integer diasSemTreinar = ultimaSessao == null ? null
                : (int) ChronoUnit.DAYS.between(ultimaSessao.atZone(clock.getZone()).toLocalDate(), hoje);
        boolean sumiu = diasSemTreinar != null
                ? diasSemTreinar > DIAS_ATE_SUMIR
                : ficha != null && ChronoUnit.DAYS.between(
                        ficha.getCriadoEm().atZone(clock.getZone()).toLocalDate(), hoje) > DIAS_ATE_SUMIR;
        return new DashboardAlunoDTO(aluno.getId(), aluno.getUsuario().getNome(), realizados,
                planejado, diasSemTreinar,
                ficha == null ? null : ficha.getId(), ficha != null && ficha.vencida(hoje), sumiu);
    }
}
