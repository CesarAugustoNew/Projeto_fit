package com.projetofit.comum.supabase;

import com.projetofit.comum.AppProperties;
import com.projetofit.comum.erro.ConflitoException;
import com.projetofit.comum.erro.IntegracaoExternaException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Cliente da Admin API do Supabase Auth (GoTrue). */
@Component
public class SupabaseAdminClient implements SupabaseAdmin {

    private final RestClient restClient;
    private final String frontUrl;

    @Autowired
    public SupabaseAdminClient(RestClient.Builder builder, AppProperties props) {
        this(builder, props, true);
    }

    /**
     * Nos testes, {@code aplicarTimeouts = false} preserva a fábrica de requisições que o
     * servidor simulado instala no builder.
     */
    SupabaseAdminClient(RestClient.Builder builder, AppProperties props, boolean aplicarTimeouts) {
        String chave = props.supabaseServiceRoleKey();
        if (chave == null || chave.isBlank()) {
            throw new IllegalStateException(
                    "app.supabase-service-role-key (SUPABASE_SERVICE_ROLE_KEY) não configurado");
        }
        if (aplicarTimeouts) {
            JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
            fabrica.setReadTimeout(Duration.ofSeconds(10));
            builder.requestFactory(fabrica);
        }
        this.frontUrl = props.frontUrl();
        this.restClient = builder
                .baseUrl(props.supabaseUrl().replaceAll("/+$", "") + "/auth/v1")
                .defaultHeader("apikey", chave)
                .defaultHeader("Authorization", "Bearer " + chave)
                .build();
    }

    @Override
    public UUID convidar(String email, String nome) {
        try {
            Map<?, ?> resposta = restClient.post()
                    .uri("/invite?redirect_to=" + frontUrl + "/definir-senha")
                    .body(Map.of("email", email, "data", Map.of("nome", nome)))
                    .retrieve()
                    .body(Map.class);
            return UUID.fromString(String.valueOf(resposta.get("id")));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.UNPROCESSABLE_ENTITY)) {
                throw new ConflitoException("E-mail já cadastrado.");
            }
            throw falhaConvite(e);
        } catch (RestClientException | NullPointerException | IllegalArgumentException e) {
            throw falhaConvite(e);
        }
    }

    @Override
    public void excluirUsuario(UUID id) {
        try {
            restClient.delete().uri("/admin/users/{id}", id).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw new IntegracaoExternaException("Não foi possível excluir o usuário no Supabase.", e);
        }
    }

    private static IntegracaoExternaException falhaConvite(Exception causa) {
        return new IntegracaoExternaException("Não foi possível enviar o convite agora. Tente novamente.", causa);
    }
}
