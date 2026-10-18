package com.projetofit.aluno;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.projetofit.comum.erro.ConflitoException;
import com.projetofit.comum.erro.IntegracaoExternaException;
import com.projetofit.comum.supabase.SupabaseAdmin;
import com.projetofit.usuario.Usuario;
import com.projetofit.usuario.UsuarioAtual;
import com.projetofit.usuario.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class AlunoServiceTest {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID PERSONAL_ID = UUID.fromString("99999999-8888-7777-6666-555555555555");

    private final AlunoRepository alunoRepository = mock(AlunoRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final SupabaseAdmin supabaseAdmin = mock(SupabaseAdmin.class);
    private final UsuarioAtual usuarioAtual = mock(UsuarioAtual.class);
    private final NovoAlunoRequest req = new NovoAlunoRequest("Ana", "ana@teste.com", null, null, null, null);

    private AlunoService service;

    @BeforeEach
    void preparar() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));
        service = new AlunoService(alunoRepository, usuarioRepository, supabaseAdmin, usuarioAtual, transactionTemplate);
        when(usuarioAtual.id()).thenReturn(PERSONAL_ID);
        when(usuarioRepository.findByEmailIgnoreCase("ana@teste.com")).thenReturn(Optional.empty());
        when(supabaseAdmin.convidar("ana@teste.com", "Ana")).thenReturn(ID);
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("usuario_email_key"));
    }

    @Test
    void falhaAoGravarExcluiUsuarioNoSupabase() {
        assertThatThrownBy(() -> service.criar(req))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("E-mail já cadastrado.");
        verify(supabaseAdmin).excluirUsuario(ID);
    }

    @Test
    void falhaNaExclusaoCompensatoriaNaoEscondeOErroOriginal() {
        doThrow(new IntegracaoExternaException("Falha ao excluir.", null)).when(supabaseAdmin).excluirUsuario(ID);
        assertThatThrownBy(() -> service.criar(req))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("E-mail já cadastrado.");
        verify(supabaseAdmin).excluirUsuario(ID);
    }
}
