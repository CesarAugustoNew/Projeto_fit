export type GrupoMuscular =
  | 'PEITO'
  | 'COSTAS'
  | 'OMBROS'
  | 'BICEPS'
  | 'TRICEPS'
  | 'ANTEBRACO'
  | 'QUADRICEPS'
  | 'POSTERIORES'
  | 'GLUTEOS'
  | 'PANTURRILHA'
  | 'ABDOMEN'
  | 'CARDIO'
  | 'CORPO_INTEIRO';

export const GRUPOS: GrupoMuscular[] = [
  'PEITO',
  'COSTAS',
  'OMBROS',
  'BICEPS',
  'TRICEPS',
  'ANTEBRACO',
  'QUADRICEPS',
  'POSTERIORES',
  'GLUTEOS',
  'PANTURRILHA',
  'ABDOMEN',
  'CARDIO',
  'CORPO_INTEIRO',
];

export const ROTULO_GRUPO: Record<GrupoMuscular, string> = {
  PEITO: 'Peito',
  COSTAS: 'Costas',
  OMBROS: 'Ombros',
  BICEPS: 'Bíceps',
  TRICEPS: 'Tríceps',
  ANTEBRACO: 'Antebraço',
  QUADRICEPS: 'Quadríceps',
  POSTERIORES: 'Posteriores de coxa',
  GLUTEOS: 'Glúteos',
  PANTURRILHA: 'Panturrilha',
  ABDOMEN: 'Abdômen',
  CARDIO: 'Cardio',
  CORPO_INTEIRO: 'Corpo inteiro',
};

export interface Exercicio {
  id: string;
  nome: string;
  grupoMuscular: GrupoMuscular;
  videoUrl: string | null;
  observacao: string | null;
  ativo: boolean;
}

export interface NovoExercicio {
  nome: string;
  grupoMuscular: GrupoMuscular;
  videoUrl: string | null;
  observacao: string | null;
}

export interface AtualizarExercicio extends NovoExercicio {
  ativo: boolean;
}

/** Devolve a URL só se for http(s); evita esquemas como javascript: em links. */
export function linkSeguro(url: string | null): string | null {
  return url !== null && /^https?:\/\//i.test(url) ? url : null;
}
