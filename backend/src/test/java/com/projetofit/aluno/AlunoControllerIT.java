package com.projetofit.aluno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.projetofit.TesteIntegracao;
import com.projetofit.comum.erro.ConflitoException;
import com.projetofit.comum.erro.IntegracaoExternaException;
import com.projetofit.comum.supabase.SupabaseAdmin;
import com.projetofit.seguranca.TokenDeTeste;
import com.projetofit.exercicio.ExercicioRepository;
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

    @Autowired
    ExercicioRepository exercicioRepository;

    @BeforeEach
    void preparar() {
        exercicioRepository.deleteAll();
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

    @Test
    void alunoDeOutroPersonalFicaOculto() throws Exception {
        Aluno aluno = gravarAluno(UUID.randomUUID(), "outra@teste.com", StatusAluno.ATIVO, gravarOutroPersonal());

        mvc.perform(get("/api/alunos/{id}", aluno.getId()).header("Authorization", bearerPersonal()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Aluno não encontrado."));
        mvc.perform(get("/api/alunos").header("Authorization", bearerPersonal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void atualizarAlteraNomeEDadosMasNaoEmail() throws Exception {
        Aluno aluno = gravarAluno(UUID.randomUUID(), "ana@teste.com", StatusAluno.ATIVO, PERSONAL_ID);

        mvc.perform(put("/api/alunos/{id}", aluno.getId()).header("Authorization", bearerPersonal())
                        .contentType(APPLICATION_JSON)
                        .content("{\"nome\":\"Ana Souza\",\"telefone\":\"11999990000\",\"email\":\"outro@teste.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.telefone").value("11999990000"))
                .andExpect(jsonPath("$.email").value("ana@teste.com"));
        Usuario usuario = usuarioRepository.findById(aluno.getUsuario().getId()).orElseThrow();
        assertThat(usuario.getNome()).isEqualTo("Ana Souza");
        assertThat(usuario.getEmail()).isEqualTo("ana@teste.com");
        assertThat(alunoRepository.findById(aluno.getId()).orElseThrow().getTelefone()).isEqualTo("11999990000");
    }

    @Test
    void desativarEReativar() throws Exception {
        Usuario usuario = usuarioRepository.save(new Usuario(UUID.randomUUID(), "Ana", "ana@teste.com", Papel.ALUNO));
        Aluno aluno = new Aluno(usuario, PERSONAL_ID, null, null, null, null);
        aluno.registrarPrimeiroAcesso();
        aluno = alunoRepository.save(aluno);

        alterarStatus(aluno.getId(), "INATIVO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INATIVO"));
        alterarStatus(aluno.getId(), "ATIVO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"));
        assertThat(alunoRepository.findById(aluno.getId()).orElseThrow().getStatus()).isEqualTo(StatusAluno.ATIVO);
    }

    @Test
    void reativarQuemNuncaEntrouVoltaParaConvidado() throws Exception {
        Aluno aluno = gravarAluno(UUID.randomUUID(), "ana@teste.com", StatusAluno.CONVIDADO, PERSONAL_ID);
        when(supabaseAdmin.convidar("ana@teste.com", "Ana")).thenReturn(aluno.getUsuario().getId());

        alterarStatus(aluno.getId(), "INATIVO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INATIVO"));
        alterarStatus(aluno.getId(), "ATIVO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONVIDADO"));
        mvc.perform(post("/api/alunos/{id}/reenviar-convite", aluno.getId()).header("Authorization", bearerPersonal()))
                .andExpect(status().isNoContent());
        verify(supabaseAdmin).convidar("ana@teste.com", "Ana");
    }

    @Test
    void ativarConvidadoRetorna409() throws Exception {
        Aluno aluno = gravarAluno(UUID.randomUUID(), "ana@teste.com", StatusAluno.CONVIDADO, PERSONAL_ID);

        alterarStatus(aluno.getId(), "ATIVO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("O aluno ainda não aceitou o convite."));
    }

    @Test
    void statusConvidadoNoPatchRetorna409() throws Exception {
        Aluno aluno = gravarAluno(UUID.randomUUID(), "ana@teste.com", StatusAluno.INATIVO, PERSONAL_ID);

        alterarStatus(aluno.getId(), "CONVIDADO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Status inválido."));
    }

    @Test
    void reenviarConviteSoParaConvidado() throws Exception {
        Aluno convidado = gravarAluno(UUID.randomUUID(), "ana@teste.com", StatusAluno.CONVIDADO, PERSONAL_ID);
        Aluno ativo = gravarAluno(UUID.randomUUID(), "bruno@teste.com", StatusAluno.ATIVO, PERSONAL_ID);
        when(supabaseAdmin.convidar("ana@teste.com", "Ana")).thenReturn(convidado.getUsuario().getId());

        mvc.perform(post("/api/alunos/{id}/reenviar-convite", convidado.getId()).header("Authorization", bearerPersonal()))
                .andExpect(status().isNoContent());
        verify(supabaseAdmin).convidar("ana@teste.com", "Ana");

        mvc.perform(post("/api/alunos/{id}/reenviar-convite", ativo.getId()).header("Authorization", bearerPersonal()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Só é possível reenviar o convite de um aluno com convite pendente."));
        verify(supabaseAdmin, never()).convidar(eq("bruno@teste.com"), any());
    }

    @Test
    void reenviarParaQuemJaConfirmouOrientaEsqueciSenha() throws Exception {
        Aluno aluno = gravarAluno(UUID.randomUUID(), "ana@teste.com", StatusAluno.CONVIDADO, PERSONAL_ID);
        when(supabaseAdmin.convidar(any(), any())).thenThrow(new ConflitoException("E-mail já cadastrado."));

        mvc.perform(post("/api/alunos/{id}/reenviar-convite", aluno.getId()).header("Authorization", bearerPersonal()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "O aluno já confirmou o e-mail. Peça para ele usar \"Esqueci minha senha\" na tela de login."));
    }

    @Test
    void editarAlunoDeOutroPersonalRetorna404() throws Exception {
        Aluno aluno = gravarAluno(UUID.randomUUID(), "outra@teste.com", StatusAluno.ATIVO, gravarOutroPersonal());

        mvc.perform(put("/api/alunos/{id}", aluno.getId()).header("Authorization", bearerPersonal())
                        .contentType(APPLICATION_JSON)
                        .content("{\"nome\":\"Invasor\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Aluno não encontrado."));
        assertThat(usuarioRepository.findById(aluno.getUsuario().getId()).orElseThrow().getNome()).isEqualTo("Ana");
    }

    private ResultActions alterarStatus(UUID id, String status) throws Exception {
        return mvc.perform(patch("/api/alunos/{id}/status", id).header("Authorization", bearerPersonal())
                .contentType(APPLICATION_JSON)
                .content("{\"status\":\"" + status + "\"}"));
    }

    private UUID gravarOutroPersonal() {
        UUID id = UUID.randomUUID();
        usuarioRepository.save(new Usuario(id, "outro personal", "outro-personal@teste.com", Papel.PERSONAL));
        return id;
    }

    private Aluno gravarAluno(UUID usuarioId, String email, StatusAluno status, UUID personalId) {
        Usuario usuario = usuarioRepository.save(new Usuario(usuarioId, "Ana", email, Papel.ALUNO));
        Aluno aluno = new Aluno(usuario, personalId, null, null, null, null);
        aluno.alterarStatus(status);
        return alunoRepository.save(aluno);
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
