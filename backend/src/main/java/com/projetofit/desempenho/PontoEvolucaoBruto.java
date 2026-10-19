package com.projetofit.desempenho;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Linha da consulta de evolução, antes de converter o instante para a data no fuso do personal. */
public record PontoEvolucaoBruto(UUID sessaoId, Instant finalizadaEm, BigDecimal cargaMaxima, BigDecimal volume) {
}
