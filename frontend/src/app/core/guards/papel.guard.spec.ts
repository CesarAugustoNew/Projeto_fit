import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { Papel, Usuario } from '../usuario/usuario.model';
import { UsuarioService } from '../usuario/usuario.service';
import { papelGuard } from './papel.guard';

describe('papelGuard', () => {
  const auth = { logout: vi.fn() };
  const atual = signal<Usuario | null>(null);
  const usuarioSvc = { usuario: atual, carregar: vi.fn() };

  beforeEach(() => {
    auth.logout.mockReset();
    auth.logout.mockResolvedValue(undefined);
    usuarioSvc.carregar.mockReset();
    atual.set(null);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: UsuarioService, useValue: usuarioSvc },
      ],
    });
  });

  const executar = (papel: Papel) =>
    TestBed.runInInjectionContext(() => papelGuard(papel)({} as never, {} as never));
  const usr = (papel: Papel): Usuario => ({ id: '1', nome: 'A', email: 'a@b.com', papel });

  it('papel igual permite', async () => {
    atual.set(usr('PERSONAL'));
    expect(await executar('PERSONAL')).toBe(true);
    expect(usuarioSvc.carregar).not.toHaveBeenCalled();
  });

  it('carrega o usuário quando ainda não está no signal', async () => {
    usuarioSvc.carregar.mockResolvedValue(usr('ALUNO'));
    expect(await executar('ALUNO')).toBe(true);
  });

  it('aluno em rota de personal vai para /aluno/treino', async () => {
    atual.set(usr('ALUNO'));
    const r = await executar('PERSONAL');
    expect(TestBed.inject(Router).serializeUrl(r as never)).toBe('/aluno/treino');
  });

  it('falha ao carregar /me desloga e vai para /login', async () => {
    usuarioSvc.carregar.mockRejectedValue(new Error('403'));
    const r = await executar('PERSONAL');
    expect(auth.logout).toHaveBeenCalled();
    expect(TestBed.inject(Router).serializeUrl(r as never)).toBe('/login');
  });
});
