package com.projetofit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class MigracoesIT extends TesteIntegracao {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void tabelaUsuarioExisteComRlsAtivo() {
        Boolean rls = jdbc.queryForObject(
                "select relrowsecurity from pg_class where relname = 'usuario'", Boolean.class);
        assertThat(rls).isTrue();
    }

    @Test
    void emailEhUnicoSemDiferenciarMaiusculas() {
        jdbc.update("delete from usuario");
        jdbc.update("insert into usuario (id, nome, email, papel) values (gen_random_uuid(), 'A', 'Aluno@Teste.com', 'ALUNO')");
        assertThatThrownBy(() -> jdbc.update(
                "insert into usuario (id, nome, email, papel) values (gen_random_uuid(), 'B', 'aluno@teste.com', 'ALUNO')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("delete from usuario");
    }
}
