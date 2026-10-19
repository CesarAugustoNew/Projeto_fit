import { alunoRoutes } from './aluno.routes';
import { Execucao } from './execucao/execucao';

describe('rotas do aluno', () => {
  it('treino/execucao é carregada junto com o app (abre sem sinal)', () => {
    const rota = alunoRoutes[0].children?.find((r) => r.path === 'treino/execucao');
    expect(rota?.component).toBe(Execucao);
    expect(rota?.loadComponent).toBeUndefined();
  });
});
