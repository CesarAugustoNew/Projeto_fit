package com.projetofit.desempenho;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Aderência semanal (segunda a domingo): treinos realizados contra a frequência da ficha vigente. */
public final class CalculoAderencia {

    /** Período em que uma ficha esteve ativa; {@code ativaAte == null} significa ainda ativa. */
    public record PeriodoFicha(Instant ativaDesde, Instant ativaAte, int frequenciaSemanal) {
    }

    private CalculoAderencia() {
    }

    public static List<SemanaAderenciaDTO> calcular(LocalDate hoje, int semanas, ZoneId zona,
                                                    List<Instant> finalizacoes, List<PeriodoFicha> fichas) {
        LocalDate segundaAtual = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<SemanaAderenciaDTO> resultado = new ArrayList<>(semanas);
        for (int i = semanas - 1; i >= 0; i--) {
            LocalDate inicio = segundaAtual.minusWeeks(i);
            LocalDate proxima = inicio.plusWeeks(1);
            Instant de = inicio.atStartOfDay(zona).toInstant();
            Instant ate = proxima.atStartOfDay(zona).toInstant();
            int realizados = (int) finalizacoes.stream()
                    .filter(f -> !f.isBefore(de) && f.isBefore(ate))
                    .count();
            int planejado = fichas.stream()
                    .filter(f -> f.ativaDesde().isBefore(ate) && (f.ativaAte() == null || !f.ativaAte().isBefore(de)))
                    .max(Comparator.comparing(PeriodoFicha::ativaDesde))
                    .map(PeriodoFicha::frequenciaSemanal)
                    .orElse(0);
            Integer percentual = planejado == 0 ? null
                    : (int) Math.min(100, Math.round(100.0 * realizados / planejado));
            resultado.add(new SemanaAderenciaDTO(inicio, proxima.minusDays(1), realizados, planejado, percentual));
        }
        return resultado;
    }
}
