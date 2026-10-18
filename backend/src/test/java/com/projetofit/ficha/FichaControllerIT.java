package com.projetofit.ficha;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.projetofit.seguranca.TokenDeTeste;
import com.projetofit.usuario.Papel;
import com.projetofit.usuario.Usuario;
import com.projetofit.usuario.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

class FichaControllerIT extends TesteIntegracao {

    private static final UUID PERSONAL_ID = UUID.randomUUID();
    private static final UUID OUTRO_PERSONAL_ID = UUID.randomUUID();
    private static final UUID USUARIO_ALUNO_ID = UUID.randomUUID();

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    AlunoRepository alunoRepository;

    @Autowired
    ExercicioRepository exercicioRepository;

    @Autowired
    FichaRepository fichaRepository;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Clock clock;

    private Aluno aluno;
    private Exercicio supino;
    private Exercicio crucifixo;
    private Exercicio agachamento;
    private Exercicio inativo;
    private Exercicio deOutroPersonal;

    @BeforeEach
    void preparar() {
        fichaRepository.deleteAll();
        exercicioRepository.deleteAll();
        alunoRepository.deleteAll();
        usuarioRepository.deleteAll();
        usuarioRepository.save(new Usuario(PERSONAL_ID, "personal", "personal@teste.com", Papel.PERSONAL));
        usuarioRepository.save(new Usuario(OUTRO_PERSONAL_ID, "outro", "outro@teste.com", Papel.PERSONAL));
        aluno = gravarAluno(USUARIO_ALUNO_ID, "Ana", "ana@teste.com", PERSONAL_ID, StatusAluno.ATIVO);
        supino = gravarExercicio(PERSONAL_ID, "Supino reto", GrupoMuscular.PEITO, true);
        crucifixo = gravarExercicio(PERSONAL_ID, "Crucifixo", GrupoMuscular.PEITO, true);
        agachamento = gravarExercicio(PERSONAL_ID, "Agachamento", GrupoMuscular.QUADRICEPS, true);
        inativo = gravarExercicio(PERSONAL_ID, "Barra antiga", GrupoMuscular.COSTAS, false);
        deOutroPersonal = gravarExercicio(OUTRO_PERSONAL_ID, "Remada", GrupoMuscular.COSTAS, true);
    }

    @Test
    void criarFichaCompleta() throws Exception {
        String json = """
                {"nome":" Hipertrofia ","objetivo":"  ","frequenciaSemanal":3,"dataInicio":"2026-10-01",
                 "dataFim":"2099-12-31","divisoes":[
                   {"nome":" Treino A ","itens":[
                     {"exercicioId":"%s","series":4,"repeticoes":"8-12","cargaKg":22.5,"descansoSeg":90,
                      "observacao":" Controlar a descida "},
                     {"exercicioId":"%s","series":3,"repeticoes":"12"}]},
                   {"nome":"Treino B","itens":[
                     {"exercicioId":"%s","series":5,"repeticoes":"10","observacao":""}]}]}
                """.formatted(supino.getId(), crucifixo.getId(), agachamento.getId());

        criar(aluno.getId(), json)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.alunoId").value(aluno.getId().toString()))
                .andExpect(jsonPath("$.nome").value("Hipertrofia"))
                .andExpect(jsonPath("$.objetivo").doesNotExist())
                .andExpect(jsonPath("$.frequenciaSemanal").value(3))
                .andExpect(jsonPath("$.dataInicio").value("2026-10-01"))
                .andExpect(jsonPath("$.dataFim").value("2099-12-31"))
                .andExpect(jsonPath("$.status").value("ATIVA"))
                .andExpect(jsonPath("$.vencida").value(false))
                .andExpect(jsonPath("$.divisoes.length()").value(2))
                .andExpect(jsonPath("$.divisoes[0].nome").value("Treino A"))
                .andExpect(jsonPath("$.divisoes[0].ordem").value(1))
                .andExpect(jsonPath("$.divisoes[1].ordem").value(2))
                .andExpect(jsonPath("$.divisoes[0].itens.length()").value(2))
                .andExpect(jsonPath("$.divisoes[0].itens[0].ordem").value(1))
                .andExpect(jsonPath("$.divisoes[0].itens[1].ordem").value(2))
                .andExpect(jsonPath("$.divisoes[0].itens[0].exercicioId").value(supino.getId().toString()))
                .andExpect(jsonPath("$.divisoes[0].itens[0].exercicioNome").value("Supino reto"))
                .andExpect(jsonPath("$.divisoes[0].itens[0].grupoMuscular").value("PEITO"))
                .andExpect(jsonPath("$.divisoes[0].itens[0].series").value(4))
                .andExpect(jsonPath("$.divisoes[0].itens[0].repeticoes").value("8-12"))
                .andExpect(jsonPath("$.divisoes[0].itens[0].cargaKg").value(22.5))
                .andExpect(jsonPath("$.divisoes[0].itens[0].descansoSeg").value(90))
                .andExpect(jsonPath("$.divisoes[0].itens[0].observacao").value("Controlar a descida"))
                .andExpect(jsonPath("$.divisoes[0].itens[1].cargaKg").doesNotExist())
                .andExpect(jsonPath("$.divisoes[1].itens.length()").value(1))
                .andExpect(jsonPath("$.divisoes[1].itens[0].exercicioNome").value("Agachamento"))
                .andExpect(jsonPath("$.divisoes[1].itens[0].observacao").doesNotExist());
    }

