package com.projetofit.comum.seguranca;

import com.projetofit.comum.AppProperties;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Decodificação e validação dos JWTs emitidos pelo Supabase Auth (chaves assimétricas via JWKS). */
@Configuration(proxyBeanMethods = false)
public class SupabaseJwtConfig {

    static final String AUDIENCIA = "authenticated";

    @Bean
    OAuth2TokenValidator<Jwt> supabaseJwtValidator(AppProperties appProperties) {
        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(issuer(appProperties)),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        aud -> aud != null && aud.contains(AUDIENCIA)));
    }

    @Bean
    JwtDecoder jwtDecoder(AppProperties appProperties, OAuth2TokenValidator<Jwt> supabaseJwtValidator) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(issuer(appProperties) + "/.well-known/jwks.json")
                .jwsAlgorithms(algoritmos -> {
                    algoritmos.add(SignatureAlgorithm.ES256);
                    algoritmos.add(SignatureAlgorithm.RS256);
                })
                .build();
        decoder.setJwtValidator(supabaseJwtValidator);
        return decoder;
    }

    private static String issuer(AppProperties appProperties) {
        String url = appProperties.supabaseUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("app.supabase-url (SUPABASE_URL) não configurado");
        }
        return url.replaceAll("/+$", "") + "/auth/v1";
    }
}
