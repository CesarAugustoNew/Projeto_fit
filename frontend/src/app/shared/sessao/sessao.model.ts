import { Ficha } from '../../personal/fichas/ficha.model';

export interface Pagina<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface FichaAtiva {
  ficha: Ficha;
  proximaDivisaoId: string | null;
}

export interface SerieRequest {
  itemDivisaoId: string | null;
  exercicioId: string;
  numeroSerie: number;
  repeticoes: number;
  cargaKg: number | null;
}

export interface SessaoRequest {
  clientId: string;
  divisaoId: string | null;
  divisaoNome: string;
  iniciadaEm: string;
  finalizadaEm: string;
  observacao: string | null;
  series: SerieRequest[];
}

export interface SerieExecutada {
  exercicioId: string;
  exercicioNome: string;
  itemDivisaoId: string | null;
  ordem: number;
  numeroSerie: number;
  repeticoes: number;
  cargaKg: number | null;
}

export interface Sessao {
  id: string;
  clientId: string;
  alunoId: string;
  divisaoId: string | null;
  divisaoNome: string;
  iniciadaEm: string;
  finalizadaEm: string;
  observacao: string | null;
  series: SerieExecutada[];
}

export interface SessaoResumo {
  id: string;
  divisaoNome: string;
  iniciadaEm: string;
  finalizadaEm: string;
  quantidadeExercicios: number;
  quantidadeSeries: number;
}

const doisDigitos = (n: number): string => String(n).padStart(2, '0');

/** Instante ISO-8601 → `dd/MM/yyyy HH:mm` no fuso do aparelho. */
export function formatarDataHora(iso: string): string {
  const d = new Date(iso);
  return (
    `${doisDigitos(d.getDate())}/${doisDigitos(d.getMonth() + 1)}/${d.getFullYear()} ` +
    `${doisDigitos(d.getHours())}:${doisDigitos(d.getMinutes())}`
  );
}

/** Duração em minutos, arredondada, com mínimo de 1. */
export function duracaoMinutos(inicio: string, fim: string): number {
  const minutos = (new Date(fim).getTime() - new Date(inicio).getTime()) / 60000;
  return Math.max(1, Math.round(minutos));
}
