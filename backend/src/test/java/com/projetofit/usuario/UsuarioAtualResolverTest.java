package com.projetofit.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.projetofit.comum.AppProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class UsuarioAtualResolverTest {

    private final UsuarioRepository repositorio = mock(UsuarioRepository.class);
    private final AcessoDoAluno acessoDoAluno = mock(AcessoDoAluno.class);
    private final BootstrapPersonal bootstrapPersonal = mock(BootstrapPersonal.class);
    private final UsuarioAtualResolver resolver = new UsuarioAtualResolver(repositorio,
            new AppProperties("https://teste.supabase.co", "personal@teste.com", "http://localhost:4200", "chave-de-teste"),
            acessoDoAluno, bootstrapPersonal);

    @Test
    void primeiroAcessoDoPersonalUsaBootstrap() {
        UUID sub = UUID.randomUUID();
        Usuario usuario = new Usuario(sub, "personal", "personal@teste.com", Papel.PERSONAL);
        when(repositorio.findById(sub)).thenReturn(Optional.empty());
        when(bootstrapPersonal.criar(sub, "personal", "personal@teste.com")).thenReturn(usuario);

        assertThat(resolver.resolver(sub, "personal@teste.com")).contains(usuario);
        verify(bootstrapPersonal).criar(sub, "personal", "personal@teste.com");
    }

    @Test
    void insercaoConcorrenteRelêOUsuario() {
        UUID sub = UUID.randomUUID();
        Usuario usuario = new Usuario(sub, "personal", "personal@teste.com", Papel.PERSONAL);
        when(repositorio.findById(sub)).thenReturn(Optional.empty(), Optional.of(usuario));
        when(bootstrapPersonal.criar(sub, "personal", "personal@teste.com"))
                .thenThrow(new DataIntegrityViolationException("duplicada"));

        assertThat(resolver.resolver(sub, "personal@teste.com")).contains(usuario);
    }

    @Test
    void personalExistenteNaoPassaPeloAcessoDoAluno() {
        UUID sub = UUID.randomUUID();
        Usuario usuario = new Usuario(sub, "personal", "personal@teste.com", Papel.PERSONAL);
        when(repositorio.findById(sub)).thenReturn(Optional.of(usuario));

        assertThat(resolver.resolver(sub, "personal@teste.com")).contains(usuario);
        verify(acessoDoAluno, never()).liberar(any());
        verify(bootstrapPersonal, never()).criar(any(), any(), any());
    }

    @Test
    void emailDiferenteDoPersonalNaoCriaNada() {
        UUID sub = UUID.randomUUID();
        when(repositorio.findById(sub)).thenReturn(Optional.empty());

        assertThat(resolver.resolver(sub, "x@teste.com")).isEmpty();
        verify(bootstrapPersonal, never()).criar(any(), any(), any());
    }

    @Test
    void semEmailNaoCriaNada() {
        UUID sub = UUID.randomUUID();
        when(repositorio.findById(sub)).thenReturn(Optional.empty());

        assertThat(resolver.resolver(sub, null)).isEmpty();
        verify(bootstrapPersonal, never()).criar(any(), any(), any());
    }

    @Test
    void alunoInativoNaoTemPapel() {
        UUID sub = UUID.randomUUID();
        when(repositorio.findById(sub)).thenReturn(Optional.of(new Usuario(sub, "Ana", "ana@teste.com", Papel.ALUNO)));
        when(acessoDoAluno.liberar(sub)).thenReturn(false);

        assertThat(resolver.resolver(sub, "ana@teste.com")).isEmpty();
    }

    @Test
    void pacoteUsuarioNaoDependeDeAluno() throws IOException {
        Path pacote = Path.of("src/main/java/com/projetofit/usuario");
        List<Path> fontes;
        try (Stream<Path> arquivos = Files.list(pacote)) {
            fontes = arquivos.filter(arquivo -> arquivo.toString().endsWith(".java")).toList();
        }

        assertThat(fontes).isNotEmpty();
        for (Path fonte : fontes) {
            assertThat(Files.readString(fonte))
                    .as("%s não deve importar o pacote aluno", fonte.getFileName())
                    .doesNotContain("import com.projetofit.aluno");
        }
    }
}
