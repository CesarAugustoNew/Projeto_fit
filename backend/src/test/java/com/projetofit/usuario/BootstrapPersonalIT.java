package com.projetofit.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.projetofit.TesteIntegracao;
import com.projetofit.aluno.AlunoRepository;
import com.projetofit.exercicio.ExercicioRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

class BootstrapPersonalIT extends TesteIntegracao {

    private static volatile boolean ouvinteFalha;

    @Autowired
    BootstrapPersonal bootstrapPersonal;

    @Autowired
    OuvinteDeTeste ouvinte;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    AlunoRepository alunoRepository;

    @Autowired
    ExercicioRepository exercicioRepository;

    @BeforeEach
    void limpar() {
        exercicioRepository.deleteAll();
        alunoRepository.deleteAll();
        usuarioRepository.deleteAll();
        ouvinte.recebidos.clear();
    }

    @AfterEach
    void desligarFalha() {
        ouvinteFalha = false;
    }

    @Test
    void criarGravaUsuarioEAvisaOuvintes() {
        UUID id = UUID.randomUUID();

        bootstrapPersonal.criar(id, "personal", "personal@teste.com");

        assertThat(usuarioRepository.findById(id))
                .get()
                .extracting(Usuario::getPapel)
                .isEqualTo(Papel.PERSONAL);
        assertThat(ouvinte.recebidos).containsExactly(id);
    }

    @Test
    void falhaNoOuvinteDesfazOUsuario() {
        UUID id = UUID.randomUUID();
        ouvinteFalha = true;

        assertThatThrownBy(() -> bootstrapPersonal.criar(id, "personal", "personal@teste.com"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(usuarioRepository.findById(id)).isEmpty();
    }

    static class OuvinteDeTeste implements AoCriarPersonal {

        final List<UUID> recebidos = new CopyOnWriteArrayList<>();

        @Override
        public void aoCriarPersonal(UUID personalId) {
            recebidos.add(personalId);
            if (ouvinteFalha) {
                throw new IllegalStateException("falha simulada no ouvinte");
            }
        }
    }

    @TestConfiguration
    static class Config {

        @Bean
        OuvinteDeTeste ouvinteDeTeste() {
            return new OuvinteDeTeste();
        }
    }
}
