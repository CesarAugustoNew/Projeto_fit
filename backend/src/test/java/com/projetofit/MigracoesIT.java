package com.projetofit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
}
