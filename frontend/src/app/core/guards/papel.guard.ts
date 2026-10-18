import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { Papel, Usuario, rotaInicial } from '../usuario/usuario.model';
import { UsuarioService } from '../usuario/usuario.service';

export function papelGuard(papel: Papel): CanActivateFn {
  return async () => {
    const auth = inject(AuthService);
    const usuarioService = inject(UsuarioService);
    const router = inject(Router);

    let usuario: Usuario | null = usuarioService.usuario();
    if (!usuario) {
      try {
        usuario = await usuarioService.carregar();
      } catch {
        await auth.logout();
        return router.parseUrl('/login');
      }
    }
    return usuario.papel === papel ? true : router.parseUrl(rotaInicial(usuario.papel));
  };
}
