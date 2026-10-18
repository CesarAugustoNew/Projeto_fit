package com.projetofit.comum.erro;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller usado apenas nos testes do tratador global de erros. */
@RestController
@RequestMapping("/teste")
class ControllerDeTeste {

    record Entrada(@NotBlank String nome, @Min(1) int series) {
    }

    @PostMapping("/validacao")
    void validacao(@Valid @RequestBody Entrada entrada) {
    }

    @GetMapping("/nao-encontrado")
    void naoEncontrado() {
        throw new RecursoNaoEncontradoException("Aluno não encontrado");
    }

    @GetMapping("/conflito")
    void conflito() {
        throw new ConflitoException("E-mail já cadastrado");
    }

    @GetMapping("/externo")
    void externo() {
        throw new IntegracaoExternaException("Supabase indisponível", new RuntimeException("timeout"));
    }

    @GetMapping("/acesso-negado")
    void acessoNegado() {
        throw new AccessDeniedException("detalhe interno de acesso");
    }

    @GetMapping("/nao-autenticado")
    void naoAutenticado() {
        throw new InsufficientAuthenticationException("detalhe interno de autenticacao");
    }

    @GetMapping("/erro")
    void erro() {
        throw new IllegalStateException("segredo interno");
    }
}
