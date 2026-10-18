package com.projetofit.comum.erro;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TratadorGlobalDeErrosTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ControllerDeTeste())
            .setControllerAdvice(new TratadorGlobalDeErros()).build();

    @Test
    void validacaoRetorna400ComCampos() throws Exception {
        mvc.perform(post("/teste/validacao").contentType(APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"series\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value("Um ou mais campos são inválidos."))
                .andExpect(jsonPath("$.errors.nome").exists())
                .andExpect(jsonPath("$.errors.series").exists());
    }

    @Test
    void naoEncontradoRetorna404() throws Exception {
        mvc.perform(get("/teste/nao-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Aluno não encontrado"));
    }

    @Test
    void conflitoRetorna409() throws Exception {
        mvc.perform(get("/teste/conflito"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Conflito"))
                .andExpect(jsonPath("$.detail").value("E-mail já cadastrado"));
    }

    @Test
    void integracaoRetorna502() throws Exception {
        mvc.perform(get("/teste/externo"))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Falha em serviço externo"));
    }

    @Test
    void erroInesperadoNaoVazaDetalhes() throws Exception {
        mvc.perform(get("/teste/erro"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Erro interno"))
                .andExpect(jsonPath("$.detail").value("Ocorreu um erro inesperado."))
                .andExpect(content().string(not(containsString("segredo interno"))));
    }

    @Test
    void accessDeniedExceptionRetorna403() throws Exception {
        mvc.perform(get("/teste/acesso-negado"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Acesso negado"))
                .andExpect(jsonPath("$.detail").value("Você não tem permissão para acessar este recurso."));
    }

    @Test
    void authenticationExceptionRetorna401() throws Exception {
        mvc.perform(get("/teste/nao-autenticado"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Não autenticado"))
                .andExpect(jsonPath("$.detail").value("Token ausente, inválido ou expirado."));
    }

    @Test
    void corpoMalformadoRetorna400() throws Exception {
        mvc.perform(post("/teste/validacao").contentType(APPLICATION_JSON).content("{nome:"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value("Corpo da requisição inválido."));
    }

    @Test
    void tipoInvalidoRetorna400() throws Exception {
        mvc.perform(get("/teste/numero/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value("Parâmetro 'n' inválido."));
    }

    @Test
    void validacaoDeParametroRetorna400ComErrors() throws Exception {
        mvc.perform(get("/teste/param").param("qtd", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value("Um ou mais campos são inválidos."))
                .andExpect(jsonPath("$.errors.qtd").exists());
    }
}
