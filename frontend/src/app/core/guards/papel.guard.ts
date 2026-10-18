import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { Papel, Usuario, rotaInicial } from '../usuario/usuario.model';
import { UsuarioService } from '../usuario/usuario.service';

export function papelGuard(papel: Papel): CanActivateFn {
  return async (_route, state) => {
    const auth = inject(AuthService);
    const usuarioService = inject(UsuarioService);
    const router = inject(Router);

    // os guards rodam em paralelo: sem sessao nao chama /me (iria sem token)
    if (!(await auth.estaLogado())) {
      return router.parseUrl('/login');
    }

    let usuario: Usuario | null = usuarioService.usuario();
    if (!usuario) {
      try {
        usuario = await usuarioService.carregar();
      } catch (e) {
        if (e instanceof HttpErrorResponse && (e.status === 401 || e.status === 403)) {
          await auth.logout();
          return router.parseUrl('/login');
        }
        // rede (0) ou 5xx: falha passageira, mantem a sessao e mostra a tela de indisponivel
        return router.createUrlTree(['/indisponivel'], { queryParams: { voltar: state.url } });
      }
    }
    return usuario.papel === papel ? true : router.parseUrl(rotaInicial(usuario.papel));
  };
}
