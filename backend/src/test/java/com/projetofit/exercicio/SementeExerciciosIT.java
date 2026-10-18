package com.projetofit.exercicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.projetofit.TesteIntegracao;
import com.projetofit.aluno.AlunoRepository;
import com.projetofit.seguranca.TokenDeTeste;
import com.projetofit.usuario.Papel;
import com.projetofit.usuario.Usuario;
import com.projetofit.usuario.UsuarioRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SementeExerciciosIT extends TesteIntegracao {

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    AlunoRepository alunoRepository;

    @Autowired
    ExercicioRepository exercicioRepository;

    @Autowired
    SementeExercicios sementeExercicios;

    @Autowired
    SementeParaPersonaisExistentes sementeParaPersonaisExistentes;

    @BeforeEach
    void limpar() {
        exercicioRepository.deleteAll();
        alunoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void primeiroAcessoDoPersonalSemeiaOsExercicios() throws Exception {
        UUID sub = UUID.randomUUID();

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + TokenDeTeste.gerar(sub, "personal@teste.com")))
                .andExpect(status().isOk());

        List<Exercicio> exercicios = exercicioRepository.findAll();
        assertThat(exercicioRepository.countByPersonalId(sub)).isEqualTo(44);
        assertThat(exercicios).extracting(Exercicio::getGrupoMuscular).containsAll(List.of(GrupoMuscular.values()));
        assertThat(exercicios).anyMatch(e -> e.getNome().equals("Supino reto com barra")
                && e.getGrupoMuscular() == GrupoMuscular.PEITO);
        assertThat(exercicios).allMatch(Exercicio::isAtivo);
    }

    @Test
    void segundoAcessoNaoDuplicaSemente() throws Exception {
        UUID sub = UUID.randomUUID();
        String token = TokenDeTeste.gerar(sub, "personal@teste.com");

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        assertThat(exercicioRepository.countByPersonalId(sub)).isEqualTo(44);
    }

    @Test
    void personalExistenteSemExerciciosRecebeSementeNaInicializacao() throws Exception {
        UUID id = UUID.randomUUID();
        usuarioRepository.save(new Usuario(id, "personal", "personal@teste.com", Papel.PERSONAL));

        sementeParaPersonaisExistentes.run(null);

        assertThat(exercicioRepository.countByPersonalId(id)).isEqualTo(44);
    }

    @Test
    void semearSeVazioNaoMexeEmCatalogoExistente() {
        UUID id = UUID.randomUUID();
        usuarioRepository.save(new Usuario(id, "personal", "personal@teste.com", Papel.PERSONAL));
        exercicioRepository.save(new Exercicio(id, "Meu exercício", GrupoMuscular.PEITO, null, null));

        sementeExercicios.semearSeVazio(id);

        assertThat(exercicioRepository.countByPersonalId(id)).isEqualTo(1);
    }
}
