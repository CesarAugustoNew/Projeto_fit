import { Routes } from '@angular/router';
import { ItemNav } from '../layout/item-nav';
import { Shell } from '../layout/shell';
import { Treino } from './treino/treino';

const itens: ItemNav[] = [{ rota: '/aluno/treino', rotulo: 'Treino', icone: 'fitness_center' }];

export const alunoRoutes: Routes = [
  {
    path: '',
    component: Shell,
    data: { itens, titulo: 'Projeto Fit' },
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'treino' },
      { path: 'treino', component: Treino },
    ],
  },
];
