import { SessaoRequest } from '../../shared/sessao/sessao.model';

export interface SerieEmAndamento {
  numero: number;
  cargaKg: number | null;
  repeticoes: number | null;
  concluida: boolean;
}

export interface ExercicioEmAndamento {
  itemDivisaoId: string;
  exercicioId: string;
  exercicioNome: string;
  prescricao: {
    series: number;
    repeticoes: string;
    cargaKg: number | null;
    descansoSeg: number | null;
    observacao: string | null;
  };
  series: SerieEmAndamento[];
}

export interface SessaoEmAndamento {
  clientId: string;
  fichaId: string;
  divisaoId: string;
  divisaoNome: string;
  iniciadaEm: string;
  observacao: string;
  exercicios: ExercicioEmAndamento[];
}

export interface EnvioComErro {
  sessao: SessaoRequest;
  mensagem: string;
}

/** Repetições pré-preenchidas a partir da prescrição: `'10'` → 10; `'8-12'` → 8. */
export function repeticoesSugeridas(prescricao: string): number {
  return parseInt(prescricao, 10);
}
