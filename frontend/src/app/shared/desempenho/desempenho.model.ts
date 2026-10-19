import { GrupoMuscular } from '../../personal/exercicios/exercicio.model';

export interface PontoEvolucao {
  data: string;
  sessaoId: string;
  cargaMaxima: number | null;
  volume: number;
}

export interface ExercicioExecutado {
  id: string;
  nome: string;
  grupoMuscular: GrupoMuscular;
}

export interface SemanaAderencia {
  inicio: string;
  fim: string;
  realizados: number;
  planejado: number;
  percentual: number | null;
}

export interface DashboardAluno {
  alunoId: string;
  nome: string;
  realizadosSemana: number;
  planejadoSemana: number;
  diasSemTreinar: number | null;
  fichaAtivaId: string | null;
  fichaVencida: boolean;
  sumiu: boolean;
}

/** Quem tem o desempenho consultado: o próprio aluno logado ou, pelo personal, um aluno dele. */
export type AlvoDesempenho = { tipo: 'aluno-logado' } | { tipo: 'aluno'; alunoId: string };

/** Média dos percentuais não nulos, arredondada; `null` se nenhuma semana tem percentual. */
export function aderenciaMedia(semanas: SemanaAderencia[]): number | null {
  const valores = semanas.map((s) => s.percentual).filter((p): p is number => p !== null);
  if (valores.length === 0) return null;
  return Math.round(valores.reduce((soma, p) => soma + p, 0) / valores.length);
}
