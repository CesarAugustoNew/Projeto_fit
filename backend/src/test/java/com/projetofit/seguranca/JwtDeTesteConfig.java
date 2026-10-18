package com.projetofit.seguranca;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Substitui o {@link JwtDecoder} de produção (que busca o JWKS do Supabase) por um que confia apenas na chave
 * EC P-256 de teste, mantendo o mesmo validador de produção ({@code supabaseJwtValidator}).
 */
@TestConfiguration(proxyBeanMethods = false)
public class JwtDeTesteConfig {

    /** Par de chaves gerado uma vez por JVM; nunca sai da memória dos testes. */
    public static final ECKey CHAVE = gerarChave();

    @Bean
    @Primary
    JwtDecoder jwtDecoderDeTeste(OAuth2TokenValidator<Jwt> supabaseJwtValidator) {
        DefaultJWTProcessor<SecurityContext> processador = new DefaultJWTProcessor<>();
        processador.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.ES256,
                new ImmutableJWKSet<>(new JWKSet(CHAVE.toPublicJWK()))));
        // As claims são validadas pelo validador do Spring, como no decoder de produção.
        processador.setJWTClaimsSetVerifier((claims, contexto) -> {
        });
        NimbusJwtDecoder decoder = new NimbusJwtDecoder(processador);
        decoder.setJwtValidator(supabaseJwtValidator);
        return decoder;
    }

    private static ECKey gerarChave() {
        try {
            return new ECKeyGenerator(Curve.P_256).keyID("chave-de-teste").generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
