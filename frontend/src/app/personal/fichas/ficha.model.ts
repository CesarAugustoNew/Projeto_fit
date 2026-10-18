import { GrupoMuscular } from '../exercicios/exercicio.model';

export type StatusFicha = 'ATIVA' | 'ENCERRADA';

export interface ItemDivisao {
  id: string;
  exercicioId: string;
  exercicioNome: string;
  grupoMuscular: GrupoMuscular;
  ordem: number;
  series: number;
  repeticoes: string;
  cargaKg: number | null;
  descansoSeg: number | null;
  observacao: string | null;
}

export interface Divisao {
  id: string;
  nome: string;
  ordem: number;
  itens: ItemDivisao[];
}

export interface Ficha {
  id: string;
  alunoId: string;
  nome: string;
  objetivo: string | null;
  frequenciaSemanal: number;
  dataInicio: string;
  dataFim: string | null;
  status: StatusFicha;
  vencida: boolean;
  divisoes: Divisao[];
}

export interface FichaResumo {
  id: string;
  nome: string;
  status: StatusFicha;
  dataInicio: string;
  dataFim: string | null;
  frequenciaSemanal: number;
  quantidadeDivisoes: number;
  vencida: boolean;
}

export interface ItemRequest {
  id: string | null;
  exercicioId: string;
  series: number;
  repeticoes: string;
  cargaKg: number | null;
  descansoSeg: number | null;
  observacao: string | null;
}

export interface DivisaoRequest {
  id: string | null;
  nome: string;
  itens: ItemRequest[];
}

export interface FichaRequest {
  nome: string;
  objetivo: string | null;
  frequenciaSemanal: number;
  dataInicio: string;
  dataFim: string | null;
  divisoes: DivisaoRequest[];
}

/** yyyy-MM-dd -> dd/MM/yyyy, sem passar por Date (evita deslocamento de fuso). */
export function formatarData(data: string | null): string {
  if (!data) return '—';
  const [ano, mes, dia] = data.split('-');
  return `${dia}/${mes}/${ano}`;
}

/** Data de hoje (yyyy-MM-dd) no fuso do aparelho. */
export function hojeLocal(): string {
  const agora = new Date();
  const mes = String(agora.getMonth() + 1).padStart(2, '0');
  const dia = String(agora.getDate()).padStart(2, '0');
  return `${agora.getFullYear()}-${mes}-${dia}`;
}
