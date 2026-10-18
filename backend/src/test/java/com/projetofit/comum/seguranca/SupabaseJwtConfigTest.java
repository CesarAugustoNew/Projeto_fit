package com.projetofit.comum.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.projetofit.comum.AppProperties;
import com.projetofit.seguranca.TokenDeTeste;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;

/**
 * Testa o {@link JwtDecoder} de produção contra um servidor JWKS local (JDK HttpServer), sem contexto Spring:
 * ES256 com a chave publicada é aceito; HS256, outra chave e audiência errada são rejeitados.
 */
class SupabaseJwtConfigTest {

    private static final String CAMINHO_JWKS = "/auth/v1/.well-known/jwks.json";

    private static HttpServer servidor;
    private static ECKey chave;
    private static String issuer;
    private static JwtDecoder decoder;

    @BeforeAll
    static void subirServidorJwks() throws Exception {
        chave = new ECKeyGenerator(Curve.P_256).keyID("chave-supabase").generate();
        byte[] jwks = new JWKSet(chave.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);

        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        servidor.createContext(CAMINHO_JWKS, troca -> {
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(200, jwks.length);
            try (OutputStream corpo = troca.getResponseBody()) {
                corpo.write(jwks);
            }
        });
        servidor.start();

        String supabaseUrl = "http://127.0.0.1:" + servidor.getAddress().getPort();
        issuer = supabaseUrl + "/auth/v1";
        AppProperties propriedades = new AppProperties(supabaseUrl, "personal@teste.com", "http://localhost:4200");
        SupabaseJwtConfig config = new SupabaseJwtConfig();
        decoder = config.jwtDecoder(propriedades, config.supabaseJwtValidator(propriedades));
    }

    @AfterAll
    static void derrubarServidor() {
        servidor.stop(0);
    }

    private static JWTClaimsSet.Builder claims() {
        return TokenDeTeste.claimsPadrao(UUID.randomUUID().toString(), "personal@teste.com",
                Instant.now().plus(Duration.ofHours(1)), issuer);
    }

    @Test
    void tokenEs256ValidoEhDecodificado() {
        JWTClaimsSet claims = claims().build();

        Jwt jwt = decoder.decode(TokenDeTeste.assinar(claims, chave));

        assertThat(jwt.getSubject()).isEqualTo(claims.getSubject());
        assertThat(jwt.getClaimAsString("email")).isEqualTo("personal@teste.com");
    }

    @Test
    void tokenHs256EhRejeitado() throws JOSEException {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims().build());
        jwt.sign(new MACSigner("segredo-compartilhado-de-teste-com-32-bytes!".getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> decoder.decode(jwt.serialize())).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenAssinadoPorOutraChaveEhRejeitado() throws JOSEException {
        // mesmo kid da chave publicada, mas outro par de chaves
        ECKey outra = new ECKeyGenerator(Curve.P_256).keyID(chave.getKeyID()).generate();

        assertThatThrownBy(() -> decoder.decode(TokenDeTeste.assinar(claims().build(), outra)))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void audienciaErradaEhRejeitada() {
        String token = TokenDeTeste.assinar(claims().audience("anon").build(), chave);

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }
}
