package com.projetofit.ficha;

import com.projetofit.aluno.Aluno;
import com.projetofit.aluno.AlunoRepository;
import com.projetofit.aluno.StatusAluno;
import com.projetofit.comum.erro.ConflitoException;
import com.projetofit.comum.erro.DadosInvalidosException;
import com.projetofit.comum.erro.RecursoNaoEncontradoException;
import com.projetofit.exercicio.Exercicio;
import com.projetofit.exercicio.ExercicioRepository;
import com.projetofit.usuario.UsuarioAtual;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fichas de treino dos alunos do personal autenticado. Toda consulta filtra pelo {@code personal_id} do aluno.
 * Os DTOs são montados dentro das transações porque divisões, itens e exercícios são carregados sob demanda.
 */
@Service
public class FichaService {

    private final FichaRepository fichaRepository;
    private final AlunoRepository alunoRepository;
    private final ExercicioRepository exercicioRepository;
    private final UsuarioAtual usuarioAtual;
    private final Clock clock;

    public FichaService(FichaRepository fichaRepository, AlunoRepository alunoRepository,
                        ExercicioRepository exercicioRepository, UsuarioAtual usuarioAtual, Clock clock) {
        this.fichaRepository = fichaRepository;
        this.alunoRepository = alunoRepository;
        this.exercicioRepository = exercicioRepository;
        this.usuarioAtual = usuarioAtual;
        this.clock = clock;
    }

    /** A ficha nova nasce {@code ATIVA}; a ativa anterior do aluno é encerrada na mesma transação. */
    @Transactional
    public FichaDTO criar(UUID alunoId, FichaRequest req) {
        Aluno aluno = alunoDoPersonal(alunoId);
        if (aluno.getStatus() == StatusAluno.INATIVO) {
            throw new ConflitoException("Não é possível criar ficha para aluno inativo.");
        }
        validarDatas(req);
        Map<UUID, Exercicio> exercicios = exerciciosValidos(req.divisoes());

        // O flush grava o UPDATE já: no flush o Hibernate faz os INSERTs antes dos UPDATEs, e a ficha nova
        // ATIVA entraria antes de a antiga deixar de ser, violando o índice único parcial (uma ATIVA por aluno).
        fichaRepository.findByAlunoIdAndStatus(alunoId, StatusFicha.ATIVA).ifPresent(anterior -> {
            anterior.encerrar();
            fichaRepository.flush();
        });

        Ficha ficha = new Ficha(aluno, req.nome().trim(), vazioParaNulo(req.objetivo()), req.frequenciaSemanal(),
                req.dataInicio(), req.dataFim());
        for (int i = 0; i < req.divisoes().size(); i++) {
            DivisaoRequest divisaoReq = req.divisoes().get(i);
            Divisao divisao = ficha.adicionarDivisao(divisaoReq.nome().trim(), i + 1);
            for (int j = 0; j < divisaoReq.itens().size(); j++) {
                ItemRequest item = divisaoReq.itens().get(j);
                divisao.adicionarItem(exercicios.get(item.exercicioId()), j + 1, item.series(),
                        item.repeticoes().trim(), item.cargaKg(), item.descansoSeg(),
                        vazioParaNulo(item.observacao()));
            }
        }
        return FichaDTO.de(fichaRepository.saveAndFlush(ficha), hoje());
    }

    /** A ficha {@code ATIVA} primeiro; depois as encerradas, da mais recente para a mais antiga. */
    @Transactional(readOnly = true)
    public List<FichaResumoDTO> listarDoAluno(UUID alunoId) {
        alunoDoPersonal(alunoId);
        LocalDate hoje = hoje();
        return fichaRepository.findByAlunoIdOrderByDataInicioDescCriadoEmDesc(alunoId).stream()
                .sorted(Comparator.comparing(f -> f.getStatus() != StatusFicha.ATIVA))
                .map(f -> FichaResumoDTO.de(f, hoje))
                .toList();
    }

    @Transactional(readOnly = true)
    public FichaDTO buscar(UUID id) {
        return FichaDTO.de(doPersonal(id), hoje());
    }

    /** A ficha {@code id} de um aluno do personal autenticado; a de outro personal é tratada como inexistente. */
    Ficha doPersonal(UUID id) {
        return fichaRepository.findByIdAndAlunoPersonalId(id, usuarioAtual.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ficha não encontrada."));
    }

    /** A data de fim, quando informada, não pode ser anterior à de início. */
    void validarDatas(FichaRequest req) {
        if (req.dataFim() != null && req.dataFim().isBefore(req.dataInicio())) {
            throw new DadosInvalidosException("dataFim", "A data de fim deve ser igual ou posterior à data de início.");
        }
    }

    /**
     * Os exercícios referenciados pelos itens, por id. Cada um precisa ser do personal autenticado e estar ativo;
     * senão o erro aponta o primeiro item inválido pelo caminho do campo (ex.: {@code divisoes[1].itens[0].exercicioId}).
     */
    Map<UUID, Exercicio> exerciciosValidos(List<DivisaoRequest> divisoes) {
        UUID personalId = usuarioAtual.id();
        Map<UUID, Exercicio> encontrados = new HashMap<>();
        for (int i = 0; i < divisoes.size(); i++) {
            List<ItemRequest> itens = divisoes.get(i).itens();
            for (int j = 0; j < itens.size(); j++) {
                UUID exercicioId = itens.get(j).exercicioId();
                if (encontrados.containsKey(exercicioId)) {
                    continue;
                }
                String campo = "divisoes[" + i + "].itens[" + j + "].exercicioId";
                Exercicio exercicio = exercicioRepository.findByIdAndPersonalId(exercicioId, personalId)
                        .filter(Exercicio::isAtivo)
                        .orElseThrow(() -> new DadosInvalidosException(campo, "Exercício inexistente ou inativo."));
                encontrados.put(exercicioId, exercicio);
            }
        }
        return encontrados;
    }

    private Aluno alunoDoPersonal(UUID alunoId) {
        return alunoRepository.findByIdAndPersonalId(alunoId, usuarioAtual.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));
    }

    private LocalDate hoje() {
        return LocalDate.now(clock);
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
