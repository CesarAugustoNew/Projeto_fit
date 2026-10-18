import { Component, inject } from '@angular/core';
import { UsuarioService } from '../../core/usuario/usuario.service';

@Component({
  selector: 'app-dashboard',
  template: `<h1>Dashboard</h1>
    <p>Olá, {{ usuarios.usuario()?.nome }}</p>`,
})
export class Dashboard {
  protected readonly usuarios = inject(UsuarioService);
}
