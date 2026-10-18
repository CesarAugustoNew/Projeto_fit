package com.projetofit.sessao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.projetofit.aluno.Aluno;
import com.projetofit.aluno.AlunoRepository;
import com.projetofit.comum.erro.ConflitoException;
import com.projetofit.exercicio.ExercicioRepository;
import com.projetofit.ficha.DivisaoRepository;
import com.projetofit.usuario.UsuarioAtual;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** Caminho de corrida de {@link SessaoService#registrar}: dois envios simultâneos do mesmo clientId. */
class SessaoServiceTest {

    private final AlunoLogado alunoLogado = mock(AlunoLogado.class);
    private final SessaoRepository sessaoRepository = mock(SessaoRepository.class);
    private final Aluno ana = mock(Aluno.class);
    private final UUID anaId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();
    private SessaoService service;
    private SessaoRequest req;

    @BeforeEach
    void preparar() {
        PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
        when(tm.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        Instant agora = Instant.parse("2026-10-18T12:00:00Z");
        service = new SessaoService(alunoLogado, sessaoRepository, mock(DivisaoRepository.class),
                mock(ExercicioRepository.class), Clock.fixed(agora, ZoneOffset.UTC), tm,
                mock(UsuarioAtual.class), mock(AlunoRepository.class));
        when(ana.getId()).thenReturn(anaId);
        when(alunoLogado.obter()).thenReturn(ana);
        req = new SessaoRequest(clientId, null, "Treino A", agora.minusSeconds(3600), agora.minusSeconds(300),
                null, List.of());
    }

    @Test
    void colisaoComVencedorDoMesmoAlunoDevolveSessaoExistente() {
        Sessao vencedora = sessaoDe(anaId);
        when(sessaoRepository.findByClientId(clientId)).thenReturn(Optional.empty(), Optional.of(vencedora));
        when(sessaoRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk client_id"));

        SessaoService.Resultado resultado = service.registrar(req);

        assertThat(resultado.criada()).isFalse();
        assertThat(resultado.sessao().id()).isEqualTo(vencedora.getId());
    }

    @Test
    void colisaoComVencedorDeOutroAlunoRetornaConflito() {
        Sessao vencedora = sessaoDe(UUID.randomUUID());
        when(sessaoRepository.findByClientId(clientId)).thenReturn(Optional.empty(), Optional.of(vencedora));
        when(sessaoRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk client_id"));

        assertThatThrownBy(() -> service.registrar(req))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Sessão já registrada.");
    }

    @Test
    void colisaoSemSessaoNaReleituraRelancaAViolacaoOriginal() {
        DataIntegrityViolationException original = new DataIntegrityViolationException("outra violacao");
        when(sessaoRepository.findByClientId(clientId)).thenReturn(Optional.empty(), Optional.empty());
        when(sessaoRepository.saveAndFlush(any())).thenThrow(original);

        assertThatThrownBy(() -> service.registrar(req)).isSameAs(original);
    }

    private static Sessao sessaoDe(UUID alunoId) {
        Aluno dono = mock(Aluno.class);
        when(dono.getId()).thenReturn(alunoId);
        Sessao sessao = mock(Sessao.class);
        when(sessao.getId()).thenReturn(UUID.randomUUID());
        when(sessao.getAluno()).thenReturn(dono);
        when(sessao.getSeries()).thenReturn(List.of());
        return sessao;
    }
}
