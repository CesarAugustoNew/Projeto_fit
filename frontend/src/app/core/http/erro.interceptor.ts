import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { UsuarioService } from '../usuario/usuario.service';
import { SILENCIOSO } from './silencioso';

export const erroInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const usuario = inject(UsuarioService);
  const router = inject(Router);
  const snackBar = inject(MatSnackBar);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        if (error.status === 401) {
          void auth.logout();
          usuario.limpar();
          if (router.url !== '/login') {
            void router.navigateByUrl('/login');
          }
        } else if (error.status === 403) {
          // no /login a propria tela mostra a mensagem
          if (router.url.split('?')[0] !== '/login') {
            void auth.logout();
            usuario.limpar();
            void router.navigateByUrl('/login');
            snackBar.open('Sua conta não tem acesso ao app.', 'Fechar', { duration: 5000 });
          }
        } else if (req.context.get(SILENCIOSO) && (error.status === 0 || error.status >= 500)) {
          // envio em segundo plano: quem chamou trata (ex.: mantém o treino na fila)
        } else if (error.status === 0) {
          snackBar.open('Sem conexão com o servidor.', 'Fechar', { duration: 5000 });
        } else {
          const msg = error.error?.detail ?? error.error?.title ?? 'Erro inesperado.';
          snackBar.open(msg, 'Fechar', { duration: 5000 });
        }
      }
      return throwError(() => error);
    }),
  );
};
