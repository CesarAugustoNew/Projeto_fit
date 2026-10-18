import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { papelGuard } from './core/guards/papel.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', loadComponent: () => import('./publico/login/login').then((m) => m.Login) },
  {
    path: 'definir-senha',
    loadComponent: () => import('./publico/definir-senha/definir-senha').then((m) => m.DefinirSenha),
  },
  {
    path: 'esqueci-senha',
    loadComponent: () => import('./publico/esqueci-senha/esqueci-senha').then((m) => m.EsqueciSenha),
  },
  {
    path: 'indisponivel',
    loadComponent: () => import('./publico/indisponivel/indisponivel').then((m) => m.Indisponivel),
  },
  {
    path: 'personal',
    canActivate: [authGuard, papelGuard('PERSONAL')],
    loadChildren: () => import('./personal/personal.routes').then((m) => m.personalRoutes),
  },
  {
    path: 'aluno',
    canActivate: [authGuard, papelGuard('ALUNO')],
    loadChildren: () => import('./aluno/aluno.routes').then((m) => m.alunoRoutes),
  },
  { path: '**', redirectTo: 'login' },
];
