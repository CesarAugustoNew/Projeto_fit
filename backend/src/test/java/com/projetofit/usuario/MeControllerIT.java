package com.projetofit.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.projetofit.TesteIntegracao;
import com.projetofit.ficha.FichaRepository;
import com.projetofit.sessao.SessaoRepository;
import com.projetofit.aluno.Aluno;
import com.projetofit.aluno.AlunoRepository;
import com.projetofit.exercicio.ExercicioRepository;
import com.projetofit.aluno.StatusAluno;
import com.projetofit.seguranca.JwtDeTesteConfig;
import com.projetofit.seguranca.TokenDeTeste;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MeControllerIT extends TesteIntegracao {

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

    @BeforeEach
    void limpar() {
        sessaoRepository.deleteAll();
        fichaRepository.deleteAll();
        exercicioRepository.deleteAll();
        alunoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void semTokenRetorna401ComProblemDetail() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Não autenticado"))
                .andExpect(jsonPath("$.detail").value("Token ausente, inválido ou expirado."))
                .andExpect(jsonPath("$.instance").value("/api/me"))
                .andExpect(jsonPath("$.properties").doesNotExist());
    }

    @Test
    void tokenExpiradoRetorna401() throws Exception {
        String token = TokenDeTeste.gerar(UUID.randomUUID(), "personal@teste.com",
                Instant.now().minus(Duration.ofMinutes(1)), TokenDeTeste.ISSUER_PADRAO);
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Não autenticado"));
        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    void tokenDeOutroIssuerRetorna401() throws Exception {
        String token = TokenDeTeste.gerar(UUID.randomUUID(), "personal@teste.com",
                Instant.now().plus(Duration.ofHours(1)), "https://outro.supabase.co/auth/v1");
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"));
        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    void tokenSemAudienciaAuthenticatedRetorna401() throws Exception {
        String token = TokenDeTeste.assinar(TokenDeTeste.claimsPadrao(UUID.randomUUID().toString(), "personal@teste.com",
                        Instant.now().plus(Duration.ofHours(1)), TokenDeTeste.ISSUER_PADRAO).audience("anon").build(),
                JwtDeTesteConfig.CHAVE);
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"));
        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    void tokenAssinadoPorOutraChaveRetorna401() throws Exception {
        String token = TokenDeTeste.assinar(TokenDeTeste.claimsPadrao(UUID.randomUUID().toString(), "personal@teste.com",
                        Instant.now().plus(Duration.ofHours(1)), TokenDeTeste.ISSUER_PADRAO).build(),
                new ECKeyGenerator(Curve.P_256).keyID("chave-de-teste").generate());
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"));
        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    void subQueNaoEhUuidRetorna401() throws Exception {
        String token = TokenDeTeste.assinar(TokenDeTeste.claimsPadrao("nao-eh-uuid", "personal@teste.com",
                        Instant.now().plus(Duration.ofHours(1)), TokenDeTeste.ISSUER_PADRAO).build(),
                JwtDeTesteConfig.CHAVE);
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"));
        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    void tokenMalformadoRetorna401() throws Exception {
        mvc.perform(get("/api/me").header("Authorization", "Bearer nao-eh-um-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"));
    }

    @Test
    void primeiroAcessoDoPersonalCriaUsuarioPersonal() throws Exception {
        UUID sub = UUID.randomUUID();
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + TokenDeTeste.gerar(sub, "personal@teste.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sub.toString()))
                .andExpect(jsonPath("$.nome").value("personal"))
                .andExpect(jsonPath("$.email").value("personal@teste.com"))
                .andExpect(jsonPath("$.papel").value("PERSONAL"));
        assertThat(usuarioRepository.findById(sub)).isPresent();
    }

    @Test
    void emailDoPersonalComMaiusculasTambemFazBootstrap() throws Exception {
        UUID sub = UUID.randomUUID();
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + TokenDeTeste.gerar(sub, "Personal@Teste.COM")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("PERSONAL"));
        assertThat(usuarioRepository.findById(sub))
                .get()
                .extracting(Usuario::getEmail)
                .isEqualTo("personal@teste.com");
    }

    @Test
    void segundoAcessoNaoDuplica() throws Exception {
        String token = TokenDeTeste.gerar(UUID.randomUUID(), "personal@teste.com");
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    void usuarioDesconhecidoRetorna403() throws Exception {
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + TokenDeTeste.gerar(UUID.randomUUID(), "outro@teste.com")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Acesso negado"))
                .andExpect(jsonPath("$.detail").value("Você não tem permissão para acessar este recurso."));
        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    void alunoExistenteRetornaPapelAluno() throws Exception {
        UUID sub = UUID.randomUUID();
        gravarAlunoAtivo(sub);
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + TokenDeTeste.gerar(sub, "ana@teste.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sub.toString()))
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.papel").value("ALUNO"));
    }

    @Test
    void alunoEmEndpointSoDoPersonalRetorna403PeloTratadorGlobal() throws Exception {
        UUID sub = UUID.randomUUID();
        gravarAlunoAtivo(sub);
        mvc.perform(get("/api/teste/so-personal").header("Authorization", "Bearer " + TokenDeTeste.gerar(sub, "ana@teste.com")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Acesso negado"))
                .andExpect(jsonPath("$.detail").value("Você não tem permissão para acessar este recurso."));
    }

    @Test
    void personalEmEndpointSoDoPersonalRetorna200() throws Exception {
        mvc.perform(get("/api/teste/so-personal")
                        .header("Authorization", "Bearer " + TokenDeTeste.gerar(UUID.randomUUID(), "personal@teste.com")))
                .andExpect(status().isOk());
    }

    @Test
    void healthEhPublico() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void rotaForaDaApiEhNegada() throws Exception {
        mvc.perform(get("/teste/erro")
                        .header("Authorization", "Bearer " + TokenDeTeste.gerar(UUID.randomUUID(), "personal@teste.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rotaInexistenteRetorna404EmPtBr() throws Exception {
        mvc.perform(get("/api/nao-existe")
                        .header("Authorization", "Bearer " + TokenDeTeste.gerar(UUID.randomUUID(), "personal@teste.com")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Rota não encontrada."));
    }

    @Test
    void dispatchDeErroDoContainerNaoViraNaoAutenticado() throws Exception {
        // Simula o forward do Tomcat para /error após uma exceção que escapou da cadeia de filtros
        // (ex.: banco fora do ar durante a conversão do JWT): a resposta deve continuar 500, não 401/403.
        mvc.perform(get("/error").with(requisicao -> {
                    requisicao.setDispatcherType(DispatcherType.ERROR);
                    requisicao.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
                    requisicao.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/api/me");
                    return requisicao;
                }))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void preflightCorsDoFrontEhPermitido() throws Exception {
        mvc.perform(options("/api/me").header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Max-Age", "3600"));
    }

    @Test
    void origemNaoPermitidaEhBloqueada() throws Exception {
        mvc.perform(options("/api/me").header("Origin", "https://malicioso.com")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isForbidden());
    }

    private void gravarAlunoAtivo(UUID sub) {
        UUID personalId = UUID.randomUUID();
        usuarioRepository.save(new Usuario(personalId, "personal", "personal@teste.com", Papel.PERSONAL));
        Usuario usuario = usuarioRepository.save(new Usuario(sub, "Ana", "ana@teste.com", Papel.ALUNO));
        Aluno aluno = new Aluno(usuario, personalId, null, null, null, null);
        aluno.alterarStatus(StatusAluno.ATIVO);
        alunoRepository.save(aluno);
    }
}
