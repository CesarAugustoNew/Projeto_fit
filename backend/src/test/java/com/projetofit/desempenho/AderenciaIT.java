package com.projetofit.desempenho;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.projetofit.TesteIntegracao;
import com.projetofit.aluno.Aluno;
import com.projetofit.aluno.AlunoRepository;
import com.projetofit.aluno.StatusAluno;
import com.projetofit.exercicio.ExercicioRepository;
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

class AderenciaIT extends TesteIntegracao {

    private static final UUID PERSONAL_ID = UUID.randomUUID();
    private static final UUID OUTRO_PERSONAL_ID = UUID.randomUUID();
    private static final UUID USUARIO_ANA_ID = UUID.randomUUID();
    private static final UUID USUARIO_CARLA_ID = UUID.randomUUID();

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    AlunoRepository alunoRepository;

    @Autowired
    FichaRepository fichaRepository;

    @Autowired
    ExercicioRepository exercicioRepository;

    @Autowired
    SessaoRepository sessaoRepository;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Clock clock;

    private Aluno ana;
    private Aluno carla;

    @BeforeEach
    void preparar() {
        sessaoRepository.deleteAll();
        fichaRepository.deleteAll();
        exercicioRepository.deleteAll();
        alunoRepository.deleteAll();
        usuarioRepository.deleteAll();
        usuarioRepository.save(new Usuario(PERSONAL_ID, "personal", "personal@teste.com", Papel.PERSONAL));
        usuarioRepository.save(new Usuario(OUTRO_PERSONAL_ID, "outro", "outro@teste.com", Papel.PERSONAL));
        ana = gravarAluno(USUARIO_ANA_ID, "Ana", "ana@teste.com", PERSONAL_ID);
        carla = gravarAluno(USUARIO_CARLA_ID, "Carla", "carla@teste.com", OUTRO_PERSONAL_ID);
    }

    @Test
    void semanasPadraoOito() throws Exception {
        mvc.perform(get("/api/alunos/" + ana.getId() + "/desempenho/aderencia")
                        .header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[7].percentual").value(nullValue()));
    }

    @Test
    void semanasForaDoLimiteRetorna400() throws Exception {
        mvc.perform(get("/api/alunos/" + ana.getId() + "/desempenho/aderencia?semanas=30")
                        .header("Authorization", bearerPersonal()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/me/desempenho/aderencia?semanas=0").header("Authorization", bearerAna()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void alunoVeAPropriaAderencia() throws Exception {
        Ficha ficha = fichaRepository.save(new Ficha(ana, "Ficha", null, 2, LocalDate.of(2026, 1, 1), null));
        Instant ha30Dias = Instant.now(clock).minus(30, ChronoUnit.DAYS);
        jdbc.update("update ficha set criado_em = ? where id = ?", Timestamp.from(ha30Dias), ficha.getId());
        Instant agora = Instant.now(clock);
        Instant inicioDaSemana = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(clock.getZone()).toInstant();
        Instant segundaAoMeioDia = inicioDaSemana.plus(12, ChronoUnit.HOURS);
        Instant fim = segundaAoMeioDia.isBefore(agora) ? segundaAoMeioDia : agora;
        sessaoRepository.saveAndFlush(
                new Sessao(UUID.randomUUID(), ana, null, "Treino", fim.minusSeconds(3600), fim, null));

        mvc.perform(get("/api/me/desempenho/aderencia?semanas=2").header("Authorization", bearerAna()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].planejado").value(2))
                .andExpect(jsonPath("$[0].realizados").value(0))
                .andExpect(jsonPath("$[0].percentual").value(0))
                .andExpect(jsonPath("$[1].planejado").value(2))
                .andExpect(jsonPath("$[1].realizados").value(1))
                .andExpect(jsonPath("$[1].percentual").value(50));
    }

    @Test
    void personalNaoVeAlunoDeOutroPersonal() throws Exception {
        mvc.perform(get("/api/alunos/" + carla.getId() + "/desempenho/aderencia")
                        .header("Authorization", bearerPersonal()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Aluno não encontrado."));
    }

    private Aluno gravarAluno(UUID usuarioId, String nome, String email, UUID personalId) {
        Usuario usuario = usuarioRepository.save(new Usuario(usuarioId, nome, email, Papel.ALUNO));
        Aluno novo = new Aluno(usuario, personalId, null, null, null, null);
        novo.alterarStatus(StatusAluno.ATIVO);
        return alunoRepository.save(novo);
    }

    private static String bearerPersonal() {
        return "Bearer " + TokenDeTeste.gerar(PERSONAL_ID, "personal@teste.com");
    }

    private static String bearerAna() {
        return "Bearer " + TokenDeTeste.gerar(USUARIO_ANA_ID, "ana@teste.com");
    }
}
