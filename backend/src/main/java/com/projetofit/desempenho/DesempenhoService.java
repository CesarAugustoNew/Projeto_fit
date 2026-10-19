package com.projetofit.desempenho;

import com.projetofit.aluno.AlunoRepository;
import com.projetofit.comum.erro.DadosInvalidosException;
import com.projetofit.comum.erro.RecursoNaoEncontradoException;
import com.projetofit.sessao.AlunoLogado;
import com.projetofit.usuario.UsuarioAtual;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DesempenhoService {

    private final DesempenhoConsultas consultas;
    private final AlunoRepository alunoRepository;
    private final AlunoLogado alunoLogado;
    private final UsuarioAtual usuarioAtual;
    private final Clock clock;

    public DesempenhoService(DesempenhoConsultas consultas, AlunoRepository alunoRepository,
                             AlunoLogado alunoLogado, UsuarioAtual usuarioAtual, Clock clock) {
        this.consultas = consultas;
        this.alunoRepository = alunoRepository;
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
