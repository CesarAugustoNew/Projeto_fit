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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fichas de treino dos alunos do personal autenticado. Toda consulta filtra pelo {@code personal_id} do aluno.
 * Os DTOs são montados dentro das transações porque divisões, itens e exercícios são carregados sob demanda.
 */
@Service
public class FichaService {

    private static final Logger log = LoggerFactory.getLogger(FichaService.class);

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
        Map<UUID, Exercicio> exercicios = exerciciosValidos(req.divisoes(), Map.of());

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
        try {
            fichaRepository.saveAndFlush(ficha);
        } catch (DataIntegrityViolationException e) {
            // Dois POSTs simultâneos para o mesmo aluno: o índice único parcial barra a segunda ficha ATIVA.
            log.warn("Violação de integridade ao gravar a ficha nova do aluno {}", alunoId, e);
            throw new ConflitoException("Este aluno já tem uma ficha ativa. Recarregue a página e tente de novo.");
        }
        return FichaDTO.de(ficha, hoje());
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

    /**
     * Sincroniza a ficha {@code ATIVA} com o request: divisões e itens com {@code id} são atualizados, os sem
     * {@code id} são criados e os que ficaram de fora, removidos. A ordem de cada um é a posição na lista.
     */
    @Transactional
    public FichaDTO atualizar(UUID id, FichaRequest req) {
        Ficha ficha = doPersonal(id);
        if (ficha.getStatus() == StatusFicha.ENCERRADA) {
            throw new ConflitoException("Só é possível editar a ficha ativa.");
        }
        validarDatas(req);
        Map<UUID, Divisao> divisoesAtuais = new HashMap<>();
        Map<UUID, UUID> exercicioAtualPorItem = new HashMap<>();
        for (Divisao divisao : ficha.getDivisoes()) {
            divisoesAtuais.put(divisao.getId(), divisao);
            for (ItemDivisao item : divisao.getItens()) {
                exercicioAtualPorItem.put(item.getId(), item.getExercicio().getId());
            }
        }
        Map<UUID, Exercicio> exercicios = exerciciosValidos(req.divisoes(), exercicioAtualPorItem);

        ficha.atualizarCabecalho(req.nome().trim(), vazioParaNulo(req.objetivo()), req.frequenciaSemanal(),
                req.dataInicio(), req.dataFim());
        Set<UUID> divisoesMantidas = new HashSet<>();
        for (int i = 0; i < req.divisoes().size(); i++) {
            DivisaoRequest divisaoReq = req.divisoes().get(i);
            String nome = divisaoReq.nome().trim();
            Divisao divisao;
            if (divisaoReq.id() == null) {
                divisao = ficha.adicionarDivisao(nome, i + 1);
            } else {
                // remove(): um id repetido no request não pode reaproveitar a mesma divisão duas vezes.
                divisao = divisoesAtuais.remove(divisaoReq.id());
                if (divisao == null) {
                    throw new DadosInvalidosException("divisoes[" + i + "].id", "Divisão não pertence a esta ficha.");
                }
                divisao.atualizar(nome, i + 1);
                divisoesMantidas.add(divisao.getId());
            }
            sincronizarItens(divisao, divisaoReq.itens(), exercicios, i);
        }
        ficha.removerDivisoesExceto(divisoesMantidas);

        try {
            fichaRepository.flush();
        } catch (DataIntegrityViolationException e) {
            log.warn("Violação de integridade ao gravar a edição da ficha {}", id, e);
            throw new ConflitoException("Não foi possível salvar a ficha. Recarregue a página e tente de novo.");
        }
        return FichaDTO.de(ficha, hoje());
    }

    @Transactional
    public FichaDTO encerrar(UUID id) {
        Ficha ficha = doPersonal(id);
        if (ficha.getStatus() == StatusFicha.ENCERRADA) {
            throw new ConflitoException("A ficha já está encerrada.");
        }
        ficha.encerrar();
        return FichaDTO.de(fichaRepository.saveAndFlush(ficha), hoje());
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
     * Os exercícios referenciados pelos itens, por id. Cada um precisa ser do personal autenticado e, salvo quando o
     * item já existe e mantém o mesmo exercício ({@code exercicioAtualPorItem}: id do item → id do exercício atual),
     * estar ativo; senão o erro aponta o primeiro item inválido pelo caminho do campo
     * (ex.: {@code divisoes[1].itens[0].exercicioId}).
     */
    Map<UUID, Exercicio> exerciciosValidos(List<DivisaoRequest> divisoes, Map<UUID, UUID> exercicioAtualPorItem) {
        UUID personalId = usuarioAtual.id();
        Map<UUID, Exercicio> encontrados = new HashMap<>();
        for (int i = 0; i < divisoes.size(); i++) {
            List<ItemRequest> itens = divisoes.get(i).itens();
            for (int j = 0; j < itens.size(); j++) {
                ItemRequest item = itens.get(j);
                UUID exercicioId = item.exercicioId();
                Exercicio exercicio = encontrados.get(exercicioId);
                if (exercicio == null) {
                    exercicio = exercicioRepository.findByIdAndPersonalId(exercicioId, personalId).orElse(null);
                }
                boolean mantemOMesmo = item.id() != null && exercicioId.equals(exercicioAtualPorItem.get(item.id()));
                if (exercicio == null || !(exercicio.isAtivo() || mantemOMesmo)) {
                    throw new DadosInvalidosException("divisoes[" + i + "].itens[" + j + "].exercicioId",
                            "Exercício inexistente ou inativo.");
                }
                encontrados.put(exercicioId, exercicio);
            }
        }
        return encontrados;
    }

    /** Atualiza os itens com {@code id} (só desta divisão), cria os sem {@code id} e remove os que ficaram de fora. */
    private static void sincronizarItens(Divisao divisao, List<ItemRequest> itens, Map<UUID, Exercicio> exercicios,
                                         int indiceDivisao) {
        Map<UUID, ItemDivisao> itensAtuais = new HashMap<>();
        for (ItemDivisao item : divisao.getItens()) {
            itensAtuais.put(item.getId(), item);
        }
        Set<UUID> itensMantidos = new HashSet<>();
        for (int j = 0; j < itens.size(); j++) {
            ItemRequest req = itens.get(j);
            Exercicio exercicio = exercicios.get(req.exercicioId());
            String repeticoes = req.repeticoes().trim();
            String observacao = vazioParaNulo(req.observacao());
            if (req.id() == null) {
                divisao.adicionarItem(exercicio, j + 1, req.series(), repeticoes, req.cargaKg(), req.descansoSeg(),
                        observacao);
            } else {
                ItemDivisao item = itensAtuais.remove(req.id());
                if (item == null) {
                    throw new DadosInvalidosException("divisoes[" + indiceDivisao + "].itens[" + j + "].id",
                            "Item não pertence a esta divisão.");
                }
                item.atualizar(exercicio, j + 1, req.series(), repeticoes, req.cargaKg(), req.descansoSeg(),
                        observacao);
                itensMantidos.add(item.getId());
            }
        }
        divisao.removerItensExceto(itensMantidos);
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
