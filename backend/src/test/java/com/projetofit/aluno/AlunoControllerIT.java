package com.projetofit.aluno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.projetofit.TesteIntegracao;
import com.projetofit.comum.erro.IntegracaoExternaException;
import com.projetofit.comum.supabase.SupabaseAdmin;
import com.projetofit.seguranca.TokenDeTeste;
import com.projetofit.usuario.Papel;
import com.projetofit.usuario.Usuario;
import com.projetofit.usuario.UsuarioRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

class AlunoControllerIT extends TesteIntegracao {

    private static final UUID PERSONAL_ID = UUID.randomUUID();

    @MockitoBean
    SupabaseAdmin supabaseAdmin;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    AlunoRepository alunoRepository;

    @BeforeEach
    void preparar() {
        alunoRepository.deleteAll();
        usuarioRepository.deleteAll();
        usuarioRepository.save(new Usuario(PERSONAL_ID, "personal", "personal@teste.com", Papel.PERSONAL));
        when(supabaseAdmin.convidar(any(), any())).thenAnswer(i -> UUID.randomUUID());
    }

    @Test
    void personalCriaAlunoConvidado() throws Exception {
        mvc.perform(post("/api/alunos").header("Authorization", bearerPersonal())
                        .contentType(APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@teste.com\",\"dataNascimento\":\"2000-05-10\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.status").value("CONVIDADO"))
                .andExpect(jsonPath("$.dataNascimento").value("2000-05-10"));
        verify(supabaseAdmin).convidar("ana@teste.com", "Ana");
    }

    @Test
    void criarNormalizaEmailEDetectaDuplicadoSemChamarSupabase() throws Exception {
        criar("Ana", " Ana@Teste.com ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@teste.com"));
        criar("Ana", "ANA@teste.com")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("E-mail já cadastrado."));
        verify(supabaseAdmin, times(1)).convidar(any(), any());
    }

    @Test
    void criarSemNomeRetorna400ComErrors() throws Exception {
        mvc.perform(post("/api/alunos").header("Authorization", bearerPersonal())
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"x@teste.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nome").exists());
        verify(supabaseAdmin, never()).convidar(any(), any());
    }

    @Test
    void conviteRecusadoPeloSupabaseNaoGravaNada() throws Exception {
        when(supabaseAdmin.convidar(any(), any()))
                .thenThrow(new IntegracaoExternaException("Não foi possível enviar o convite.", null));
        criar("Ana", "ana@teste.com").andExpect(status().isBadGateway());
        assertThat(alunoRepository.count()).isZero();
    }

    @Test
    void listarTrazConvidadosEAtivosOrdenadosPorNome() throws Exception {
        criar("Bruno", "bruno@teste.com").andExpect(status().isCreated());
        criar("Ana", "ana@teste.com").andExpect(status().isCreated());
        criar("Carla", "carla@teste.com").andExpect(status().isCreated());
        UUID carlaId = usuarioRepository.findByEmailIgnoreCase("carla@teste.com").orElseThrow().getId();
        Aluno carla = alunoRepository.findByUsuarioId(carlaId).orElseThrow();
        carla.alterarStatus(StatusAluno.INATIVO);
        alunoRepository.save(carla);

        mvc.perform(get("/api/alunos").header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Ana"))
                .andExpect(jsonPath("$[1].nome").value("Bruno"));
        mvc.perform(get("/api/alunos").param("status", "INATIVO").header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Carla"))
                .andExpect(jsonPath("$[0].status").value("INATIVO"));
    }

    @Test
    void statusInvalidoRetorna400() throws Exception {
        mvc.perform(get("/api/alunos").param("status", "XYZ").header("Authorization", bearerPersonal()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Parâmetro 'status' inválido."));
    }

    @Test
    void buscarAlunoInexistenteRetorna404() throws Exception {
        mvc.perform(get("/api/alunos/{id}", UUID.randomUUID()).header("Authorization", bearerPersonal()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Aluno não encontrado."));
    }

    @Test
    void alunoNaoAcessaEndpointsDoPersonal() throws Exception {
        UUID alunoId = UUID.randomUUID();
        Usuario usuarioAluno = usuarioRepository.save(new Usuario(alunoId, "Ana", "ana@teste.com", Papel.ALUNO));
        Aluno aluno = new Aluno(usuarioAluno, PERSONAL_ID, null, null, null, null);
        aluno.alterarStatus(StatusAluno.ATIVO);
        alunoRepository.save(aluno);

        mvc.perform(get("/api/alunos")
                        .header("Authorization", "Bearer " + TokenDeTeste.gerar(alunoId, "ana@teste.com")))
                .andExpect(status().isForbidden());
    }

    private ResultActions criar(String nome, String email) throws Exception {
        return mvc.perform(post("/api/alunos").header("Authorization", bearerPersonal())
                .contentType(APPLICATION_JSON)
                .content("{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\"}"));
    }

    private static String bearerPersonal() {
        return "Bearer " + TokenDeTeste.gerar(PERSONAL_ID, "personal@teste.com");
    }
}
