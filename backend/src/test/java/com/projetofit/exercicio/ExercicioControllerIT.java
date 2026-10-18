package com.projetofit.exercicio;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.projetofit.TesteIntegracao;
import com.projetofit.aluno.Aluno;
import com.projetofit.aluno.AlunoRepository;
import com.projetofit.aluno.StatusAluno;
import com.projetofit.seguranca.TokenDeTeste;
import com.projetofit.usuario.Papel;
import com.projetofit.usuario.Usuario;
import com.projetofit.usuario.UsuarioRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

class ExercicioControllerIT extends TesteIntegracao {

    private static final UUID PERSONAL_ID = UUID.randomUUID();
    private static final UUID OUTRO_PERSONAL_ID = UUID.randomUUID();

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    AlunoRepository alunoRepository;

    @Autowired
    ExercicioRepository exercicioRepository;

    @BeforeEach
    void preparar() {
        exercicioRepository.deleteAll();
        alunoRepository.deleteAll();
        usuarioRepository.deleteAll();
        usuarioRepository.save(new Usuario(PERSONAL_ID, "personal", "personal@teste.com", Papel.PERSONAL));
        usuarioRepository.save(new Usuario(OUTRO_PERSONAL_ID, "outro", "outro@teste.com", Papel.PERSONAL));
    }

    @Test
    void criarExercicio() throws Exception {
        criar("{\"nome\":\" Supino reto \",\"grupoMuscular\":\"PEITO\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nome").value("Supino reto"))
                .andExpect(jsonPath("$.grupoMuscular").value("PEITO"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.videoUrl").doesNotExist());
    }

    @Test
    void nomeDuplicadoSemDiferenciarMaiusculasRetorna409() throws Exception {
        criar("{\"nome\":\"Supino reto\",\"grupoMuscular\":\"PEITO\"}").andExpect(status().isCreated());
        criar("{\"nome\":\"SUPINO RETO\",\"grupoMuscular\":\"PEITO\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe um exercício com este nome."));
    }

    @Test
    void renomearParaNomeExistenteRetorna409() throws Exception {
        Exercicio a = gravar(PERSONAL_ID, "Supino reto", GrupoMuscular.PEITO, true);
        Exercicio b = gravar(PERSONAL_ID, "Remada", GrupoMuscular.COSTAS, true);

        atualizar(b.getId(), "{\"nome\":\"supino reto\",\"grupoMuscular\":\"COSTAS\",\"ativo\":true}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe um exercício com este nome."));
        atualizar(a.getId(), "{\"nome\":\"SUPINO RETO\",\"grupoMuscular\":\"PEITO\",\"ativo\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("SUPINO RETO"));
    }

    @Test
    void linkComEsquemaMaiusculoEhAceito() throws Exception {
        criar("{\"nome\":\"Supino\",\"grupoMuscular\":\"PEITO\",\"videoUrl\":\"HTTPS://youtu.be/abc\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.videoUrl").value("HTTPS://youtu.be/abc"));
    }

    @Test
    void linkDeVideoSemHttpRetorna400() throws Exception {
        criar("{\"nome\":\"Supino\",\"grupoMuscular\":\"PEITO\",\"videoUrl\":\"javascript:alert(1)\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.videoUrl").value("Informe um link começando com http:// ou https://."));
    }

    @Test
    void grupoInvalidoRetorna400() throws Exception {
        criar("{\"nome\":\"Supino\",\"grupoMuscular\":\"PESCOCO\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"));
    }

    @Test
    void listarFiltraPorAtivoEGrupoOrdenadoPorNome() throws Exception {
        gravar(PERSONAL_ID, "Supino", GrupoMuscular.PEITO, true);
        gravar(PERSONAL_ID, "Crucifixo", GrupoMuscular.PEITO, true);
        gravar(PERSONAL_ID, "Remada", GrupoMuscular.COSTAS, true);
        gravar(PERSONAL_ID, "Barra antiga", GrupoMuscular.COSTAS, false);

        mvc.perform(get("/api/exercicios").header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].nome").value("Crucifixo"))
                .andExpect(jsonPath("$[1].nome").value("Remada"))
                .andExpect(jsonPath("$[2].nome").value("Supino"));
        mvc.perform(get("/api/exercicios").param("grupo", "COSTAS").header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Remada"));
        mvc.perform(get("/api/exercicios").param("ativo", "false").header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Barra antiga"))
                .andExpect(jsonPath("$[0].ativo").value(false));
    }

    @Test
    void desativarEReativarPeloPut() throws Exception {
        Exercicio e = gravar(PERSONAL_ID, "Supino", GrupoMuscular.PEITO, true);

        atualizar(e.getId(), "{\"nome\":\"Supino\",\"grupoMuscular\":\"PEITO\",\"ativo\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
        mvc.perform(get("/api/exercicios").header("Authorization", bearerPersonal()))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/exercicios").param("ativo", "false").header("Authorization", bearerPersonal()))
                .andExpect(jsonPath("$.length()").value(1));

        atualizar(e.getId(), "{\"nome\":\"Supino\",\"grupoMuscular\":\"PEITO\",\"ativo\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));
        mvc.perform(get("/api/exercicios").header("Authorization", bearerPersonal()))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void exercicioDeOutroPersonalRetorna404() throws Exception {
        Exercicio e = gravar(OUTRO_PERSONAL_ID, "Supino", GrupoMuscular.PEITO, true);

        mvc.perform(get("/api/exercicios/{id}", e.getId()).header("Authorization", bearerPersonal()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Exercício não encontrado."));
        atualizar(e.getId(), "{\"nome\":\"Invasor\",\"grupoMuscular\":\"PEITO\",\"ativo\":true}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Exercício não encontrado."));
        mvc.perform(get("/api/exercicios").header("Authorization", bearerPersonal()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void alunoNaoAcessaCatalogo() throws Exception {
        UUID alunoId = UUID.randomUUID();
        Usuario usuarioAluno = usuarioRepository.save(new Usuario(alunoId, "Ana", "ana@teste.com", Papel.ALUNO));
        Aluno aluno = new Aluno(usuarioAluno, PERSONAL_ID, null, null, null, null);
        aluno.alterarStatus(StatusAluno.ATIVO);
        alunoRepository.save(aluno);

        mvc.perform(get("/api/exercicios")
                        .header("Authorization", "Bearer " + TokenDeTeste.gerar(alunoId, "ana@teste.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void atualizarSemAtivoRetorna400() throws Exception {
        Exercicio e = gravar(PERSONAL_ID, "Supino", GrupoMuscular.PEITO, true);

        atualizar(e.getId(), "{\"nome\":\"Supino\",\"grupoMuscular\":\"PEITO\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.ativo").exists());
    }

    private Exercicio gravar(UUID personalId, String nome, GrupoMuscular grupo, boolean ativo) {
        Exercicio e = new Exercicio(personalId, nome, grupo, null, null);
        if (!ativo) {
            e.atualizar(nome, grupo, null, null, false);
        }
        return exercicioRepository.save(e);
    }

    private ResultActions criar(String json) throws Exception {
        return mvc.perform(post("/api/exercicios").header("Authorization", bearerPersonal())
                .contentType(APPLICATION_JSON).content(json));
    }

    private ResultActions atualizar(UUID id, String json) throws Exception {
        return mvc.perform(put("/api/exercicios/{id}", id).header("Authorization", bearerPersonal())
                .contentType(APPLICATION_JSON).content(json));
    }

    private static String bearerPersonal() {
        return "Bearer " + TokenDeTeste.gerar(PERSONAL_ID, "personal@teste.com");
    }
}
