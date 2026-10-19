import { HttpContextToken } from '@angular/common/http';

/** Requisição em segundo plano: o `erroInterceptor` não avisa falhas de rede (0) nem 5xx. */
export const SILENCIOSO = new HttpContextToken<boolean>(() => false);
