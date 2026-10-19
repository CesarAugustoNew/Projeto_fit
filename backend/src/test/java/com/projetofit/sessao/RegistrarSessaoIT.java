package com.projetofit.sessao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.projetofit.TesteIntegracao;
import com.projetofit.aluno.Aluno;
import com.projetofit.aluno.AlunoRepository;
import com.projetofit.aluno.StatusAluno;
import com.projetofit.exercicio.Exercicio;
import com.projetofit.exercicio.ExercicioRepository;
import com.projetofit.exercicio.GrupoMuscular;
import com.projetofit.ficha.Divisao;
import com.projetofit.ficha.Ficha;
import com.projetofit.ficha.FichaRepository;
import com.projetofit.ficha.ItemDivisao;
import com.projetofit.seguranca.TokenDeTeste;
import com.projetofit.usuario.Papel;
import com.projetofit.usuario.Usuario;
import com.projetofit.usuario.UsuarioRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

class RegistrarSessaoIT extends TesteIntegracao {

    private static final UUID PERSONAL_ID = UUID.randomUUID();
    private static final UUID OUTRO_PERSONAL_ID = UUID.randomUUID();
    private static final UUID USUARIO_ANA_ID = UUID.randomUUID();
    private static final UUID USUARIO_BRUNO_ID = UUID.randomUUID();

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
    private Exercicio supino;
    private Exercicio crucifixo;
    private Exercicio deOutroPersonal;
    private Ficha ficha;
    private Divisao treinoA;
    private Divisao treinoB;
    private ItemDivisao itemSupino;
    private ItemDivisao itemCrucifixo;
    private ItemDivisao itemDeB;

    @BeforeEach
    void preparar() {
        sessaoRepository.deleteAll();
        fichaRepository.deleteAll();
        exercicioRepository.deleteAll();
        alunoRepository.deleteAll();
        usuarioRepository.deleteAll();
        usuarioRepository.save(new Usuario(PERSONAL_ID, "personal", "personal@teste.com", Papel.PERSONAL));
        usuarioRepository.save(new Usuario(OUTRO_PERSONAL_ID, "outro", "outro@teste.com", Papel.PERSONAL));
        ana = gravarAluno(USUARIO_ANA_ID, "Ana", "ana@teste.com");
        bruno = gravarAluno(USUARIO_BRUNO_ID, "Bruno", "bruno@teste.com");
        supino = exercicioRepository.save(new Exercicio(PERSONAL_ID, "Supino reto", GrupoMuscular.PEITO, null, null));
        crucifixo = exercicioRepository.save(new Exercicio(PERSONAL_ID, "Crucifixo", GrupoMuscular.PEITO, null, null));
        deOutroPersonal = exercicioRepository.save(
                new Exercicio(OUTRO_PERSONAL_ID, "Remada", GrupoMuscular.COSTAS, null, null));

        Ficha nova = new Ficha(ana, "Ficha", null, 3, LocalDate.now(), null);
        Divisao a = nova.adicionarDivisao("Treino A", 1);
        a.adicionarItem(supino, 1, 3, "10", null, null, null);
        a.adicionarItem(crucifixo, 2, 3, "12", null, null, null);
        Divisao b = nova.adicionarDivisao("Treino B", 2);
        b.adicionarItem(supino, 1, 4, "8", null, null, null);
        ficha = fichaRepository.saveAndFlush(nova);
        treinoA = divisao(ficha, 1);
        treinoB = divisao(ficha, 2);
        itemSupino = item(treinoA, 1);
        itemCrucifixo = item(treinoA, 2);
        itemDeB = item(treinoB, 1);
    }

    @Test
    void registrarSessao() throws Exception {
        UUID clientId = UUID.randomUUID();
        String series = """
                [{"itemDivisaoId":"%s","exercicioId":"%s","numeroSerie":1,"repeticoes":12},
                 {"itemDivisaoId":"%s","exercicioId":"%s","numeroSerie":1,"repeticoes":10,"cargaKg":22.5},
                 {"itemDivisaoId":"%s","exercicioId":"%s","numeroSerie":2,"repeticoes":8,"cargaKg":22.5}]
                """.formatted(itemCrucifixo.getId(), crucifixo.getId(), itemSupino.getId(), supino.getId(),
                itemSupino.getId(), supino.getId());

        registrar(sessao(clientId, treinoA.getId(), "Outro nome", agora().minus(Duration.ofHours(1)),
                agora().minus(Duration.ofMinutes(5)), "\"  Pesado hoje  \"", series), bearerAna())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.alunoId").value(ana.getId().toString()))
                .andExpect(jsonPath("$.divisaoId").value(treinoA.getId().toString()))
                .andExpect(jsonPath("$.divisaoNome").value("Outro nome"))
                .andExpect(jsonPath("$.observacao").value("Pesado hoje"))
                .andExpect(jsonPath("$.series.length()").value(3))
                .andExpect(jsonPath("$.series[0].exercicioId").value(crucifixo.getId().toString()))
                .andExpect(jsonPath("$.series[0].exercicioNome").value("Crucifixo"))
                .andExpect(jsonPath("$.series[0].itemDivisaoId").value(itemCrucifixo.getId().toString()))
                .andExpect(jsonPath("$.series[0].ordem").value(1))
                .andExpect(jsonPath("$.series[0].repeticoes").value(12))
                .andExpect(jsonPath("$.series[0].cargaKg").doesNotExist())
                .andExpect(jsonPath("$.series[1].exercicioNome").value("Supino reto"))
                .andExpect(jsonPath("$.series[1].ordem").value(2))
                .andExpect(jsonPath("$.series[1].numeroSerie").value(1))
                .andExpect(jsonPath("$.series[1].cargaKg").value(22.5))
                .andExpect(jsonPath("$.series[2].ordem").value(3))
                .andExpect(jsonPath("$.series[2].numeroSerie").value(2));
    }

