package com.projetofit.comum.seguranca;

import com.projetofit.usuario.UsuarioAtualResolver;
import java.util.List;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Converte um JWT válido do Supabase em autenticação, com o papel lido da tabela {@code usuario}
 * ({@code ROLE_PERSONAL} / {@code ROLE_ALUNO}). Sem usuário cadastrado, a autenticação fica sem authorities
 * e as rotas {@code /api/**} respondem 403.
 */
@Component
public class UsuarioJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioAtualResolver usuarioAtualResolver;

    public UsuarioJwtAuthenticationConverter(UsuarioAtualResolver usuarioAtualResolver) {
        this.usuarioAtualResolver = usuarioAtualResolver;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID sub = sub(jwt);
        List<GrantedAuthority> authorities = usuarioAtualResolver.resolver(sub, jwt.getClaimAsString("email"))
                .<List<GrantedAuthority>>map(usuario -> List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPapel().name())))
                .orElse(List.of());
        return new JwtAuthenticationToken(jwt, authorities);
    }

    /** O {@code sub} do Supabase é sempre um UUID; qualquer outra coisa é token inválido (401). */
    static UUID sub(Jwt jwt) {
        String sub = jwt.getSubject();
        if (sub == null) {
            throw new InvalidBearerTokenException("Token sem claim sub");
        }
        try {
            return UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            throw new InvalidBearerTokenException("Claim sub não é um UUID");
        }
    }
}
