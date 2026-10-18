import { Routes } from '@angular/router';
import { ItemNav } from '../layout/item-nav';
import { Shell } from '../layout/shell';
import { Dashboard } from './dashboard/dashboard';

const itens: ItemNav[] = [{ rota: '/personal/dashboard', rotulo: 'Dashboard', icone: 'dashboard' }];

export const personalRoutes: Routes = [
  {
    path: '',
    component: Shell,
    data: { itens },
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', component: Dashboard },
    ],
  },
];
