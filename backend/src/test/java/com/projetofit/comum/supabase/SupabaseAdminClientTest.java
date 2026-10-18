package com.projetofit.comum.supabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.projetofit.comum.AppProperties;
import com.projetofit.comum.erro.ConflitoException;
import com.projetofit.comum.erro.IntegracaoExternaException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SupabaseAdminClientTest {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String BASE = "https://teste.supabase.co/auth/v1";
    private static final String CONVITE = BASE + "/invite?redirect_to=http://localhost:4200/definir-senha";

    private MockRestServiceServer servidor;
    private SupabaseAdminClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new SupabaseAdminClient(builder, propriedades("chave-de-teste"), false);
    }

    private static AppProperties propriedades(String chave) {
        return new AppProperties("https://teste.supabase.co", "personal@teste.com", "http://localhost:4200", chave);
    }

    @Test
    void convidarEnviaPostComChaveERedirectEDevolveId() {
        servidor.expect(requestTo(CONVITE))
                .andExpect(method(POST))
                .andExpect(header("apikey", "chave-de-teste"))
                .andExpect(header("Authorization", "Bearer chave-de-teste"))
                .andExpect(jsonPath("$.email").value("ana@teste.com"))
                .andExpect(jsonPath("$.data.nome").value("Ana"))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"email\":\"ana@teste.com\"}", APPLICATION_JSON));

        assertThat(cliente.convidar("ana@teste.com", "Ana")).isEqualTo(ID);
        servidor.verify();
    }

    @Test
    void convidarEmailExistenteViraConflito() {
        servidor.expect(requestTo(CONVITE)).andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThatThrownBy(() -> cliente.convidar("ana@teste.com", "Ana"))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("E-mail já cadastrado.");
    }

    @Test
    void convidarErroDoSupabaseViraIntegracaoExterna() {
        servidor.expect(requestTo(CONVITE)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> cliente.convidar("ana@teste.com", "Ana"))
                .isInstanceOf(IntegracaoExternaException.class)
                .hasMessage("Não foi possível enviar o convite agora. Tente novamente.");
    }

    @Test
    void excluirUsuarioEnviaDelete() {
        servidor.expect(requestTo(BASE + "/admin/users/" + ID))
                .andExpect(method(DELETE))
                .andExpect(header("apikey", "chave-de-teste"))
                .andRespond(withSuccess());

        cliente.excluirUsuario(ID);
        servidor.verify();
    }

    @Test
    void excluirUsuarioComErroViraIntegracaoExterna() {
        servidor.expect(requestTo(BASE + "/admin/users/" + ID))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> cliente.excluirUsuario(ID))
                .isInstanceOf(IntegracaoExternaException.class)
                .hasMessage("Não foi possível excluir o usuário no Supabase.");
    }

    @Test
    void semChaveNaoSobe() {
        assertThatThrownBy(() -> new SupabaseAdminClient(RestClient.builder(), propriedades("")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("app.supabase-service-role-key (SUPABASE_SERVICE_ROLE_KEY) não configurado");
    }
}
