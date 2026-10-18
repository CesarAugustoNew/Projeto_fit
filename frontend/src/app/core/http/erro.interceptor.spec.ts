import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../auth/auth.service';
import { UsuarioService } from '../usuario/usuario.service';
import { erroInterceptor } from './erro.interceptor';

describe('erroInterceptor', () => {
  let http: HttpClient;
  let ctrl: HttpTestingController;
  const auth = { logout: vi.fn() };
  const usuario = { limpar: vi.fn() };
  const router = { url: '/aluno/treino', navigateByUrl: vi.fn() };
  const snack = { open: vi.fn() };

  beforeEach(() => {
    [auth.logout, usuario.limpar, router.navigateByUrl, snack.open].forEach((f) => f.mockReset());
    auth.logout.mockResolvedValue(undefined);
    router.url = '/aluno/treino';
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([erroInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
        { provide: UsuarioService, useValue: usuario },
        { provide: Router, useValue: router },
        { provide: MatSnackBar, useValue: snack },
      ],
    });
    http = TestBed.inject(HttpClient);
    ctrl = TestBed.inject(HttpTestingController);
  });

  function falhar(body: object, init: { status: number; statusText: string }) {
    let erro: unknown;
    http.get('/x').subscribe({ error: (e) => (erro = e) });
    ctrl.expectOne('/x').flush(body, init);
    return () => erro;
  }

  it('401 desloga, limpa usuário e vai ao login', () => {
    falhar({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.logout).toHaveBeenCalledTimes(1);
    expect(usuario.limpar).toHaveBeenCalledTimes(1);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });

  it('401 já estando no /login não navega de novo', () => {
    router.url = '/login';
    falhar({}, { status: 401, statusText: 'Unauthorized' });
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('erro com Problem Details mostra o detail', () => {
    falhar({ title: 'Conflito', detail: 'E-mail já cadastrado' }, { status: 409, statusText: 'Conflict' });
    expect(snack.open).toHaveBeenCalledWith('E-mail já cadastrado', 'Fechar', { duration: 5000 });
  });

  it('status 0 mostra sem conexão', () => {
    let erro: unknown;
    http.get('/x').subscribe({ error: (e) => (erro = e) });
    ctrl.expectOne('/x').error(new ProgressEvent('error'));
    expect(erro).toBeDefined();
    expect(snack.open).toHaveBeenCalledWith('Sem conexão com o servidor.', 'Fechar', { duration: 5000 });
  });

  it('403 não mostra snackbar', () => {
    falhar({}, { status: 403, statusText: 'Forbidden' });
    expect(snack.open).not.toHaveBeenCalled();
  });

  it('relança o erro', () => {
    const erro = falhar({ detail: 'x' }, { status: 500, statusText: 'Server Error' });
    expect((erro() as { status: number }).status).toBe(500);
  });
});
