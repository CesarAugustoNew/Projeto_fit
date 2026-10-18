package com.projetofit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
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
                .contains("usuario", "aluno", "exercicio", "ficha", "divisao", "item_divisao", "sessao", "serie_executada");
    }

    @Test
    void statusDeAlunoInvalidoEhRecusado() {
        jdbc.update("delete from sessao");
        jdbc.update("delete from ficha");
        jdbc.update("delete from exercicio");
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
        jdbc.update("delete from sessao");
        jdbc.update("delete from ficha");
        jdbc.update("delete from exercicio");
        jdbc.update("delete from aluno");
        jdbc.update("delete from usuario");
        jdbc.update("insert into usuario (id, nome, email, papel) values (gen_random_uuid(), 'A', 'Aluno@Teste.com', 'ALUNO')");
        assertThatThrownBy(() -> jdbc.update(
                "insert into usuario (id, nome, email, papel) values (gen_random_uuid(), 'B', 'aluno@teste.com', 'ALUNO')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("delete from usuario");
    }

    @Test
    void nomeDeExercicioEhUnicoPorPersonalSemDiferenciarMaiusculas() {
        jdbc.update("delete from sessao");
        jdbc.update("delete from ficha");
        jdbc.update("delete from exercicio");
        jdbc.update("delete from aluno");
        jdbc.update("delete from usuario");
        jdbc.update("insert into usuario (id, nome, email, papel) values ('00000000-0000-0000-0000-000000000001', 'P1', 'p1@teste.com', 'PERSONAL')");
        jdbc.update("insert into usuario (id, nome, email, papel) values ('00000000-0000-0000-0000-000000000002', 'P2', 'p2@teste.com', 'PERSONAL')");
        jdbc.update("insert into exercicio (personal_id, nome, grupo_muscular) values "
                + "('00000000-0000-0000-0000-000000000001', 'Supino', 'PEITO')");
        assertThatThrownBy(() -> jdbc.update("insert into exercicio (personal_id, nome, grupo_muscular) values "
                + "('00000000-0000-0000-0000-000000000001', 'supino', 'PEITO')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("insert into exercicio (personal_id, nome, grupo_muscular) values "
                + "('00000000-0000-0000-0000-000000000002', 'supino', 'PEITO')");
        jdbc.update("delete from sessao");
        jdbc.update("delete from ficha");
        jdbc.update("delete from exercicio");
        jdbc.update("delete from usuario");
    }

    @Test
    void grupoMuscularInvalidoEhRecusado() {
        jdbc.update("delete from sessao");
        jdbc.update("delete from ficha");
        jdbc.update("delete from exercicio");
        jdbc.update("delete from aluno");
        jdbc.update("delete from usuario");
        jdbc.update("insert into usuario (id, nome, email, papel) values ('00000000-0000-0000-0000-000000000001', 'P', 'p@teste.com', 'PERSONAL')");
        assertThatThrownBy(() -> jdbc.update("insert into exercicio (personal_id, nome, grupo_muscular) values "
                + "('00000000-0000-0000-0000-000000000001', 'Pescoco', 'PESCOCO')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("delete from usuario");
    }

    private static final String PERSONAL = "00000000-0000-0000-0000-000000000001";
    private static final String ALUNO_USUARIO = "00000000-0000-0000-0000-000000000002";
    private static final String ALUNO = "00000000-0000-0000-0000-000000000003";

    private void prepararAluno() {
        jdbc.update("delete from sessao");
        jdbc.update("delete from ficha");
        jdbc.update("delete from exercicio");
        jdbc.update("delete from aluno");
        jdbc.update("delete from usuario");
        jdbc.update("insert into usuario (id, nome, email, papel) values ('" + PERSONAL + "', 'P', 'p@teste.com', 'PERSONAL')");
        jdbc.update("insert into usuario (id, nome, email, papel) values ('" + ALUNO_USUARIO + "', 'A', 'a@teste.com', 'ALUNO')");
        jdbc.update("insert into aluno (id, usuario_id, personal_id, status) values ('" + ALUNO + "', '"
                + ALUNO_USUARIO + "', '" + PERSONAL + "', 'ATIVO')");
    }

    private void inserirFicha(String status) {
        jdbc.update("insert into ficha (aluno_id, nome, frequencia_semanal, data_inicio, status) values ('"
                + ALUNO + "', 'F', 3, current_date, '" + status + "')");
    }

    private void limpar() {
        jdbc.update("delete from sessao");
        jdbc.update("delete from ficha");
        jdbc.update("delete from exercicio");
        jdbc.update("delete from aluno");
        jdbc.update("delete from usuario");
    }

    @Test
    void soUmaFichaAtivaPorAluno() {
        prepararAluno();
        inserirFicha("ATIVA");
        assertThatThrownBy(() -> inserirFicha("ATIVA")).isInstanceOf(DataIntegrityViolationException.class);
        inserirFicha("ENCERRADA");
        limpar();
    }

    @Test
    void apagarFichaApagaDivisoesEItens() {
        prepararAluno();
        inserirFicha("ATIVA");
        jdbc.update("insert into exercicio (id, personal_id, nome, grupo_muscular) values "
                + "('00000000-0000-0000-0000-0000000000e1', '" + PERSONAL + "', 'Supino', 'PEITO')");
        jdbc.update("insert into divisao (id, ficha_id, nome, ordem) select "
                + "'00000000-0000-0000-0000-0000000000d1', id, 'A', 1 from ficha");
        jdbc.update("insert into item_divisao (divisao_id, exercicio_id, ordem, series, repeticoes) values "
                + "('00000000-0000-0000-0000-0000000000d1', '00000000-0000-0000-0000-0000000000e1', 1, 3, '10')");
        jdbc.update("delete from sessao");
        jdbc.update("delete from ficha");
        assertThat(jdbc.queryForObject("select count(*) from divisao", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from item_divisao", Integer.class)).isZero();
        limpar();
    }

    @Test
    void seriesForaDoLimiteSaoRecusadas() {
        prepararAluno();
        inserirFicha("ATIVA");
        jdbc.update("insert into exercicio (id, personal_id, nome, grupo_muscular) values "
                + "('00000000-0000-0000-0000-0000000000e1', '" + PERSONAL + "', 'Supino', 'PEITO')");
        jdbc.update("insert into divisao (id, ficha_id, nome, ordem) select "
                + "'00000000-0000-0000-0000-0000000000d1', id, 'A', 1 from ficha");
        for (int series : new int[] {0, 21}) {
            assertThatThrownBy(() -> jdbc.update(
                    "insert into item_divisao (divisao_id, exercicio_id, ordem, series, repeticoes) values "
                            + "('00000000-0000-0000-0000-0000000000d1', '00000000-0000-0000-0000-0000000000e1', 1, "
                            + series + ", '10')"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
        limpar();
    }

    private static final String DIVISAO = "00000000-0000-0000-0000-0000000000d1";
    private static final String EXERCICIO = "00000000-0000-0000-0000-0000000000e1";
    private static final String ITEM = "00000000-0000-0000-0000-0000000000a1";
    private static final String SESSAO = "00000000-0000-0000-0000-0000000000b1";

    private void prepararSessao() {
        prepararAluno();
        inserirFicha("ATIVA");
        jdbc.update("insert into exercicio (id, personal_id, nome, grupo_muscular) values ('" + EXERCICIO + "', '"
                + PERSONAL + "', 'Supino', 'PEITO')");
        jdbc.update("insert into divisao (id, ficha_id, nome, ordem) select '" + DIVISAO + "', id, 'A', 1 from ficha");
        jdbc.update("insert into item_divisao (id, divisao_id, exercicio_id, ordem, series, repeticoes) values ('"
                + ITEM + "', '" + DIVISAO + "', '" + EXERCICIO + "', 1, 3, '10')");
        inserirSessao(SESSAO, "00000000-0000-0000-0000-0000000000c1");
    }

    private void inserirSessao(String id, String clientId) {
        jdbc.update("insert into sessao (id, client_id, aluno_id, divisao_id, divisao_nome, iniciada_em, "
                + "finalizada_em) values ('" + id + "', '" + clientId + "', '" + ALUNO + "', '" + DIVISAO
                + "', 'A', now(), now())");
    }

    private void inserirSerie(int repeticoes) {
        jdbc.update("insert into serie_executada (sessao_id, exercicio_id, item_divisao_id, ordem, numero_serie, "
                + "repeticoes) values ('" + SESSAO + "', '" + EXERCICIO + "', '" + ITEM + "', 1, 1, " + repeticoes
                + ")");
    }

    @Test
    void clientIdEhUnico() {
        prepararSessao();
        assertThatThrownBy(() -> inserirSessao("00000000-0000-0000-0000-0000000000b2",
                "00000000-0000-0000-0000-0000000000c1")).isInstanceOf(DataIntegrityViolationException.class);
        limpar();
    }

    @Test
    void apagarDivisaoDeixaSessaoComDivisaoNula() {
        prepararSessao();
        jdbc.update("delete from divisao where id = '" + DIVISAO + "'");
        assertThat(jdbc.queryForObject("select divisao_id from sessao where id = '" + SESSAO + "'", UUID.class))
                .isNull();
        assertThat(jdbc.queryForObject("select divisao_nome from sessao where id = '" + SESSAO + "'", String.class))
                .isEqualTo("A");
        limpar();
    }

    @Test
    void apagarItemDeixaSerieComItemNulo() {
        prepararSessao();
        inserirSerie(10);
        jdbc.update("delete from item_divisao where id = '" + ITEM + "'");
        assertThat(jdbc.queryForObject("select item_divisao_id from serie_executada", UUID.class)).isNull();
        limpar();
    }

    @Test
    void repeticoesNegativasSaoRecusadas() {
        prepararSessao();
        assertThatThrownBy(() -> inserirSerie(-1)).isInstanceOf(DataIntegrityViolationException.class);
        limpar();
    }
}
