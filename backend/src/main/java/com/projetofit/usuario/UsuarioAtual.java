package com.projetofit.usuario;

import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Acesso ao usuário autenticado na requisição atual. */
@Component
public class UsuarioAtual {

    private static final String PREFIXO_PAPEL = "ROLE_";

    /** O {@code sub} do token (= {@code usuario.id}). */
    public UUID id() {
        return UUID.fromString(autenticacao().getToken().getSubject());
    }

    /** O papel do usuário, conforme a tabela {@code usuario}. */
    public Papel papel() {
        return autenticacao().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority != null && authority.startsWith(PREFIXO_PAPEL))
                .map(authority -> Papel.valueOf(authority.substring(PREFIXO_PAPEL.length())))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Usuário sem papel"));
    }

    private static JwtAuthenticationToken autenticacao() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao instanceof JwtAuthenticationToken jwt) {
            return jwt;
        }
        throw new AuthenticationCredentialsNotFoundException("Requisição sem token JWT");
    }
}
