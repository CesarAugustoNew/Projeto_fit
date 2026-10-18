package com.projetofit.sessao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.projetofit.TesteIntegracao;
import com.projetofit.aluno.Aluno;
import com.projetofit.aluno.AlunoRepository;
import com.projetofit.aluno.StatusAluno;
import com.projetofit.exercicio.ExercicioRepository;
import com.projetofit.ficha.Divisao;
import com.projetofit.ficha.Ficha;
import com.projetofit.ficha.FichaRepository;
import com.projetofit.seguranca.TokenDeTeste;
import com.projetofit.usuario.Papel;
import com.projetofit.usuario.Usuario;
import com.projetofit.usuario.UsuarioRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TreinoDoAlunoControllerIT extends TesteIntegracao {

    private static final UUID PERSONAL_ID = UUID.randomUUID();
    private static final String URL = "/api/me/ficha-ativa";

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    AlunoRepository alunoRepository;

    @Autowired
    ExercicioRepository exercicioRepository;

    @Autowired
    FichaRepository fichaRepository;

    @Autowired
    SessaoRepository sessaoRepository;

    private UUID alunoUsuarioId;
    private Aluno aluno;

    @BeforeEach
    void preparar() {
        sessaoRepository.deleteAll();
        fichaRepository.deleteAll();
        exercicioRepository.deleteAll();
        alunoRepository.deleteAll();
        usuarioRepository.deleteAll();
        usuarioRepository.save(new Usuario(PERSONAL_ID, "personal", "personal@teste.com", Papel.PERSONAL));
        alunoUsuarioId = UUID.randomUUID();
        Usuario usuario = usuarioRepository.save(new Usuario(alunoUsuarioId, "Ana", "ana@teste.com", Papel.ALUNO));
        aluno = new Aluno(usuario, PERSONAL_ID, null, null, null, null);
        aluno.alterarStatus(StatusAluno.ATIVO);
        aluno = alunoRepository.save(aluno);
    }

    @Test
    void semFichaAtivaRetorna204() throws Exception {
        mvc.perform(get(URL).header("Authorization", bearerAluno()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void semSessaoSugereAPrimeiraDivisao() throws Exception {
        Ficha ficha = gravarFicha();

        mvc.perform(get(URL).header("Authorization", bearerAluno()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proximaDivisaoId").value(divisao(ficha, 1).getId().toString()))
                .andExpect(jsonPath("$.ficha.divisoes.length()").value(3));
    }

    @Test
    void depoisDeAsugereB() throws Exception {
        Ficha ficha = gravarFicha();
        gravarSessao(divisao(ficha, 1), Instant.parse("2026-10-01T10:00:00Z"));

        mvc.perform(get(URL).header("Authorization", bearerAluno()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proximaDivisaoId").value(divisao(ficha, 2).getId().toString()));
    }

    @Test
    void depoisDaUltimaVoltaParaAPrimeira() throws Exception {
        Ficha ficha = gravarFicha();
        gravarSessao(divisao(ficha, 1), Instant.parse("2026-10-01T10:00:00Z"));
        gravarSessao(divisao(ficha, 3), Instant.parse("2026-10-02T10:00:00Z"));

        mvc.perform(get(URL).header("Authorization", bearerAluno()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proximaDivisaoId").value(divisao(ficha, 1).getId().toString()));
    }

    @Test
    void sessaoDeFichaAnteriorNaoConta() throws Exception {
        Ficha antiga = gravarFicha();
        gravarSessao(divisao(antiga, 1), Instant.parse("2026-10-01T10:00:00Z"));
        antiga.encerrar();
        fichaRepository.saveAndFlush(antiga);
        Ficha nova = gravarFicha();

        mvc.perform(get(URL).header("Authorization", bearerAluno()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ficha.id").value(nova.getId().toString()))
                .andExpect(jsonPath("$.proximaDivisaoId").value(divisao(nova, 1).getId().toString()));
    }

    @Test
    void sessaoComDivisaoRemovidaEhIgnorada() throws Exception {
        Ficha ficha = gravarFicha();
        gravarSessao(divisao(ficha, 1), Instant.parse("2026-10-01T10:00:00Z"));
        sessaoRepository.save(new Sessao(UUID.randomUUID(), aluno, null, "Removida",
                Instant.parse("2026-10-02T09:00:00Z"), Instant.parse("2026-10-02T10:00:00Z"), null));

        mvc.perform(get(URL).header("Authorization", bearerAluno()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proximaDivisaoId").value(divisao(ficha, 2).getId().toString()));
    }

    @Test
    void personalNaoAcessa() throws Exception {
        mvc.perform(get(URL).header("Authorization",
                        "Bearer " + TokenDeTeste.gerar(PERSONAL_ID, "personal@teste.com")))
                .andExpect(status().isForbidden());
    }

    private Ficha gravarFicha() {
        Ficha ficha = new Ficha(aluno, "Ficha", null, 3, LocalDate.now(), null);
        ficha.adicionarDivisao("A", 1);
        ficha.adicionarDivisao("B", 2);
        ficha.adicionarDivisao("C", 3);
        return fichaRepository.saveAndFlush(ficha);
    }

    private static Divisao divisao(Ficha ficha, int ordem) {
        return ficha.getDivisoes().stream().filter(d -> d.getOrdem() == ordem).findFirst().orElseThrow();
    }

    private void gravarSessao(Divisao divisao, Instant finalizadaEm) {
        sessaoRepository.save(new Sessao(UUID.randomUUID(), aluno, divisao, divisao.getNome(),
                finalizadaEm.minusSeconds(3600), finalizadaEm, null));
    }

    private String bearerAluno() {
        return "Bearer " + TokenDeTeste.gerar(alunoUsuarioId, "ana@teste.com");
    }
}
