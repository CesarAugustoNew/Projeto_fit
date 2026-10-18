package com.projetofit.aluno;

import com.projetofit.comum.erro.ConflitoException;
import com.projetofit.comum.erro.RecursoNaoEncontradoException;
import com.projetofit.comum.supabase.SupabaseAdmin;
import com.projetofit.usuario.Papel;
import com.projetofit.usuario.Usuario;
import com.projetofit.usuario.UsuarioAtual;
import com.projetofit.usuario.UsuarioRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Alunos do personal autenticado. Toda consulta filtra por {@code personal_id = usuarioAtual.id()}. */
@Service
public class AlunoService {

    private static final Logger log = LoggerFactory.getLogger(AlunoService.class);
    private static final String EMAIL_JA_CADASTRADO = "E-mail já cadastrado.";

    private final AlunoRepository alunoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SupabaseAdmin supabaseAdmin;
    private final UsuarioAtual usuarioAtual;
    private final TransactionTemplate transactionTemplate;

    public AlunoService(AlunoRepository alunoRepository, UsuarioRepository usuarioRepository,
                        SupabaseAdmin supabaseAdmin, UsuarioAtual usuarioAtual, TransactionTemplate transactionTemplate) {
        this.alunoRepository = alunoRepository;
        this.usuarioRepository = usuarioRepository;
        this.supabaseAdmin = supabaseAdmin;
        this.usuarioAtual = usuarioAtual;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Convida o aluno no Supabase e grava {@code usuario} + {@code aluno}. Sem {@code @Transactional} de propósito:
     * a chamada HTTP ao Supabase fica fora da transação, e só a gravação roda em {@link TransactionTemplate}. Se a
     * gravação falhar depois do convite, o usuário criado no Auth é excluído (compensação) para que o e-mail possa
     * ser convidado de novo.
     */
    public AlunoDTO criar(NovoAlunoRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        String nome = req.nome().trim();
        if (usuarioRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ConflitoException(EMAIL_JA_CADASTRADO);
        }
        UUID personalId = usuarioAtual.id();
        UUID id = supabaseAdmin.convidar(email, nome);
        try {
            return transactionTemplate.execute(status -> {
                Usuario usuario = usuarioRepository.saveAndFlush(new Usuario(id, nome, email, Papel.ALUNO));
                Aluno aluno = alunoRepository.saveAndFlush(new Aluno(usuario, personalId, req.telefone(),
                        req.dataNascimento(), req.objetivo(), req.observacoes()));
                return AlunoDTO.de(aluno);
            });
        } catch (RuntimeException e) {
            excluirNoSupabase(id);
            if (e instanceof DataIntegrityViolationException) {
                log.warn("Violação de integridade ao gravar o aluno; respondendo como e-mail já cadastrado", e);
                throw new ConflitoException(EMAIL_JA_CADASTRADO);
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<AlunoDTO> listar(StatusAluno status) {
        EnumSet<StatusAluno> filtro = status == null
                ? EnumSet.of(StatusAluno.CONVIDADO, StatusAluno.ATIVO)
                : EnumSet.of(status);
        return alunoRepository.findByPersonalIdAndStatusInOrderByUsuarioNomeAsc(usuarioAtual.id(), filtro).stream()
                .map(AlunoDTO::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public AlunoDTO buscar(UUID id) {
        return AlunoDTO.de(doPersonal(id));
    }

    /** Altera nome e dados do aluno; o e-mail não é editável. */
    @Transactional
    public AlunoDTO atualizar(UUID id, AtualizarAlunoRequest req) {
        Aluno aluno = doPersonal(id);
        aluno.getUsuario().alterarNome(req.nome().trim());
        aluno.atualizarDados(req.telefone(), req.dataNascimento(), req.objetivo(), req.observacoes());
        return AlunoDTO.de(aluno);
    }

    /**
     * {@code INATIVO} a partir de qualquer status; {@code ATIVO} só a partir de {@code INATIVO} ou {@code ATIVO}
     * (quem passa de {@code CONVIDADO} para {@code ATIVO} é o próprio aluno, no primeiro acesso). Reativar um aluno
     * que nunca entrou o devolve a {@code CONVIDADO}, para que o convite possa ser reenviado.
     */
    @Transactional
    public AlunoDTO alterarStatus(UUID id, StatusAluno novo) {
        if (novo == StatusAluno.CONVIDADO) {
            throw new ConflitoException("Status inválido.");
        }
        Aluno aluno = doPersonal(id);
        if (novo == StatusAluno.ATIVO && aluno.getStatus() == StatusAluno.CONVIDADO) {
            throw new ConflitoException("O aluno ainda não aceitou o convite.");
        }
        if (novo == StatusAluno.ATIVO && aluno.getPrimeiroAcessoEm() == null) {
            aluno.alterarStatus(StatusAluno.CONVIDADO);
        } else {
            aluno.alterarStatus(novo);
        }
        return AlunoDTO.de(aluno);
    }

    /**
     * Reenvia o convite pelo Supabase (o id devolvido é ignorado). Sem {@code @Transactional} de propósito: a
     * chamada HTTP não segura transação aberta; o {@code usuario} já vem carregado por {@link #doPersonal}.
     */
    public void reenviarConvite(UUID id) {
        Aluno aluno = doPersonal(id);
        if (aluno.getStatus() != StatusAluno.CONVIDADO) {
            throw new ConflitoException("Só é possível reenviar o convite de um aluno com convite pendente.");
        }
        supabaseAdmin.convidar(aluno.getUsuario().getEmail(), aluno.getUsuario().getNome());
    }

    /** O aluno {@code id} do personal autenticado; aluno de outro personal é tratado como inexistente. */
    Aluno doPersonal(UUID id) {
        return alunoRepository.findByIdAndPersonalId(id, usuarioAtual.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));
    }

    private void excluirNoSupabase(UUID id) {
        try {
            supabaseAdmin.excluirUsuario(id);
        } catch (RuntimeException e) {
            log.error("Falha ao excluir no Supabase o usuário {} após erro ao gravar o aluno", id, e);
        }
    }
}
