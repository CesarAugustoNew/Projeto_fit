import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';
import { provideSupabase } from './core/auth/supabase';
import { authInterceptor } from './core/http/auth.interceptor';
import { erroInterceptor } from './core/http/erro.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding({ queryParams: false })),
    provideHttpClient(withInterceptors([authInterceptor, erroInterceptor])),
    provideSupabase(),
  ]
};
