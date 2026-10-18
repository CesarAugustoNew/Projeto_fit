package com.projetofit.sessao;

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
import com.projetofit.usuario.Papel;
import com.projetofit.usuario.Usuario;
import com.projetofit.usuario.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class HistoricoSessoesIT extends TesteIntegracao {

    private static final UUID PERSONAL_ID = UUID.randomUUID();
    private static final UUID OUTRO_PERSONAL_ID = UUID.randomUUID();
    private static final UUID USUARIO_ANA_ID = UUID.randomUUID();
    private static final UUID USUARIO_BRUNO_ID = UUID.randomUUID();
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

    private Aluno ana;
    private Aluno bruno;
    private Aluno carla;
    private Exercicio supino;
    private Exercicio crucifixo;
    private Sessao maisAntiga;
    private Sessao maisRecente;
    private Sessao sessaoDoBruno;

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
        bruno = gravarAluno(USUARIO_BRUNO_ID, "Bruno", "bruno@teste.com", PERSONAL_ID);
        carla = gravarAluno(USUARIO_CARLA_ID, "Carla", "carla@teste.com", OUTRO_PERSONAL_ID);
        supino = exercicioRepository.save(new Exercicio(PERSONAL_ID, "Supino reto", GrupoMuscular.PEITO, null, null));
        crucifixo = exercicioRepository.save(new Exercicio(PERSONAL_ID, "Crucifixo", GrupoMuscular.PEITO, null, null));

        Instant base = Instant.now().minus(10, ChronoUnit.DAYS);
        maisAntiga = gravarSessao(ana, "Treino A", base, 1, 1);
        gravarSessao(ana, "Treino B", base.plus(2, ChronoUnit.DAYS), 1, 1);
        maisRecente = gravarSessao(ana, "Treino C", base.plus(4, ChronoUnit.DAYS), 2, 6);
        sessaoDoBruno = gravarSessao(bruno, "Treino do Bruno", base.plus(3, ChronoUnit.DAYS), 1, 1);
    }

    @Test
    void minhasSessoesPaginadasDaMaisRecente() throws Exception {
        mvc.perform(get("/api/me/sessoes?size=2").header("Authorization", bearer(USUARIO_ANA_ID, "ana@teste.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(maisRecente.getId().toString()))
                .andExpect(jsonPath("$.content[0].divisaoNome").value("Treino C"))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    void resumoContaExerciciosESeries() throws Exception {
        mvc.perform(get("/api/me/sessoes").header("Authorization", bearer(USUARIO_ANA_ID, "ana@teste.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(maisRecente.getId().toString()))
                .andExpect(jsonPath("$.content[0].quantidadeExercicios").value(2))
                .andExpect(jsonPath("$.content[0].quantidadeSeries").value(6))
                .andExpect(jsonPath("$.content[0].iniciadaEm").exists())
                .andExpect(jsonPath("$.content[0].finalizadaEm").exists())
                .andExpect(jsonPath("$.content[2].quantidadeExercicios").value(1))
                .andExpect(jsonPath("$.content[2].quantidadeSeries").value(1));
    }

    @Test
    void personalVeSessoesDoAluno() throws Exception {
        mvc.perform(get("/api/alunos/" + ana.getId() + "/sessoes").header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    void personalNaoVeAlunoDeOutroPersonal() throws Exception {
        mvc.perform(get("/api/alunos/" + carla.getId() + "/sessoes").header("Authorization", bearerPersonal()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Aluno não encontrado."));
    }

    @Test
    void alunoNaoAcessaListaDoPersonal() throws Exception {
        mvc.perform(get("/api/alunos/" + ana.getId() + "/sessoes")
                        .header("Authorization", bearer(USUARIO_ANA_ID, "ana@teste.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void alunoVeDetalheDaPropriaSessao() throws Exception {
        mvc.perform(get("/api/sessoes/" + maisRecente.getId())
                        .header("Authorization", bearer(USUARIO_ANA_ID, "ana@teste.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(maisRecente.getId().toString()))
                .andExpect(jsonPath("$.series.length()").value(6));
    }

    @Test
    void alunoNaoVeSessaoDeOutroAluno() throws Exception {
        mvc.perform(get("/api/sessoes/" + sessaoDoBruno.getId())
                        .header("Authorization", bearer(USUARIO_ANA_ID, "ana@teste.com")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Sessão não encontrada."));
    }

    @Test
    void personalVeDetalheDeSessaoDoSeuAluno() throws Exception {
        mvc.perform(get("/api/sessoes/" + maisAntiga.getId()).header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(maisAntiga.getId().toString()));
    }

    @Test
    void personalNaoVeSessaoDeAlunoDeOutroPersonal() throws Exception {
        Sessao daCarla = gravarSessao(carla, "Treino da Carla", Instant.now().minus(1, ChronoUnit.DAYS), 1, 1);

        mvc.perform(get("/api/sessoes/" + daCarla.getId()).header("Authorization", bearerPersonal()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Sessão não encontrada."));
    }

    @Test
    void tamanhoDePaginaLimitadoA50() throws Exception {
        mvc.perform(get("/api/me/sessoes?size=500").header("Authorization", bearer(USUARIO_ANA_ID, "ana@teste.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(50));
    }

    private Aluno gravarAluno(UUID usuarioId, String nome, String email, UUID personalId) {
        Usuario usuario = usuarioRepository.save(new Usuario(usuarioId, nome, email, Papel.ALUNO));
        Aluno novo = new Aluno(usuario, personalId, null, null, null, null);
        novo.alterarStatus(StatusAluno.ATIVO);
        return alunoRepository.save(novo);
    }

    /** Sessão com {@code exercicios} exercícios distintos (1 ou 2) e {@code series} séries no total. */
    private Sessao gravarSessao(Aluno aluno, String divisaoNome, Instant finalizadaEm, int exercicios, int series) {
        Sessao sessao = new Sessao(UUID.randomUUID(), aluno, null, divisaoNome,
                finalizadaEm.minus(1, ChronoUnit.HOURS), finalizadaEm, null);
        for (int i = 0; i < series; i++) {
            Exercicio exercicio = exercicios == 2 && i >= series / 2 ? crucifixo : supino;
            sessao.adicionarSerie(exercicio, null, i + 1, i % 3 + 1, 10, new BigDecimal("20"));
        }
        return sessaoRepository.saveAndFlush(sessao);
    }

    private static String bearer(UUID usuarioId, String email) {
        return "Bearer " + TokenDeTeste.gerar(usuarioId, email);
    }

    private static String bearerPersonal() {
        return bearer(PERSONAL_ID, "personal@teste.com");
    }
}
