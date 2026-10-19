package com.projetofit.desempenho;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.projetofit.TesteIntegracao;
import com.projetofit.aluno.Aluno;
import com.projetofit.aluno.AlunoRepository;
import com.projetofit.aluno.StatusAluno;
import com.projetofit.exercicio.Exercicio;
import com.projetofit.exercicio.ExercicioRepository;
import com.projetofit.exercicio.GrupoMuscular;
import com.projetofit.ficha.FichaRepository;
import com.projetofit.seguranca.TokenDeTeste;
import com.projetofit.sessao.Sessao;
import com.projetofit.sessao.SessaoRepository;
import com.projetofit.usuario.Papel;
import com.projetofit.usuario.Usuario;
import com.projetofit.usuario.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EvolucaoIT extends TesteIntegracao {

    private static final UUID PERSONAL_ID = UUID.randomUUID();
    private static final UUID OUTRO_PERSONAL_ID = UUID.randomUUID();
    private static final UUID USUARIO_ANA_ID = UUID.randomUUID();
    private static final UUID USUARIO_CARLA_ID = UUID.randomUUID();

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

    @Autowired
    Clock clock;

    private Aluno ana;
    private Aluno carla;
    private Exercicio supino;
    private Exercicio prancha;

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
        supino = exercicioRepository.save(new Exercicio(PERSONAL_ID, "Supino reto", GrupoMuscular.PEITO, null, null));
        prancha = exercicioRepository.save(new Exercicio(PERSONAL_ID, "Prancha", GrupoMuscular.ABDOMEN, null, null));
    }

    @Test
    void evolucaoPorSessaoEmOrdemCronologica() throws Exception {
        LocalDate hoje = LocalDate.now(clock);
        gravarSessao(ana, hoje.minusDays(1), supino, "45", 8, 2);
        gravarSessao(ana, hoje.minusDays(5), supino, "40", 10, 2);
        Sessao segunda = gravarSessao(ana, hoje.minusDays(3), supino, "42.5", 10, 2);
        // outro exercício na mesma sessão não entra na conta do supino
        segunda.adicionarSerie(prancha, null, 9, 1, 1, new BigDecimal("100"));
        sessaoRepository.saveAndFlush(segunda);

        mvc.perform(get("/api/alunos/" + ana.getId() + "/desempenho/evolucao?exercicioId=" + supino.getId())
                        .header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].data").value(hoje.minusDays(5).toString()))
                .andExpect(jsonPath("$[0].cargaMaxima").value(40.0))
                .andExpect(jsonPath("$[0].volume").value(800.0))
                .andExpect(jsonPath("$[1].data").value(hoje.minusDays(3).toString()))
                .andExpect(jsonPath("$[1].sessaoId").value(segunda.getId().toString()))
                .andExpect(jsonPath("$[1].cargaMaxima").value(42.5))
                .andExpect(jsonPath("$[1].volume").value(850.0))
                .andExpect(jsonPath("$[2].data").value(hoje.minusDays(1).toString()))
                .andExpect(jsonPath("$[2].cargaMaxima").value(45.0))
                .andExpect(jsonPath("$[2].volume").value(720.0));
    }

    @Test
    void exercicioSemCargaTemCargaMaximaNula() throws Exception {
        gravarSessao(ana, LocalDate.now(clock).minusDays(1), prancha, null, 1, 3);

        mvc.perform(get("/api/alunos/" + ana.getId() + "/desempenho/evolucao?exercicioId=" + prancha.getId())
                        .header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cargaMaxima").doesNotExist())
                .andExpect(jsonPath("$[0].volume").value(0));
    }

    @Test
    void cargasDiferentesNaMesmaSessaoUsamMaximoESomaVolume() throws Exception {
        Sessao sessao = gravarSessao(ana, LocalDate.now(clock).minusDays(1), supino, "40", 10, 1);
        sessao.adicionarSerie(supino, null, 2, 2, 10, new BigDecimal("42.5"));
        sessaoRepository.saveAndFlush(sessao);

        mvc.perform(get("/api/alunos/" + ana.getId() + "/desempenho/evolucao?exercicioId=" + supino.getId())
                        .header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cargaMaxima").value(42.5))
                .andExpect(jsonPath("$[0].volume").value(825.0));
    }

    @Test
    void sessaoMistaComSeriesSemCargaContaVolumeSoDasComCarga() throws Exception {
        Sessao sessao = gravarSessao(ana, LocalDate.now(clock).minusDays(1), supino, "40", 10, 1);
        sessao.adicionarSerie(supino, null, 2, 2, 10, null);
        sessaoRepository.saveAndFlush(sessao);

        mvc.perform(get("/api/alunos/" + ana.getId() + "/desempenho/evolucao?exercicioId=" + supino.getId())
                        .header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cargaMaxima").value(40.0))
                .andExpect(jsonPath("$[0].volume").value(400.0));
    }

    @Test
    void periodoPadraoSaoSeisMeses() throws Exception {
        LocalDate hoje = LocalDate.now(clock);
        gravarSessao(ana, hoje.minusMonths(7), supino, "40", 10, 1);
        gravarSessao(ana, hoje.minusMonths(5), supino, "41", 10, 1);

        mvc.perform(get("/api/me/desempenho/evolucao?exercicioId=" + supino.getId())
                        .header("Authorization", bearerAna()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].data").value(hoje.minusMonths(5).toString()));
    }

    @Test
    void periodoInvalidoRetorna400() throws Exception {
        mvc.perform(get("/api/alunos/" + ana.getId() + "/desempenho/evolucao?exercicioId=" + supino.getId()
                        + "&de=2026-05-01&ate=2026-04-01")
                        .header("Authorization", bearerPersonal()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.de").value("A data inicial deve ser anterior ou igual à final."));
    }

    @Test
    void exerciciosExecutadosSemRepeticaoPorNome() throws Exception {
        LocalDate hoje = LocalDate.now(clock);
        gravarSessao(ana, hoje.minusDays(2), supino, "40", 10, 3);
        gravarSessao(ana, hoje.minusDays(1), supino, "42", 10, 3);
        gravarSessao(ana, hoje.minusDays(1), prancha, null, 1, 3);

        mvc.perform(get("/api/alunos/" + ana.getId() + "/desempenho/exercicios")
                        .header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Prancha"))
                .andExpect(jsonPath("$[0].grupoMuscular").value("ABDOMEN"))
                .andExpect(jsonPath("$[1].nome").value("Supino reto"));

        mvc.perform(get("/api/me/desempenho/exercicios").header("Authorization", bearerAna()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void alunoVeAPropriaEvolucaoPeloMe() throws Exception {
        gravarSessao(ana, LocalDate.now(clock).minusDays(1), supino, "40", 10, 1);

        mvc.perform(get("/api/me/desempenho/evolucao?exercicioId=" + supino.getId())
                        .header("Authorization", bearerAna()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void personalNaoVeAlunoDeOutroPersonal() throws Exception {
        mvc.perform(get("/api/alunos/" + carla.getId() + "/desempenho/evolucao?exercicioId=" + supino.getId())
                        .header("Authorization", bearerPersonal()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Aluno não encontrado."));
        mvc.perform(get("/api/alunos/" + carla.getId() + "/desempenho/exercicios")
                        .header("Authorization", bearerPersonal()))
                .andExpect(status().isNotFound());
    }

    @Test
    void alunoNaoUsaRotaDoPersonal() throws Exception {
        mvc.perform(get("/api/alunos/" + ana.getId() + "/desempenho/evolucao?exercicioId=" + supino.getId())
                        .header("Authorization", bearerAna()))
                .andExpect(status().isForbidden());
    }

    private Aluno gravarAluno(UUID usuarioId, String nome, String email, UUID personalId) {
        Usuario usuario = usuarioRepository.save(new Usuario(usuarioId, nome, email, Papel.ALUNO));
        Aluno novo = new Aluno(usuario, personalId, null, null, null, null);
        novo.alterarStatus(StatusAluno.ATIVO);
        return alunoRepository.save(novo);
    }

    /** Sessão finalizada ao meio-dia (fuso do relógio) de {@code dia}, com {@code series} séries do exercício. */
    private Sessao gravarSessao(Aluno aluno, LocalDate dia, Exercicio exercicio, String carga, int repeticoes,
                                int series) {
        Instant fim = dia.atTime(12, 0).atZone(clock.getZone()).toInstant();
        Sessao sessao = new Sessao(UUID.randomUUID(), aluno, null, "Treino", fim.minusSeconds(3600), fim, null);
        for (int i = 0; i < series; i++) {
            sessao.adicionarSerie(exercicio, null, i + 1, i + 1, repeticoes,
                    carga == null ? null : new BigDecimal(carga));
        }
        return sessaoRepository.saveAndFlush(sessao);
    }

    private static String bearerPersonal() {
        return "Bearer " + TokenDeTeste.gerar(PERSONAL_ID, "personal@teste.com");
    }

    private static String bearerAna() {
        return "Bearer " + TokenDeTeste.gerar(USUARIO_ANA_ID, "ana@teste.com");
    }
}
