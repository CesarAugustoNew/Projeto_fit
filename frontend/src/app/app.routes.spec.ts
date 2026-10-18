import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { AuthService } from './core/auth/auth.service';
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
