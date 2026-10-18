package com.projetofit.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.projetofit.comum.AppProperties;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class UsuarioAtualResolverTest {

    private final UsuarioRepository repositorio = mock(UsuarioRepository.class);
    private final UsuarioAtualResolver resolver = new UsuarioAtualResolver(repositorio,
            new AppProperties("https://teste.supabase.co", "personal@teste.com", "http://localhost:4200"));

    @Test
    void insercaoConcorrenteRelêOUsuario() {
        UUID sub = UUID.randomUUID();
        Usuario usuario = new Usuario(sub, "personal", "personal@teste.com", Papel.PERSONAL);
        when(repositorio.findById(sub)).thenReturn(Optional.empty(), Optional.of(usuario));
        when(repositorio.saveAndFlush(any(Usuario.class))).thenThrow(new DataIntegrityViolationException("duplicada"));

        assertThat(resolver.resolver(sub, "personal@teste.com")).contains(usuario);
    }

    @Test
    void emailDiferenteDoPersonalNaoCriaNada() {
        UUID sub = UUID.randomUUID();
        when(repositorio.findById(sub)).thenReturn(Optional.empty());

        assertThat(resolver.resolver(sub, "x@teste.com")).isEmpty();
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void semEmailNaoCriaNada() {
        UUID sub = UUID.randomUUID();
        when(repositorio.findById(sub)).thenReturn(Optional.empty());

        assertThat(resolver.resolver(sub, null)).isEmpty();
        verify(repositorio, never()).saveAndFlush(any());
    }
}
