import { Routes } from '@angular/router';
import { ItemNav } from '../layout/item-nav';
import { Shell } from '../layout/shell';
import { Execucao } from './execucao/execucao';
import { Treino } from './treino/treino';

const itens: ItemNav[] = [
  { rota: '/aluno/treino', rotulo: 'Treino', icone: 'fitness_center' },
  { rota: '/aluno/historico', rotulo: 'Histórico', icone: 'history' },
  { rota: '/aluno/evolucao', rotulo: 'Evolução', icone: 'trending_up' },
];

export const alunoRoutes: Routes = [
  {
    path: '',
    component: Shell,
    data: { itens, titulo: 'Projeto Fit' },
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'treino' },
      { path: 'treino', component: Treino },
      // Eager: o aluno pode tocar "Começar treino" já sem sinal, quando um chunk lazy não baixaria.
      { path: 'treino/execucao', component: Execucao },
      {
        path: 'historico',
        loadComponent: () => import('./historico/historico').then((m) => m.Historico),
      },
      {
        path: 'evolucao',
        loadComponent: () => import('./evolucao/evolucao').then((m) => m.Evolucao),
      },
      {
        path: 'historico/:id',
        loadComponent: () =>
          import('../shared/sessao/detalhe-sessao/detalhe-sessao').then((m) => m.DetalheSessao),
        data: { voltarPara: '/aluno/historico' },
      },
    ],
  },
];
