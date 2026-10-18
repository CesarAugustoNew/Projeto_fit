package com.projetofit.aluno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.projetofit.TesteIntegracao;
import com.projetofit.ficha.FichaRepository;
import com.projetofit.sessao.SessaoRepository;
import com.projetofit.seguranca.TokenDeTeste;
import com.projetofit.exercicio.ExercicioRepository;
import com.projetofit.usuario.Papel;
import com.projetofit.usuario.Usuario;
import com.projetofit.usuario.UsuarioRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AcessoAlunoIT extends TesteIntegracao {

    private static final UUID PERSONAL_ID = UUID.randomUUID();

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    AlunoRepository alunoRepository;

    @Autowired
    ExercicioRepository exercicioRepository;

    @Autowired
    FichaRepository fichaRepository;

    @Autowired
    SessaoRepository sessaoRepository;

    @BeforeEach
    void preparar() {
        sessaoRepository.deleteAll();
        fichaRepository.deleteAll();
        exercicioRepository.deleteAll();
        alunoRepository.deleteAll();
        usuarioRepository.deleteAll();
        usuarioRepository.save(new Usuario(PERSONAL_ID, "personal", "personal@teste.com", Papel.PERSONAL));
    }

    @Test
    void primeiroAcessoDoConvidadoAtiva() throws Exception {
        UUID alunoUsuarioId = UUID.randomUUID();
        Aluno aluno = gravarAluno(alunoUsuarioId, StatusAluno.CONVIDADO);

        mvc.perform(get("/api/me").header("Authorization", bearerAluno(alunoUsuarioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("ALUNO"));
        Aluno atualizado = alunoRepository.findById(aluno.getId()).orElseThrow();
        assertThat(atualizado.getStatus()).isEqualTo(StatusAluno.ATIVO);
        assertThat(atualizado.getPrimeiroAcessoEm()).isNotNull();
    }

    @Test
    void usuarioAlunoSemCadastroDeAlunoRecebe403() throws Exception {
        UUID alunoUsuarioId = UUID.randomUUID();
        usuarioRepository.save(new Usuario(alunoUsuarioId, "Ana", "ana@teste.com", Papel.ALUNO));

        mvc.perform(get("/api/me").header("Authorization", bearerAluno(alunoUsuarioId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso negado"));
    }

    @Test
    void alunoInativoRecebe403EReativadoVoltaAEntrar() throws Exception {
        UUID alunoUsuarioId = UUID.randomUUID();
        Aluno aluno = gravarAluno(alunoUsuarioId, StatusAluno.INATIVO);

        mvc.perform(get("/api/me").header("Authorization", bearerAluno(alunoUsuarioId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso negado"));
        assertThat(alunoRepository.findById(aluno.getId()).orElseThrow().getStatus()).isEqualTo(StatusAluno.INATIVO);

        Aluno reativado = alunoRepository.findById(aluno.getId()).orElseThrow();
        reativado.alterarStatus(StatusAluno.ATIVO);
        alunoRepository.save(reativado);

        mvc.perform(get("/api/me").header("Authorization", bearerAluno(alunoUsuarioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("ALUNO"));
    }

    private Aluno gravarAluno(UUID usuarioId, StatusAluno status) {
        Usuario usuario = usuarioRepository.save(new Usuario(usuarioId, "Ana", "ana@teste.com", Papel.ALUNO));
        Aluno aluno = new Aluno(usuario, PERSONAL_ID, null, null, null, null);
        aluno.alterarStatus(status);
        return alunoRepository.save(aluno);
    }

    private static String bearerAluno(UUID usuarioId) {
        return "Bearer " + TokenDeTeste.gerar(usuarioId, "ana@teste.com");
    }
}
