package com.projetofit.desempenho;

import static org.assertj.core.api.Assertions.assertThat;

import com.projetofit.desempenho.CalculoAderencia.PeriodoFicha;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculoAderenciaTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 14);

    private static Instant em(String iso) {
        return Instant.parse(iso);
    }

    private static PeriodoFicha fichaDesde(String iso, int freq) {
        return new PeriodoFicha(em(iso), null, freq);
    }

    @Test
    void oitoSemanasTerminandoNaAtual() {
        List<SemanaAderenciaDTO> semanas = CalculoAderencia.calcular(HOJE, 8, SP, List.of(), List.of());

        assertThat(semanas).hasSize(8);
        SemanaAderenciaDTO ultima = semanas.get(7);
        assertThat(ultima.inicio()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(ultima.fim()).isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(semanas.get(0).inicio()).isEqualTo(LocalDate.of(2026, 8, 24));
    }

    @Test
    void contaRealizadosEPercentualLimitadoA100() {
        List<PeriodoFicha> fichas = List.of(fichaDesde("2026-01-01T00:00:00Z", 3));
        List<Instant> quatro = List.of(em("2026-10-12T15:00:00Z"), em("2026-10-13T15:00:00Z"),
                em("2026-10-14T15:00:00Z"), em("2026-10-15T15:00:00Z"));
        List<Instant> dois = quatro.subList(0, 2);

        SemanaAderenciaDTO cheia = CalculoAderencia.calcular(HOJE, 1, SP, quatro, fichas).get(0);
        SemanaAderenciaDTO parcial = CalculoAderencia.calcular(HOJE, 1, SP, dois, fichas).get(0);

        assertThat(cheia.realizados()).isEqualTo(4);
        assertThat(cheia.planejado()).isEqualTo(3);
        assertThat(cheia.percentual()).isEqualTo(100);
        assertThat(parcial.percentual()).isEqualTo(67);
    }

    @Test
    void treinoDomingoNoiteContaNaSemanaCerta() {
        // domingo 11 as 22h em Sao Paulo
        List<Instant> fin = List.of(em("2026-10-12T01:00:00Z"));

        List<SemanaAderenciaDTO> semanas = CalculoAderencia.calcular(HOJE, 2, SP, fin, List.of());

        assertThat(semanas.get(0).inicio()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(semanas.get(0).realizados()).isEqualTo(1);
        assertThat(semanas.get(1).realizados()).isZero();
    }

    @Test
    void semanaSemFichaTemPercentualNulo() {
        SemanaAderenciaDTO semana = CalculoAderencia.calcular(HOJE, 1, SP,
                List.of(em("2026-10-13T15:00:00Z")), List.of()).get(0);

        assertThat(semana.planejado()).isZero();
        assertThat(semana.percentual()).isNull();
        assertThat(semana.realizados()).isEqualTo(1);
    }

    @Test
    void fichaTrocadaNaSemanaUsaAMaisRecente() {
        PeriodoFicha a = new PeriodoFicha(em("2026-01-01T00:00:00Z"), em("2026-10-13T13:00:00Z"), 3);
        PeriodoFicha b = new PeriodoFicha(em("2026-10-13T13:00:00Z"), null, 5);

        SemanaAderenciaDTO semana = CalculoAderencia.calcular(HOJE, 1, SP, List.of(), List.of(a, b)).get(0);

        assertThat(semana.planejado()).isEqualTo(5);
    }

    @Test
    void fichaEncerradaAntesDaSemanaNaoConta() {
        PeriodoFicha antiga = new PeriodoFicha(em("2026-01-01T00:00:00Z"), em("2026-10-05T02:00:00Z"), 4);

        List<SemanaAderenciaDTO> semanas = CalculoAderencia.calcular(HOJE, 2, SP, List.of(), List.of(antiga));

        // 05/10 02:00Z = domingo 04/10 23:00 em SP: ja fora da semana que comeca em 05/10
        assertThat(semanas.get(0).planejado()).isZero();
        assertThat(semanas.get(1).planejado()).isZero();
        assertThat(semanas.get(1).percentual()).isNull();
    }
}
