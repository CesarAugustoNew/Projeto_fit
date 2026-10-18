import { Routes } from '@angular/router';
import { ItemNav } from '../layout/item-nav';
import { Shell } from '../layout/shell';
import { Treino } from './treino/treino';

const itens: ItemNav[] = [
  { rota: '/aluno/treino', rotulo: 'Treino', icone: 'fitness_center' },
  { rota: '/aluno/historico', rotulo: 'Histórico', icone: 'history' },
];

export const alunoRoutes: Routes = [
  {
    path: '',
    component: Shell,
    data: { itens, titulo: 'Projeto Fit' },
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'treino' },
      { path: 'treino', component: Treino },
      {
        path: 'treino/execucao',
        loadComponent: () => import('./execucao/execucao').then((m) => m.Execucao),
      },
      {
        path: 'historico',
        loadComponent: () => import('./historico/historico').then((m) => m.Historico),
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
