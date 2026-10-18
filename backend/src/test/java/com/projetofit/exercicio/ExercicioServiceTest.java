package com.projetofit.exercicio;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.projetofit.comum.erro.ConflitoException;
import com.projetofit.usuario.UsuarioAtual;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ExercicioServiceTest {

    private static final UUID PERSONAL_ID = UUID.fromString("99999999-8888-7777-6666-555555555555");
    private static final String MENSAGEM = "Já existe um exercício com este nome.";

    private final ExercicioRepository repository = mock(ExercicioRepository.class);
    private final UsuarioAtual usuarioAtual = mock(UsuarioAtual.class);
    private ExercicioService service;

    @BeforeEach
    void preparar() {
        service = new ExercicioService(repository, usuarioAtual);
        when(usuarioAtual.id()).thenReturn(PERSONAL_ID);
        when(repository.saveAndFlush(any(Exercicio.class)))
                .thenThrow(new DataIntegrityViolationException("exercicio_personal_nome_uk"));
    }

    @Test
    void corridaNaCriacaoViraConflito() {
        var req = new NovoExercicioRequest("Supino", GrupoMuscular.PEITO, null, null);

        assertThatThrownBy(() -> service.criar(req))
                .isInstanceOf(ConflitoException.class).hasMessage(MENSAGEM);
    }

    @Test
    void corridaNaRenomeacaoViraConflito() {
        UUID id = UUID.randomUUID();
        Exercicio exercicio = new Exercicio(PERSONAL_ID, "Remada", GrupoMuscular.COSTAS, null, null);
        when(repository.findByIdAndPersonalId(id, PERSONAL_ID)).thenReturn(Optional.of(exercicio));
        var req = new AtualizarExercicioRequest("Supino", GrupoMuscular.PEITO, null, null, true);

        assertThatThrownBy(() -> service.atualizar(id, req))
                .isInstanceOf(ConflitoException.class).hasMessage(MENSAGEM);
    }
}
