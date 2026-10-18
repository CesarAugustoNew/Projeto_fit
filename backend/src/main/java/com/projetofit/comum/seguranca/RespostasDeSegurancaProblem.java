package com.projetofit.comum.seguranca;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Respostas 401/403 em Problem Details, escritas pela cadeia de filtros do Spring Security. Os mesmos
 * problemas ({@link #naoAutenticado()} / {@link #acessoNegado()}) são usados pelo tratador global de erros
 * quando a exceção nasce dentro de um controller (segurança de método).
 */
@Component
public class RespostasDeSegurancaProblem implements AuthenticationEntryPoint, AccessDeniedHandler {

    public static final String TITULO_NAO_AUTENTICADO = "Não autenticado";
    public static final String DETALHE_NAO_AUTENTICADO = "Token ausente, inválido ou expirado.";
    public static final String TITULO_ACESSO_NEGADO = "Acesso negado";
    public static final String DETALHE_ACESSO_NEGADO = "Você não tem permissão para acessar este recurso.";

    private final JsonMapper jsonMapper;

    public RespostasDeSegurancaProblem(JsonMapper jsonMapper) {
        // Mesmo formato que o conversor HTTP do Spring MVC usa para ProblemDetail (propriedades extras no topo).
        this.jsonMapper = jsonMapper.rebuild()
                .addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
                .build();
    }

    public static ProblemDetail naoAutenticado() {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, DETALHE_NAO_AUTENTICADO);
        problema.setTitle(TITULO_NAO_AUTENTICADO);
        return problema;
    }

    public static ProblemDetail acessoNegado() {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, DETALHE_ACESSO_NEGADO);
        problema.setTitle(TITULO_ACESSO_NEGADO);
        return problema;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        escrever(request, response, naoAutenticado());
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        escrever(request, response, acessoNegado());
    }

    private void escrever(HttpServletRequest request, HttpServletResponse response, ProblemDetail problema)
            throws IOException {
        try {
            problema.setInstance(URI.create(request.getRequestURI()));
        } catch (IllegalArgumentException e) {
            // caminho que não forma uma URI válida: responde sem "instance"
        }
        response.setStatus(problema.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(jsonMapper.writeValueAsBytes(problema));
    }
}
