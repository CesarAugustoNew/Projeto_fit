import { Routes } from '@angular/router';
import { ItemNav } from '../layout/item-nav';
import { Shell } from '../layout/shell';
import { Dashboard } from './dashboard/dashboard';

const itens: ItemNav[] = [
  { rota: '/personal/dashboard', rotulo: 'Dashboard', icone: 'dashboard' },
  { rota: '/personal/alunos', rotulo: 'Alunos', icone: 'group' },
];

export const personalRoutes: Routes = [
  {
    path: '',
    component: Shell,
    data: { itens, titulo: 'Projeto Fit' },
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', component: Dashboard },
      {
        path: 'alunos',
        loadComponent: () => import('./alunos/lista-alunos/lista-alunos').then((m) => m.ListaAlunos),
      },
      {
        path: 'alunos/novo',
        loadComponent: () => import('./alunos/form-aluno/form-aluno').then((m) => m.FormAluno),
      },
      {
        path: 'alunos/:id',
        loadComponent: () =>
          import('./alunos/detalhe-aluno/detalhe-aluno').then((m) => m.DetalheAluno),
      },
      {
        path: 'alunos/:id/editar',
        loadComponent: () => import('./alunos/form-aluno/form-aluno').then((m) => m.FormAluno),
      },
    ],
  },
];
