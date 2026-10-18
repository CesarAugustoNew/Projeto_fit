import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  const auth = { estaLogado: vi.fn() };

  beforeEach(() => {
    auth.estaLogado.mockReset();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
  });

  const executar = () => TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));

  it('sem sessão redireciona para /login', async () => {
    auth.estaLogado.mockResolvedValue(false);
    const r = await executar();
    expect(TestBed.inject(Router).serializeUrl(r as never)).toBe('/login');
  });

  it('com sessão permite', async () => {
    auth.estaLogado.mockResolvedValue(true);
    expect(await executar()).toBe(true);
  });
});
