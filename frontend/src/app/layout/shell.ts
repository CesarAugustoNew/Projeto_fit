import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Component, inject, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '../core/auth/auth.service';
import { UsuarioService } from '../core/usuario/usuario.service';
import { LogoRaio } from '../shared/marca/logo-raio';
import { AlternarTema } from '../shared/tema/alternar-tema';
import { ItemNav } from './item-nav';

@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatButtonModule,
    MatIconModule,
    MatSidenavModule,
    LogoRaio,
    AlternarTema,
  ],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly auth = inject(AuthService);
  private readonly usuarios = inject(UsuarioService);
  private readonly router = inject(Router);

  readonly itens = input.required<ItemNav[]>();
  readonly titulo = input('Projeto Fit');

  protected readonly celular = toSignal(
    inject(BreakpointObserver)
      .observe(Breakpoints.Handset)
      .pipe(map((estado) => estado.matches)),
    { initialValue: false },
  );

  protected async sair(): Promise<void> {
    await this.auth.logout();
    this.usuarios.limpar();
    await this.router.navigateByUrl('/login');
  }
}
