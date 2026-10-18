export type StatusAluno = 'CONVIDADO' | 'ATIVO' | 'INATIVO';

export interface Aluno {
  id: string;
  nome: string;
  email: string;
  telefone: string | null;
  dataNascimento: string | null;
  objetivo: string | null;
  observacoes: string | null;
  status: StatusAluno;
  criadoEm: string;
}

export interface NovoAluno {
  nome: string;
  email: string;
  telefone?: string | null;
  dataNascimento?: string | null;
  objetivo?: string | null;
  observacoes?: string | null;
}

export type DadosAluno = Omit<NovoAluno, 'email'>;

export const ROTULO_STATUS: Record<StatusAluno, string> = {
  CONVIDADO: 'Convite pendente',
  ATIVO: 'Ativo',
  INATIVO: 'Inativo',
};
