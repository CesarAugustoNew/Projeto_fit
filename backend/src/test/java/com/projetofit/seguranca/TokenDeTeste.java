package com.projetofit.seguranca;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/** Gera JWTs no formato do Supabase Auth, assinados em ES256 com a chave de {@link JwtDeTesteConfig}. */
public final class TokenDeTeste {

    public static final String ISSUER_PADRAO = "https://teste.supabase.co/auth/v1";

    private TokenDeTeste() {
    }

    public static String gerar(UUID sub, String email) {
        return gerar(sub, email, Instant.now().plus(Duration.ofHours(1)), ISSUER_PADRAO);
    }

    public static String gerar(UUID sub, String email, Instant expiracao, String issuer) {
        return assinar(claimsPadrao(sub.toString(), email, expiracao, issuer).build(), JwtDeTesteConfig.CHAVE);
    }

    /** Claims padrão do Supabase, para testes que precisam alterar alguma antes de assinar. */
    public static JWTClaimsSet.Builder claimsPadrao(String sub, String email, Instant expiracao, String issuer) {
        return new JWTClaimsSet.Builder()
                .subject(sub)
                .claim("email", email)
                .claim("role", "authenticated")
                .issuer(issuer)
                .audience("authenticated")
                .issueTime(Date.from(expiracao.minus(Duration.ofHours(1))))
                .expirationTime(Date.from(expiracao));
    }

    public static String assinar(JWTClaimsSet claims, ECKey chave) {
        JWSHeader cabecalho = new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(chave.getKeyID()).build();
        SignedJWT jwt = new SignedJWT(cabecalho, claims);
        try {
            jwt.sign(new ECDSASigner(chave));
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
        return jwt.serialize();
    }
}
