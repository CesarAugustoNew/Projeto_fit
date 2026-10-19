package com.projetofit.desempenho;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.projetofit.TesteIntegracao;
import com.projetofit.aluno.Aluno;
import com.projetofit.aluno.AlunoRepository;
import com.projetofit.aluno.StatusAluno;
import com.projetofit.ficha.Ficha;
import com.projetofit.ficha.FichaRepository;
import com.projetofit.seguranca.TokenDeTeste;
import com.projetofit.sessao.Sessao;
import com.projetofit.sessao.SessaoRepository;
import com.projetofit.usuario.Papel;
import com.projetofit.usuario.Usuario;
import com.projetofit.usuario.UsuarioRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class DashboardIT extends TesteIntegracao {

    private static final UUID PERSONAL_ID = UUID.randomUUID();
    private static final UUID OUTRO_PERSONAL_ID = UUID.randomUUID();

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    AlunoRepository alunoRepository;

    @Autowired
    FichaRepository fichaRepository;

    @Autowired
    SessaoRepository sessaoRepository;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Clock clock;

    @BeforeEach
    void preparar() {
        sessaoRepository.deleteAll();
        fichaRepository.deleteAll();
        alunoRepository.deleteAll();
        usuarioRepository.deleteAll();
        usuarioRepository.save(new Usuario(PERSONAL_ID, "personal", "personal@teste.com", Papel.PERSONAL));
        usuarioRepository.save(new Usuario(OUTRO_PERSONAL_ID, "outro", "outro@teste.com", Papel.PERSONAL));
    }

    @Test
    void resumoDaSemanaDeCadaAluno() throws Exception {
        Aluno ana = gravarAluno("Ana", PERSONAL_ID, StatusAluno.ATIVO);
        Ficha ficha = gravarFicha(ana, 3);
        Instant agora = Instant.now(clock);
        Instant inicioDaSemana = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(clock.getZone()).toInstant();
        // Segunda-feira logo apos a meia-noite e o instante atual (ambos dentro da semana atual).
        gravarSessao(ana, inicioDaSemana.plusSeconds(60));
        gravarSessao(ana, agora);

        mvc.perform(get("/api/dashboard").header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].alunoId").value(ana.getId().toString()))
                .andExpect(jsonPath("$[0].nome").value("Ana"))
                .andExpect(jsonPath("$[0].realizadosSemana").value(2))
                .andExpect(jsonPath("$[0].planejadoSemana").value(3))
                .andExpect(jsonPath("$[0].diasSemTreinar").value(0))
                .andExpect(jsonPath("$[0].fichaAtivaId").value(ficha.getId().toString()))
                .andExpect(jsonPath("$[0].sumiu").value(false));
    }

    @Test
    void alunoQueSumiuVemPrimeiro() throws Exception {
        Aluno ana = gravarAluno("Ana", PERSONAL_ID, StatusAluno.ATIVO);
        gravarFicha(ana, 3);
        gravarSessao(ana, Instant.now(clock));
        Aluno zeca = gravarAluno("Zeca", PERSONAL_ID, StatusAluno.ATIVO);
        gravarFicha(zeca, 3);
        gravarSessao(zeca, Instant.now(clock).minus(10, ChronoUnit.DAYS));

        mvc.perform(get("/api/dashboard").header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Zeca"))
                .andExpect(jsonPath("$[0].sumiu").value(true))
                .andExpect(jsonPath("$[0].diasSemTreinar").value(10))
                .andExpect(jsonPath("$[1].nome").value("Ana"))
                .andExpect(jsonPath("$[1].sumiu").value(false));
    }

    @Test
    void nuncaTreinouComFichaRecenteNaoSumiu() throws Exception {
        Aluno ana = gravarAluno("Ana", PERSONAL_ID, StatusAluno.ATIVO);
        gravarFicha(ana, 3);

        mvc.perform(get("/api/dashboard").header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diasSemTreinar").value(nullValue()))
                .andExpect(jsonPath("$[0].sumiu").value(false));
    }

    @Test
    void nuncaTreinouComFichaAntigaSumiu() throws Exception {
        Aluno ana = gravarAluno("Ana", PERSONAL_ID, StatusAluno.ATIVO);
        Ficha ficha = gravarFicha(ana, 3);
        jdbc.update("update ficha set criado_em = ? where id = ?",
                Timestamp.from(Instant.now(clock).minus(10, ChronoUnit.DAYS)), ficha.getId());

        mvc.perform(get("/api/dashboard").header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diasSemTreinar").value(nullValue()))
                .andExpect(jsonPath("$[0].sumiu").value(true));
    }

    @Test
    void fichaVencidaSinalizada() throws Exception {
        Aluno ana = gravarAluno("Ana", PERSONAL_ID, StatusAluno.ATIVO);
        fichaRepository.save(new Ficha(ana, "Ficha", null, 3, LocalDate.now(clock).minusDays(30),
                LocalDate.now(clock).minusDays(1)));

        mvc.perform(get("/api/dashboard").header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fichaVencida").value(true));
    }

    @Test
    void semFichaTemPlanejadoZero() throws Exception {
        gravarAluno("Ana", PERSONAL_ID, StatusAluno.ATIVO);

        mvc.perform(get("/api/dashboard").header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fichaAtivaId").value(nullValue()))
                .andExpect(jsonPath("$[0].planejadoSemana").value(0))
                .andExpect(jsonPath("$[0].fichaVencida").value(false))
                .andExpect(jsonPath("$[0].sumiu").value(false));
    }

    @Test
    void fichaEncerradaNaSemanaAindaDefinePlanejadoMasNaoEhFichaAtiva() throws Exception {
        Aluno ana = gravarAluno("Ana", PERSONAL_ID, StatusAluno.ATIVO);
        Ficha ficha = gravarFicha(ana, 4);
        ficha.encerrar();
        fichaRepository.saveAndFlush(ficha);
        Instant inicioDaSemana = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(clock.getZone()).toInstant();
        jdbc.update("update ficha set criado_em = ?, encerrada_em = ? where id = ?",
                Timestamp.from(inicioDaSemana.minus(3, ChronoUnit.DAYS)),
                Timestamp.from(inicioDaSemana.plusSeconds(60)), ficha.getId());

        mvc.perform(get("/api/dashboard").header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].planejadoSemana").value(4))
                .andExpect(jsonPath("$[0].fichaAtivaId").value(nullValue()))
                .andExpect(jsonPath("$[0].fichaVencida").value(false));
    }

    @Test
    void soAlunosAtivosDoPersonal() throws Exception {
        gravarAluno("Ana", PERSONAL_ID, StatusAluno.ATIVO);
        gravarAluno("Convidado", PERSONAL_ID, StatusAluno.CONVIDADO);
        gravarAluno("Inativo", PERSONAL_ID, StatusAluno.INATIVO);
        gravarAluno("Alheio", OUTRO_PERSONAL_ID, StatusAluno.ATIVO);

        mvc.perform(get("/api/dashboard").header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Ana"));
    }

    @Test
    void alunoNaoAcessaDashboard() throws Exception {
        Aluno ana = gravarAluno("Ana", PERSONAL_ID, StatusAluno.ATIVO);

        mvc.perform(get("/api/dashboard").header("Authorization",
                        "Bearer " + TokenDeTeste.gerar(ana.getUsuario().getId(), "ana@teste.com")))
                .andExpect(status().isForbidden());
    }

    private Aluno gravarAluno(String nome, UUID personalId, StatusAluno statusAluno) {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = usuarioRepository.save(
                new Usuario(usuarioId, nome, nome.toLowerCase() + "@teste.com", Papel.ALUNO));
        Aluno novo = new Aluno(usuario, personalId, null, null, null, null);
        novo.alterarStatus(statusAluno);
        return alunoRepository.save(novo);
    }

    private Ficha gravarFicha(Aluno aluno, int frequencia) {
        return fichaRepository.save(new Ficha(aluno, "Ficha", null, frequencia, LocalDate.now(clock), null));
    }

    private void gravarSessao(Aluno aluno, Instant fim) {
        sessaoRepository.saveAndFlush(
                new Sessao(UUID.randomUUID(), aluno, null, "Treino", fim.minusSeconds(3600), fim, null));
    }

    private static String bearerPersonal() {
        return "Bearer " + TokenDeTeste.gerar(PERSONAL_ID, "personal@teste.com");
    }
}