    @Test
    void mesmoClientIdDevolveAMesmaSessao() throws Exception {
        String json = sessaoPadrao(UUID.randomUUID(), treinoA.getId());
        String primeira = registrar(json, bearerAna())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(primeira, "$.id");

        registrar(json, bearerAna())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.series.length()").value(1));

        assertThat(sessaoRepository.count()).isEqualTo(1);
    }

    @Test
    void clientIdDeOutroAlunoRetorna409() throws Exception {
        UUID clientId = UUID.randomUUID();
        sessaoRepository.save(new Sessao(clientId, bruno, null, "Treino do Bruno",
                agora().minus(Duration.ofHours(2)), agora().minus(Duration.ofHours(1)), null));

        registrar(sessaoPadrao(clientId, treinoA.getId()), bearerAna())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Sessão já registrada."));
    }

    @Test
    void fimAntesDoInicioRetorna400() throws Exception {
        registrar(sessao(UUID.randomUUID(), treinoA.getId(), "Treino A", agora().minus(Duration.ofMinutes(10)),
                agora().minus(Duration.ofMinutes(20)), null, seriesPadrao()), bearerAna())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.finalizadaEm").value("O fim do treino deve ser depois do início."));
    }

    @Test
    void fimNoFuturoRetorna400() throws Exception {
        registrar(sessao(UUID.randomUUID(), treinoA.getId(), "Treino A", agora().minus(Duration.ofHours(1)),
                agora().plus(Duration.ofMinutes(10)), null, seriesPadrao()), bearerAna())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.finalizadaEm")
                        .value("O horário de fim está no futuro. Confira o relógio do celular."));
    }

    @Test
    void fimAteCincoMinutosNoFuturoEhAceito() throws Exception {
        registrar(sessao(UUID.randomUUID(), treinoA.getId(), "Treino A", agora().minus(Duration.ofHours(1)),
                agora().plus(Duration.ofMinutes(4)), null, seriesPadrao()), bearerAna())
                .andExpect(status().isCreated());
    }

    @Test
    void semSeriesRetorna400() throws Exception {
        registrar(sessao(UUID.randomUUID(), treinoA.getId(), "Treino A", agora().minus(Duration.ofHours(1)),
                agora().minus(Duration.ofMinutes(5)), null, "[]"), bearerAna())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.series").exists());
    }

    @Test
    void divisaoDeOutroAlunoRetorna400() throws Exception {
        Ficha doBruno = new Ficha(bruno, "Ficha do Bruno", null, 3, LocalDate.now(), null);
        doBruno.adicionarDivisao("Treino X", 1);
        doBruno = fichaRepository.saveAndFlush(doBruno);

        registrar(sessaoPadrao(UUID.randomUUID(), divisao(doBruno, 1).getId()), bearerAna())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.divisaoId").value("Divisão inválida."));
    }

    @Test
    void divisaoRemovidaDuranteOTreinoAindaRegistra() throws Exception {
        registrar(sessao(UUID.randomUUID(), UUID.randomUUID(), " Treino A ", agora().minus(Duration.ofHours(1)),
                agora().minus(Duration.ofMinutes(5)), null, seriesPadrao()), bearerAna())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.divisaoId").doesNotExist())
                .andExpect(jsonPath("$.divisaoNome").value("Treino A"))
                .andExpect(jsonPath("$.series[0].itemDivisaoId").doesNotExist());
    }

    @Test
    void itemRemovidoDuranteOTreinoAindaRegistra() throws Exception {
        String series = """
                [{"itemDivisaoId":"%s","exercicioId":"%s","numeroSerie":1,"repeticoes":10},
                 {"itemDivisaoId":"%s","exercicioId":"%s","numeroSerie":1,"repeticoes":10}]
                """.formatted(UUID.randomUUID(), supino.getId(), itemDeB.getId(), supino.getId());

        registrar(sessao(UUID.randomUUID(), treinoA.getId(), "Treino A", agora().minus(Duration.ofHours(1)),
                agora().minus(Duration.ofMinutes(5)), null, series), bearerAna())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.series[0].itemDivisaoId").doesNotExist())
                .andExpect(jsonPath("$.series[0].exercicioId").value(supino.getId().toString()))
                .andExpect(jsonPath("$.series[1].itemDivisaoId").doesNotExist());
    }

    @Test
    void itemComExercicioTrocadoDuranteOTreinoNaoEhLigado() throws Exception {
        String series = """
                [{"itemDivisaoId":"%s","exercicioId":"%s","numeroSerie":1,"repeticoes":10},
                 {"itemDivisaoId":"%s","exercicioId":"%s","numeroSerie":1,"repeticoes":10}]
                """.formatted(itemSupino.getId(), crucifixo.getId(), itemCrucifixo.getId(), crucifixo.getId());

        registrar(sessao(UUID.randomUUID(), treinoA.getId(), "Treino A", agora().minus(Duration.ofHours(1)),
                agora().minus(Duration.ofMinutes(5)), null, series), bearerAna())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.series[0].exercicioId").value(crucifixo.getId().toString()))
                .andExpect(jsonPath("$.series[0].itemDivisaoId").doesNotExist())
                .andExpect(jsonPath("$.series[1].itemDivisaoId").value(itemCrucifixo.getId().toString()));
    }

    @Test
    void divisaoDeFichaEncerradaEhAceita() throws Exception {
        ficha.encerrar();
        fichaRepository.saveAndFlush(ficha);

        registrar(sessaoPadrao(UUID.randomUUID(), treinoA.getId()), bearerAna())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.divisaoId").value(treinoA.getId().toString()))
                .andExpect(jsonPath("$.series[0].itemDivisaoId").value(itemSupino.getId().toString()));
    }

    @Test
    void exercicioDeOutroPersonalRetorna400() throws Exception {
        String series = """
                [{"exercicioId":"%s","numeroSerie":1,"repeticoes":10}]
                """.formatted(deOutroPersonal.getId());

        registrar(sessao(UUID.randomUUID(), treinoA.getId(), "Treino A", agora().minus(Duration.ofHours(1)),
                agora().minus(Duration.ofMinutes(5)), null, series), bearerAna())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['series[0].exercicioId']").value("Exercício inválido."));
    }

    @Test
    void personalNaoRegistraSessao() throws Exception {
        registrar(sessaoPadrao(UUID.randomUUID(), treinoA.getId()),
                "Bearer " + TokenDeTeste.gerar(PERSONAL_ID, "personal@teste.com"))
                .andExpect(status().isForbidden());
    }

    private Aluno gravarAluno(UUID usuarioId, String nome, String email) {
        Usuario usuario = usuarioRepository.save(new Usuario(usuarioId, nome, email, Papel.ALUNO));
        Aluno novo = new Aluno(usuario, PERSONAL_ID, null, null, null, null);
        novo.alterarStatus(StatusAluno.ATIVO);
        return alunoRepository.save(novo);
    }

    private static Divisao divisao(Ficha ficha, int ordem) {
        return ficha.getDivisoes().stream().filter(d -> d.getOrdem() == ordem).findFirst().orElseThrow();
    }

    private static ItemDivisao item(Divisao divisao, int ordem) {
        return divisao.getItens().stream().filter(i -> i.getOrdem() == ordem).findFirst().orElseThrow();
    }

    private static Instant agora() {
        return Instant.now();
    }

    private String seriesPadrao() {
        return """
                [{"itemDivisaoId":"%s","exercicioId":"%s","numeroSerie":1,"repeticoes":10,"cargaKg":20}]
                """.formatted(itemSupino.getId(), supino.getId());
    }

    private String sessaoPadrao(UUID clientId, UUID divisaoId) {
        return sessao(clientId, divisaoId, "Treino A", agora().minus(Duration.ofHours(1)),
                agora().minus(Duration.ofMinutes(5)), null, seriesPadrao());
    }

    private static String sessao(UUID clientId, UUID divisaoId, String divisaoNome, Instant iniciadaEm,
                                 Instant finalizadaEm, String observacaoJson, String seriesJson) {
        return """
                {"clientId":"%s","divisaoId":%s,"divisaoNome":"%s","iniciadaEm":"%s","finalizadaEm":"%s",
                 "observacao":%s,"series":%s}
                """.formatted(clientId, divisaoId == null ? "null" : "\"" + divisaoId + "\"", divisaoNome,
                iniciadaEm, finalizadaEm, observacaoJson == null ? "null" : observacaoJson, seriesJson);
    }

    private ResultActions registrar(String json, String bearer) throws Exception {
        return mvc.perform(post("/api/sessoes").header("Authorization", bearer)
                .contentType(APPLICATION_JSON).content(json));
    }

    private static String bearerAna() {
        return "Bearer " + TokenDeTeste.gerar(USUARIO_ANA_ID, "ana@teste.com");
    }
}
