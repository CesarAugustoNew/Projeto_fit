export type Papel = 'PERSONAL' | 'ALUNO';

export interface Usuario {
  id: string;
  nome: string;
  email: string;
  papel: Papel;
}

export function rotaInicial(papel: Papel): string {
  return papel === 'PERSONAL' ? '/personal/dashboard' : '/aluno/treino';
}
