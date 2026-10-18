import { BreakpointObserver } from '@angular/cdk/layout';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { appConfig } from './app.config';
import { AuthService } from './core/auth/auth.service';
import { Usuario } from './core/usuario/usuario.model';
import { UsuarioService } from './core/usuario/usuario.service';
import { routes } from './app.routes';

describe('rotas', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        { provide: AuthService, useValue: { estaLogado: async () => false, sessao: async () => null, logout: vi.fn() } },
        { provide: UsuarioService, useValue: { usuario: () => null, carregar: vi.fn() } },
      ],
    });
  });

  it('rota vazia redireciona para /login', async () => {
    await RouterTestingHarness.create('/');
    expect(TestBed.inject(Router).url).toBe('/login');
  });

  it('/personal usa authGuard e papelGuard', () => {
    const rota = routes.find((r) => r.path === 'personal');
    expect(rota?.canActivate?.length).toBe(2);
  });

  it('/aluno usa authGuard e papelGuard', () => {
    const rota = routes.find((r) => r.path === 'aluno');
    expect(rota?.canActivate?.length).toBe(2);
  });
});

describe('área do personal (configuração real do router)', () => {
  const personal: Usuario = { id: '1', nome: 'P', email: 'p@teste.com', papel: 'PERSONAL' };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        // providers reais do app (inclui withComponentInputBinding) com os serviços
        // substituídos por stubs que fazem authGuard e papelGuard permitirem
        ...appConfig.providers,
        { provide: AuthService, useValue: { estaLogado: async () => true, logout: vi.fn() } },
        { provide: UsuarioService, useValue: { usuario: signal(personal), carregar: vi.fn(), limpar: vi.fn() } },
        { provide: BreakpointObserver, useValue: { observe: () => of({ matches: false, breakpoints: {} }) } },
      ],
    });
  });

  const titulo = (el: HTMLElement) => el.querySelector('.titulo')?.textContent?.trim();

  it('Shell recebe itens e título de data da rota', async () => {
    const harness = await RouterTestingHarness.create('/personal/dashboard');
    const el = harness.routeNativeElement!;
    expect(titulo(el)).toBe('Projeto Fit');
    expect(el.querySelector('nav a')?.textContent).toContain('Dashboard');
  });

  it('query param ?titulo= não altera o título', async () => {
    const harness = await RouterTestingHarness.create('/personal/dashboard?titulo=Hackeado');
    expect(titulo(harness.routeNativeElement!)).toBe('Projeto Fit');
  });
});
