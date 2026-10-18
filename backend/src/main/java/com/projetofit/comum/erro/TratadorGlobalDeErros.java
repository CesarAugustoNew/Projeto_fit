package com.projetofit.comum.erro;

import com.projetofit.comum.seguranca.RespostasDeSegurancaProblem;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Converte exceções em respostas Problem Details ({@code application/problem+json}). */
@RestControllerAdvice
public class TratadorGlobalDeErros extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalDeErros.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            erros.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Um ou mais campos são inválidos.");
        problema.setProperty("errors", erros);
        return handleExceptionInternal(ex, problema, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(resultado -> {
            MethodParameter parametro = resultado.getMethodParameter();
            String nome = parametro.getParameterName() != null
                    ? parametro.getParameterName() : "parametro" + parametro.getParameterIndex();
            resultado.getResolvableErrors().stream().findFirst()
                    .ifPresent(erro -> erros.putIfAbsent(nome, erro.getDefaultMessage()));
        });
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Um ou mais campos são inválidos.");
        problema.setProperty("errors", erros);
        return handleExceptionInternal(ex, problema, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(ex, problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Corpo da requisição inválido."), headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String nome = ex instanceof MethodArgumentTypeMismatchException m ? m.getName() : ex.getPropertyName();
        return handleExceptionInternal(ex, problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Parâmetro '" + nome + "' inválido."), headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(ex, problema(HttpStatus.NOT_FOUND, "Recurso não encontrado",
                "Rota não encontrada."), headers, HttpStatus.NOT_FOUND, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(ex, problema(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido",
                "Método " + ex.getMethod() + " não é suportado nesta rota."), headers,
                HttpStatus.METHOD_NOT_ALLOWED, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(ex, problema(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Tipo de conteúdo não suportado", "Envie o corpo em JSON (application/json)."), headers,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(ex, problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Parâmetro '" + ex.getParameterName() + "' é obrigatório."), headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(RecursoNaoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    ProblemDetail conflito(ConflitoException ex) {
        return problema(HttpStatus.CONFLICT, "Conflito", ex.getMessage());
    }

    @ExceptionHandler(IntegracaoExternaException.class)
    ProblemDetail integracaoExterna(IntegracaoExternaException ex) {
        log.warn("Falha em serviço externo", ex);
        return problema(HttpStatus.BAD_GATEWAY, "Falha em serviço externo", ex.getMessage());
    }

    /** Segurança de método ({@code @PreAuthorize}) lança dentro do controller; sem isto viraria 500. */
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail acessoNegado(AccessDeniedException ex) {
        return RespostasDeSegurancaProblem.acessoNegado();
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail naoAutenticado(AuthenticationException ex) {
        return RespostasDeSegurancaProblem.naoAutenticado();
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception ex) {
        log.error("Erro inesperado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Ocorreu um erro inesperado.");
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }
}
