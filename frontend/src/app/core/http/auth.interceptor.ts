import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';

/** apiUrl vazia (produção até o deploy) = apenas URLs relativas. */
export function ehChamadaApi(url: string, apiUrl: string): boolean {
  if (apiUrl === '') {
    return !/^https?:\/\//i.test(url);
  }
  return url.startsWith(apiUrl);
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!ehChamadaApi(req.url, environment.apiUrl)) {
    return next(req);
  }
  const auth = inject(AuthService);
  return from(auth.token()).pipe(
    switchMap((token) =>
      next(token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req),
    ),
  );
};
