package com.projetofit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class MigracoesIT extends TesteIntegracao {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void todaTabelaPublicTemRlsAtivoESemPoliticas() {
        List<String> semRls = jdbc.queryForList(
                "select c.relname from pg_class c join pg_namespace n on n.oid = c.relnamespace "
                        + "where n.nspname = 'public' and c.relkind = 'r' and c.relname <> 'flyway_schema_history' "
                        + "and not c.relrowsecurity", String.class);
        assertThat(semRls).isEmpty();
        Integer politicas = jdbc.queryForObject(
                "select count(*) from pg_policies where schemaname = 'public'", Integer.class);
        assertThat(politicas).isZero();
        assertThat(jdbc.queryForList("select tablename from pg_tables where schemaname = 'public'", String.class))
                .contains("usuario", "aluno");
    }

    @Test
    void statusDeAlunoInvalidoEhRecusado() {
        jdbc.update("delete from aluno");
        jdbc.update("delete from usuario");
        jdbc.update("insert into usuario (id, nome, email, papel) values ('00000000-0000-0000-0000-000000000001', 'P', 'p@teste.com', 'PERSONAL')");
        jdbc.update("insert into usuario (id, nome, email, papel) values ('00000000-0000-0000-0000-000000000002', 'A', 'a@teste.com', 'ALUNO')");
        assertThatThrownBy(() -> jdbc.update(
                "insert into aluno (id, usuario_id, personal_id, status) values (gen_random_uuid(), "
                        + "'00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'BLOQUEADO')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("delete from usuario");
    }

    @Test
    void emailEhUnicoSemDiferenciarMaiusculas() {
        jdbc.update("delete from aluno");
        jdbc.update("delete from usuario");
        jdbc.update("insert into usuario (id, nome, email, papel) values (gen_random_uuid(), 'A', 'Aluno@Teste.com', 'ALUNO')");
        assertThatThrownBy(() -> jdbc.update(
                "insert into usuario (id, nome, email, papel) values (gen_random_uuid(), 'B', 'aluno@teste.com', 'ALUNO')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("delete from usuario");
    }
}
