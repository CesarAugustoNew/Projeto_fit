import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { papelGuard } from './core/guards/papel.guard';
import { DefinirSenha } from './publico/definir-senha/definir-senha';
import { EsqueciSenha } from './publico/esqueci-senha/esqueci-senha';
import { Login } from './publico/login/login';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', component: Login },
  { path: 'definir-senha', component: DefinirSenha },
  { path: 'esqueci-senha', component: EsqueciSenha },
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
