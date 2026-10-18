package com.projetofit.ficha;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.projetofit.aluno.Aluno;
import com.projetofit.aluno.AlunoRepository;
import com.projetofit.comum.erro.ConflitoException;
import com.projetofit.exercicio.Exercicio;
import com.projetofit.exercicio.ExercicioRepository;
import com.projetofit.exercicio.GrupoMuscular;
import com.projetofit.usuario.Papel;
import com.projetofit.usuario.Usuario;
import com.projetofit.usuario.UsuarioAtual;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class FichaServiceTest {

    private static final UUID PERSONAL_ID = UUID.fromString("99999999-8888-7777-6666-555555555555");
    private static final UUID ALUNO_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID EXERCICIO_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    private final FichaRepository fichaRepository = mock(FichaRepository.class);
    private final AlunoRepository alunoRepository = mock(AlunoRepository.class);
    private final ExercicioRepository exercicioRepository = mock(ExercicioRepository.class);
    private final UsuarioAtual usuarioAtual = mock(UsuarioAtual.class);
    private FichaService service;
    private Aluno aluno;

    @BeforeEach
    void preparar() {
        service = new FichaService(fichaRepository, alunoRepository, exercicioRepository, usuarioAtual,
                Clock.systemUTC());
        when(usuarioAtual.id()).thenReturn(PERSONAL_ID);
        aluno = new Aluno(new Usuario(UUID.randomUUID(), "Ana", "ana@teste.com", Papel.ALUNO), PERSONAL_ID,
                null, null, null, null);
        when(alunoRepository.findByIdAndPersonalId(ALUNO_ID, PERSONAL_ID)).thenReturn(Optional.of(aluno));
        when(exercicioRepository.findByIdAndPersonalId(EXERCICIO_ID, PERSONAL_ID))
                .thenReturn(Optional.of(new Exercicio(PERSONAL_ID, "Supino", GrupoMuscular.PEITO, null, null)));
    }

    @Test
    void corridaNaCriacaoViraConflito() {
        when(fichaRepository.findByAlunoIdAndStatus(ALUNO_ID, StatusFicha.ATIVA)).thenReturn(Optional.empty());
        when(fichaRepository.saveAndFlush(any(Ficha.class)))
                .thenThrow(new DataIntegrityViolationException("ficha_uma_ativa_por_aluno"));

        assertThatThrownBy(() -> service.criar(ALUNO_ID, requisicao()))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Este aluno já tem uma ficha ativa. Recarregue a página e tente de novo.");
    }

    @Test
    void violacaoDeIntegridadeNaEdicaoViraConflito() {
        UUID fichaId = UUID.randomUUID();
        Ficha ficha = new Ficha(aluno, "Ficha", null, 3, LocalDate.of(2026, 10, 1), null);
        when(fichaRepository.findByIdAndAlunoPersonalId(fichaId, PERSONAL_ID)).thenReturn(Optional.of(ficha));
        doThrow(new DataIntegrityViolationException("fk")).when(fichaRepository).flush();

        assertThatThrownBy(() -> service.atualizar(fichaId, requisicao()))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Não foi possível salvar a ficha. Recarregue a página e tente de novo.");
    }

    private static FichaRequest requisicao() {
        ItemRequest item = new ItemRequest(null, EXERCICIO_ID, 3, "10", null, null, null);
        return new FichaRequest("Ficha", null, 3, LocalDate.of(2026, 10, 1), null,
                List.of(new DivisaoRequest(null, "A", List.of(item))));
    }
}