    @Test
    void criarNovaFichaEncerraAAnterior() throws Exception {
        String antiga = criar(aluno.getId(), fichaSimples("Antiga", "2026-01-01", null, supino.getId()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String idAntiga = JsonPath.read(antiga, "$.id");

        String nova = criar(aluno.getId(), fichaSimples("Nova", "2026-09-01", null, supino.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ATIVA"))
                .andReturn().getResponse().getContentAsString();
        String idNova = JsonPath.read(nova, "$.id");

        mvc.perform(get("/api/alunos/{id}/fichas", aluno.getId()).header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(idNova))
                .andExpect(jsonPath("$[0].nome").value("Nova"))
                .andExpect(jsonPath("$[0].status").value("ATIVA"))
                .andExpect(jsonPath("$[0].quantidadeDivisoes").value(1))
                .andExpect(jsonPath("$[0].frequenciaSemanal").value(3))
                .andExpect(jsonPath("$[0].encerradaEm").doesNotExist())
                .andExpect(jsonPath("$[1].id").value(idAntiga))
                .andExpect(jsonPath("$[1].status").value("ENCERRADA"))
                .andExpect(jsonPath("$[1].encerradaEm").value(LocalDate.now(clock).toString()))
                .andExpect(jsonPath("$[1].vencida").value(false));
        mvc.perform(get("/api/fichas/{id}", idAntiga).header("Authorization", bearerPersonal()))
                .andExpect(jsonPath("$.encerradaEm").value(LocalDate.now(clock).toString()));
    }

    @Test
    void listaComAtivaPrimeiroMesmoComInicioAnteriorAsEncerradas() throws Exception {
        criar(aluno.getId(), fichaSimples("Futura", "2027-01-01", null, supino.getId()))
                .andExpect(status().isCreated());
        criar(aluno.getId(), fichaSimples("Atual", "2026-01-01", null, supino.getId()))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/alunos/{id}/fichas", aluno.getId()).header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Atual"))
                .andExpect(jsonPath("$[0].status").value("ATIVA"))
                .andExpect(jsonPath("$[1].nome").value("Futura"))
                .andExpect(jsonPath("$[1].status").value("ENCERRADA"));
    }

    @Test
    void fichaSemDivisaoRetorna400() throws Exception {
        criar(aluno.getId(), """
                {"nome":"Ficha","frequenciaSemanal":3,"dataInicio":"2026-10-01","divisoes":[]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors.divisoes").exists());
    }

    @Test
    void divisaoSemItemRetorna400() throws Exception {
        criar(aluno.getId(), """
                {"nome":"Ficha","frequenciaSemanal":3,"dataInicio":"2026-10-01",
                 "divisoes":[{"nome":"Treino A","itens":[]}]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['divisoes[0].itens']").exists());
    }

    @Test
    void repeticoesInvalidasRetorna400() throws Exception {
        criar(aluno.getId(), fichaComItem("""
                {"exercicioId":"%s","series":3,"repeticoes":"dez"}""".formatted(supino.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['divisoes[0].itens[0].repeticoes']")
                        .value("Use um número (ex.: 10) ou uma faixa (ex.: 8-12)."));
    }

    @Test
    void exercicioInativoOuDeOutroPersonalRetorna400() throws Exception {
        criar(aluno.getId(), fichaComItem("""
                {"exercicioId":"%s","series":3,"repeticoes":"10"}""".formatted(inativo.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors['divisoes[0].itens[0].exercicioId']")
                        .value("Exercício inexistente ou inativo."));

        String json = """
                {"nome":"Ficha","frequenciaSemanal":3,"dataInicio":"2026-10-01","divisoes":[
                  {"nome":"A","itens":[{"exercicioId":"%s","series":3,"repeticoes":"10"}]},
                  {"nome":"B","itens":[{"exercicioId":"%s","series":3,"repeticoes":"10"},
                                       {"exercicioId":"%s","series":3,"repeticoes":"10"}]}]}
                """.formatted(supino.getId(), crucifixo.getId(), deOutroPersonal.getId());
        criar(aluno.getId(), json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['divisoes[1].itens[1].exercicioId']")
                        .value("Exercício inexistente ou inativo."));

        mvc.perform(get("/api/alunos/{id}/fichas", aluno.getId()).header("Authorization", bearerPersonal()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void dataFimAntesDoInicioRetorna400() throws Exception {
        criar(aluno.getId(), fichaSimples("Ficha", "2026-10-10", "2026-10-09", supino.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors.dataFim")
                        .value("A data de fim deve ser igual ou posterior à data de início."));
    }

    @Test
    void alunoInativoRetorna409() throws Exception {
        aluno.alterarStatus(StatusAluno.INATIVO);
        alunoRepository.save(aluno);

        criar(aluno.getId(), fichaSimples("Ficha", "2026-10-01", null, supino.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Não é possível criar ficha para aluno inativo."));
    }

    @Test
    void fichaVencidaQuandoDataFimPassou() throws Exception {
        String resposta = criar(aluno.getId(), fichaSimples("Velha", "2020-01-01", "2020-02-01", supino.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vencida").value(true))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(resposta, "$.id");

        mvc.perform(get("/api/fichas/{id}", id).header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vencida").value(true));
        mvc.perform(get("/api/alunos/{id}/fichas", aluno.getId()).header("Authorization", bearerPersonal()))
                .andExpect(jsonPath("$[0].vencida").value(true));
    }

    @Test
    void fichaDeAlunoDeOutroPersonalRetorna404() throws Exception {
        Aluno alunoDeOutro = gravarAluno(UUID.randomUUID(), "Bia", "bia@teste.com", OUTRO_PERSONAL_ID,
                StatusAluno.ATIVO);
        Ficha fichaDeOutro = new Ficha(alunoDeOutro, "Dele", null, 3, LocalDate.of(2026, 1, 1), null);
        fichaDeOutro.adicionarDivisao("A", 1).adicionarItem(deOutroPersonal, 1, 3, "10", null, null, null);
        fichaRepository.save(fichaDeOutro);

        mvc.perform(get("/api/fichas/{id}", fichaDeOutro.getId()).header("Authorization", bearerPersonal()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Ficha não encontrada."));
        mvc.perform(get("/api/alunos/{id}/fichas", alunoDeOutro.getId()).header("Authorization", bearerPersonal()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Aluno não encontrado."));
        criar(alunoDeOutro.getId(), fichaSimples("Invasora", "2026-10-01", null, supino.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Aluno não encontrado."));
    }

    @Test
    void alunoNaoAcessaFichasPelaApiDoPersonal() throws Exception {
        mvc.perform(get("/api/alunos/{id}/fichas", aluno.getId())
                        .header("Authorization", "Bearer " + TokenDeTeste.gerar(USUARIO_ALUNO_ID, "ana@teste.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void elementoNuloNaListaRetorna400() throws Exception {
        criar(aluno.getId(), """
                {"nome":"Ficha","frequenciaSemanal":3,"dataInicio":"2026-10-01","divisoes":[null]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['divisoes[0]']").exists());
        criar(aluno.getId(), fichaComItem("null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['divisoes[0].itens[0]']").exists());
    }

    @Test
    void editarAtualizaCriaERemoveDivisoesEItens() throws Exception {
        String criada = criar(aluno.getId(), """
                {"nome":"Ficha","frequenciaSemanal":3,"dataInicio":"2026-10-01","divisoes":[
                  {"nome":"A","itens":[{"exercicioId":"%s","series":3,"repeticoes":"10"},
                                       {"exercicioId":"%s","series":3,"repeticoes":"12"}]},
                  {"nome":"B","itens":[{"exercicioId":"%s","series":4,"repeticoes":"8"}]}]}
                """.formatted(supino.getId(), crucifixo.getId(), agachamento.getId()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String fichaId = JsonPath.read(criada, "$.id");
        String divA = JsonPath.read(criada, "$.divisoes[0].id");
        String a1 = JsonPath.read(criada, "$.divisoes[0].itens[0].id");
        String a2 = JsonPath.read(criada, "$.divisoes[0].itens[1].id");
        String divB = JsonPath.read(criada, "$.divisoes[1].id");
        String b1 = JsonPath.read(criada, "$.divisoes[1].itens[0].id");

        String json = """
                {"nome":" Ficha nova ","objetivo":"Força","frequenciaSemanal":4,"dataInicio":"2026-10-05",
                 "dataFim":"2099-01-31","divisoes":[
                  {"id":"%s","nome":" A1 ","itens":[
                    {"id":"%s","exercicioId":"%s","series":5,"repeticoes":"6-8","cargaKg":40,"descansoSeg":120,
                     "observacao":" Pesado "},
                    {"exercicioId":"%s","series":2,"repeticoes":"15"}]},
                  {"nome":"C","itens":[{"exercicioId":"%s","series":3,"repeticoes":"10"}]}]}
                """.formatted(divA, a2, agachamento.getId(), supino.getId(), crucifixo.getId());

        editar(fichaId, json)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fichaId))
                .andExpect(jsonPath("$.nome").value("Ficha nova"))
                .andExpect(jsonPath("$.objetivo").value("Força"))
                .andExpect(jsonPath("$.frequenciaSemanal").value(4))
                .andExpect(jsonPath("$.dataInicio").value("2026-10-05"))
                .andExpect(jsonPath("$.dataFim").value("2099-01-31"))
                .andExpect(jsonPath("$.status").value("ATIVA"))
                .andExpect(jsonPath("$.divisoes.length()").value(2))
                .andExpect(jsonPath("$.divisoes[0].id").value(divA))
                .andExpect(jsonPath("$.divisoes[0].nome").value("A1"))
                .andExpect(jsonPath("$.divisoes[0].ordem").value(1))
                .andExpect(jsonPath("$.divisoes[0].itens.length()").value(2))
                .andExpect(jsonPath("$.divisoes[0].itens[0].id").value(a2))
                .andExpect(jsonPath("$.divisoes[0].itens[0].ordem").value(1))
                .andExpect(jsonPath("$.divisoes[0].itens[0].exercicioId").value(agachamento.getId().toString()))
                .andExpect(jsonPath("$.divisoes[0].itens[0].exercicioNome").value("Agachamento"))
                .andExpect(jsonPath("$.divisoes[0].itens[0].series").value(5))
                .andExpect(jsonPath("$.divisoes[0].itens[0].repeticoes").value("6-8"))
                .andExpect(jsonPath("$.divisoes[0].itens[0].cargaKg").value(40))
                .andExpect(jsonPath("$.divisoes[0].itens[0].descansoSeg").value(120))
                .andExpect(jsonPath("$.divisoes[0].itens[0].observacao").value("Pesado"))
                .andExpect(jsonPath("$.divisoes[0].itens[1].id").value(not(a1)))
                .andExpect(jsonPath("$.divisoes[0].itens[1].ordem").value(2))
                .andExpect(jsonPath("$.divisoes[0].itens[1].exercicioNome").value("Supino reto"))
                .andExpect(jsonPath("$.divisoes[1].id").value(not(divB)))
                .andExpect(jsonPath("$.divisoes[1].nome").value("C"))
                .andExpect(jsonPath("$.divisoes[1].ordem").value(2))
                .andExpect(jsonPath("$.divisoes[1].itens[0].exercicioNome").value("Crucifixo"));

        assertThat(contar("divisao", divB)).isZero();
        assertThat(contar("item_divisao", b1)).isZero();
        assertThat(contar("item_divisao", a1)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from divisao where ficha_id = ?::uuid", Integer.class,
                fichaId)).isEqualTo(2);
        mvc.perform(get("/api/fichas/{id}", fichaId).header("Authorization", bearerPersonal()))
                .andExpect(jsonPath("$.divisoes.length()").value(2))
                .andExpect(jsonPath("$.divisoes[0].itens.length()").value(2))
                .andExpect(jsonPath("$.divisoes[0].itens[0].id").value(a2))
                .andExpect(jsonPath("$.divisoes[1].nome").value("C"));
    }

    @Test
    void editarReordenaDivisoesEItens() throws Exception {
        String criada = criar(aluno.getId(), """
                {"nome":"Ficha","frequenciaSemanal":3,"dataInicio":"2026-10-01","divisoes":[
                  {"nome":"A","itens":[{"exercicioId":"%s","series":3,"repeticoes":"10"},
                                       {"exercicioId":"%s","series":3,"repeticoes":"12"}]},
                  {"nome":"B","itens":[{"exercicioId":"%s","series":4,"repeticoes":"8"}]}]}
                """.formatted(supino.getId(), crucifixo.getId(), agachamento.getId()))
                .andReturn().getResponse().getContentAsString();
        String fichaId = JsonPath.read(criada, "$.id");
        String divA = JsonPath.read(criada, "$.divisoes[0].id");
        String a1 = JsonPath.read(criada, "$.divisoes[0].itens[0].id");
        String a2 = JsonPath.read(criada, "$.divisoes[0].itens[1].id");
        String divB = JsonPath.read(criada, "$.divisoes[1].id");
        String b1 = JsonPath.read(criada, "$.divisoes[1].itens[0].id");

        editar(fichaId, """
                {"nome":"Ficha","frequenciaSemanal":3,"dataInicio":"2026-10-01","divisoes":[
                  {"id":"%s","nome":"B","itens":[{"id":"%s","exercicioId":"%s","series":4,"repeticoes":"8"}]},
                  {"id":"%s","nome":"A","itens":[{"id":"%s","exercicioId":"%s","series":3,"repeticoes":"12"},
                                                 {"id":"%s","exercicioId":"%s","series":3,"repeticoes":"10"}]}]}
                """.formatted(divB, b1, agachamento.getId(), divA, a2, crucifixo.getId(), a1, supino.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.divisoes[0].id").value(divB))
                .andExpect(jsonPath("$.divisoes[0].ordem").value(1))
                .andExpect(jsonPath("$.divisoes[1].id").value(divA))
                .andExpect(jsonPath("$.divisoes[1].ordem").value(2))
                .andExpect(jsonPath("$.divisoes[1].itens[0].id").value(a2))
                .andExpect(jsonPath("$.divisoes[1].itens[0].ordem").value(1))
                .andExpect(jsonPath("$.divisoes[1].itens[1].id").value(a1))
                .andExpect(jsonPath("$.divisoes[1].itens[1].ordem").value(2));
    }

    @Test
    void editarComDataFimAntesDoInicioRetorna400() throws Exception {
        String fichaId = criarFichaSimples("Ficha");

        editar(fichaId, fichaSimples("Ficha", "2026-10-10", "2026-10-09", supino.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.dataFim")
                        .value("A data de fim deve ser igual ou posterior à data de início."));
    }

    @Test
    void editarMantendoExercicioQueFicouInativoFunciona() throws Exception {
        String criada = criar(aluno.getId(), fichaSimples("Ficha", "2026-10-01", null, supino.getId()))
                .andReturn().getResponse().getContentAsString();
        String fichaId = JsonPath.read(criada, "$.id");
        String divA = JsonPath.read(criada, "$.divisoes[0].id");
        String a1 = JsonPath.read(criada, "$.divisoes[0].itens[0].id");
        supino.atualizar(supino.getNome(), supino.getGrupoMuscular(), null, null, false);
        exercicioRepository.save(supino);

        editar(fichaId, fichaComItens(divA, """
                {"id":"%s","exercicioId":"%s","series":5,"repeticoes":"10"}""".formatted(a1, supino.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.divisoes[0].itens[0].id").value(a1))
                .andExpect(jsonPath("$.divisoes[0].itens[0].series").value(5));

        editar(fichaId, fichaComItens(divA, """
                {"id":"%s","exercicioId":"%s","series":5,"repeticoes":"10"}""".formatted(a1, inativo.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['divisoes[0].itens[0].exercicioId']")
                        .value("Exercício inexistente ou inativo."));

        editar(fichaId, fichaComItens(divA, """
                {"id":"%s","exercicioId":"%s","series":5,"repeticoes":"10"},
                {"exercicioId":"%s","series":3,"repeticoes":"10"}""".formatted(a1, supino.getId(), supino.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['divisoes[0].itens[1].exercicioId']")
                        .value("Exercício inexistente ou inativo."));
    }

    @Test
    void idDeDivisaoDeOutraFichaRetorna400() throws Exception {
        String fichaId = criarFichaSimples("Da Ana");
        Aluno bia = gravarAluno(UUID.randomUUID(), "Bia", "bia@teste.com", PERSONAL_ID, StatusAluno.ATIVO);
        String outra = criar(bia.getId(), fichaSimples("Da Bia", "2026-10-01", null, crucifixo.getId()))
                .andReturn().getResponse().getContentAsString();
        String outraId = JsonPath.read(outra, "$.id");
        String divisaoDaOutra = JsonPath.read(outra, "$.divisoes[0].id");

        editar(fichaId, fichaComItens(divisaoDaOutra, """
                {"exercicioId":"%s","series":3,"repeticoes":"10"}""".formatted(supino.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors['divisoes[0].id']").value("Divisão não pertence a esta ficha."));

        mvc.perform(get("/api/fichas/{id}", outraId).header("Authorization", bearerPersonal()))
                .andExpect(jsonPath("$.divisoes.length()").value(1))
                .andExpect(jsonPath("$.divisoes[0].id").value(divisaoDaOutra))
                .andExpect(jsonPath("$.divisoes[0].nome").value("Treino A"))
                .andExpect(jsonPath("$.divisoes[0].itens.length()").value(1))
                .andExpect(jsonPath("$.divisoes[0].itens[0].exercicioNome").value("Crucifixo"));
    }

    @Test
    void idDeItemDeOutraDivisaoRetorna400() throws Exception {
        String criada = criar(aluno.getId(), """
                {"nome":"Ficha","frequenciaSemanal":3,"dataInicio":"2026-10-01","divisoes":[
                  {"nome":"A","itens":[{"exercicioId":"%s","series":3,"repeticoes":"10"}]},
                  {"nome":"B","itens":[{"exercicioId":"%s","series":4,"repeticoes":"8"}]}]}
                """.formatted(supino.getId(), agachamento.getId()))
                .andReturn().getResponse().getContentAsString();
        String fichaId = JsonPath.read(criada, "$.id");
        String divA = JsonPath.read(criada, "$.divisoes[0].id");
        String b1 = JsonPath.read(criada, "$.divisoes[1].itens[0].id");

        editar(fichaId, fichaComItens(divA, """
                {"id":"%s","exercicioId":"%s","series":4,"repeticoes":"8"}""".formatted(b1, agachamento.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['divisoes[0].itens[0].id']").value("Item não pertence a esta divisão."));
    }

    @Test
    void editarFichaEncerradaRetorna409() throws Exception {
        String antiga = criarFichaSimples("Antiga");
        criarFichaSimples("Nova");

        editar(antiga, fichaSimples("Antiga editada", "2026-10-01", null, supino.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Só é possível editar a ficha ativa."));
    }

    @Test
    void encerrarFicha() throws Exception {
        String fichaId = criarFichaSimples("Ficha");

        encerrar(fichaId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fichaId))
                .andExpect(jsonPath("$.status").value("ENCERRADA"))
                .andExpect(jsonPath("$.divisoes.length()").value(1));
        encerrar(fichaId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A ficha já está encerrada."));
        mvc.perform(get("/api/fichas/{id}", fichaId).header("Authorization", bearerPersonal()))
                .andExpect(jsonPath("$.status").value("ENCERRADA"));
    }

    @Test
    void encerrarPreencheEncerradaEm() throws Exception {
        String fichaId = criarFichaSimples("Ficha");
        mvc.perform(get("/api/fichas/{id}", fichaId).header("Authorization", bearerPersonal()))
                .andExpect(jsonPath("$.encerradaEm").doesNotExist());

        encerrar(fichaId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encerradaEm").value(LocalDate.now(clock).toString()));
        assertThat(jdbc.queryForObject("select encerrada_em is not null from ficha where id = ?::uuid",
                Boolean.class, fichaId)).isTrue();
    }

    @Test
    void encerrarFichaDeOutroPersonalRetorna404() throws Exception {
        Aluno alunoDeOutro = gravarAluno(UUID.randomUUID(), "Bia", "bia@teste.com", OUTRO_PERSONAL_ID,
                StatusAluno.ATIVO);
        Ficha fichaDeOutro = new Ficha(alunoDeOutro, "Dele", null, 3, LocalDate.of(2026, 1, 1), null);
        fichaDeOutro.adicionarDivisao("A", 1).adicionarItem(deOutroPersonal, 1, 3, "10", null, null, null);
        fichaRepository.save(fichaDeOutro);
        String id = fichaDeOutro.getId().toString();

        encerrar(id)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Ficha não encontrada."));
        editar(id, fichaSimples("Invasora", "2026-10-01", null, supino.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Ficha não encontrada."));
        assertThat(jdbc.queryForObject("select status from ficha where id = ?::uuid", String.class, id))
                .isEqualTo("ATIVA");
    }

    private Aluno gravarAluno(UUID usuarioId, String nome, String email, UUID personalId, StatusAluno status) {
        Usuario usuario = usuarioRepository.save(new Usuario(usuarioId, nome, email, Papel.ALUNO));
        Aluno novo = new Aluno(usuario, personalId, null, null, null, null);
        novo.alterarStatus(status);
        return alunoRepository.save(novo);
    }

    private Exercicio gravarExercicio(UUID personalId, String nome, GrupoMuscular grupo, boolean ativo) {
        Exercicio e = new Exercicio(personalId, nome, grupo, null, null);
        if (!ativo) {
            e.atualizar(nome, grupo, null, null, false);
        }
        return exercicioRepository.save(e);
    }

    private static String fichaSimples(String nome, String dataInicio, String dataFim, UUID exercicioId) {
        String fim = dataFim == null ? "null" : "\"" + dataFim + "\"";
        return """
                {"nome":"%s","frequenciaSemanal":3,"dataInicio":"%s","dataFim":%s,
                 "divisoes":[{"nome":"Treino A","itens":[{"exercicioId":"%s","series":3,"repeticoes":"10"}]}]}
                """.formatted(nome, dataInicio, fim, exercicioId);
    }

    private static String fichaComItem(String item) {
        return """
                {"nome":"Ficha","frequenciaSemanal":3,"dataInicio":"2026-10-01",
                 "divisoes":[{"nome":"Treino A","itens":[%s]}]}
                """.formatted(item);
    }

    private ResultActions criar(UUID alunoId, String json) throws Exception {
        return mvc.perform(post("/api/alunos/{id}/fichas", alunoId).header("Authorization", bearerPersonal())
                .contentType(APPLICATION_JSON).content(json));
    }

    private String criarFichaSimples(String nome) throws Exception {
        String resposta = criar(aluno.getId(), fichaSimples(nome, "2026-10-01", null, supino.getId()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private static String fichaComItens(String divisaoId, String itens) {
        return """
                {"nome":"Ficha","frequenciaSemanal":3,"dataInicio":"2026-10-01",
                 "divisoes":[{"id":"%s","nome":"Treino A","itens":[%s]}]}
                """.formatted(divisaoId, itens);
    }

    private int contar(String tabela, String id) {
        return jdbc.queryForObject("select count(*) from " + tabela + " where id = ?::uuid", Integer.class, id);
    }

    private ResultActions editar(String fichaId, String json) throws Exception {
        return mvc.perform(put("/api/fichas/{id}", fichaId).header("Authorization", bearerPersonal())
                .contentType(APPLICATION_JSON).content(json));
    }

    private ResultActions encerrar(String fichaId) throws Exception {
        return mvc.perform(post("/api/fichas/{id}/encerrar", fichaId).header("Authorization", bearerPersonal()));
    }

    private static String bearerPersonal() {
        return "Bearer " + TokenDeTeste.gerar(PERSONAL_ID, "personal@teste.com");
    }
}
