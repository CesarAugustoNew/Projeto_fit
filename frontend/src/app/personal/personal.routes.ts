import { Routes } from '@angular/router';
import { ItemNav } from '../layout/item-nav';
import { Shell } from '../layout/shell';
import { Dashboard } from './dashboard/dashboard';

const itens: ItemNav[] = [
  { rota: '/personal/dashboard', rotulo: 'Dashboard', icone: 'dashboard' },
  { rota: '/personal/alunos', rotulo: 'Alunos', icone: 'group' },
  { rota: '/personal/exercicios', rotulo: 'Exercícios', icone: 'fitness_center' },
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
      {
        path: 'alunos/:alunoId/fichas/nova',
        loadComponent: () => import('./fichas/form-ficha/form-ficha').then((m) => m.FormFicha),
      },
      {
        path: 'fichas/:id',
        loadComponent: () => import('./fichas/ver-ficha/ver-ficha').then((m) => m.VerFicha),
      },
      {
        path: 'fichas/:id/editar',
        loadComponent: () => import('./fichas/form-ficha/form-ficha').then((m) => m.FormFicha),
      },
      {
        path: 'exercicios',
        loadComponent: () =>
          import('./exercicios/lista-exercicios/lista-exercicios').then((m) => m.ListaExercicios),
      },
      {
        path: 'exercicios/novo',
        loadComponent: () =>
          import('./exercicios/form-exercicio/form-exercicio').then((m) => m.FormExercicio),
      },
      {
        path: 'exercicios/:id/editar',
        loadComponent: () =>
          import('./exercicios/form-exercicio/form-exercicio').then((m) => m.FormExercicio),
      },
    ],
  },
];
