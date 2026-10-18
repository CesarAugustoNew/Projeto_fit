import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { SUPABASE_CLIENT } from './supabase';

describe('AuthService', () => {
  let service: AuthService;
  const supabaseFalso = {
    auth: {
      signInWithPassword: vi.fn(),
      signOut: vi.fn(),
      getSession: vi.fn(),
      updateUser: vi.fn(),
      resetPasswordForEmail: vi.fn(),
    },
  };

  beforeEach(() => {
    vi.resetAllMocks();
    TestBed.configureTestingModule({
      providers: [{ provide: SUPABASE_CLIENT, useValue: supabaseFalso }],
    });
    service = TestBed.inject(AuthService);
  });

  it('login chama signInWithPassword com email e senha', async () => {
    supabaseFalso.auth.signInWithPassword.mockResolvedValue({ data: {}, error: null });
    await service.login('a@b.com', '12345678');
    expect(supabaseFalso.auth.signInWithPassword).toHaveBeenCalledWith({
      email: 'a@b.com',
      password: '12345678',
    });
  });

  it('login lança erro quando o Supabase devolve erro', async () => {
    supabaseFalso.auth.signInWithPassword.mockResolvedValue({
      data: {},
      error: { message: 'Invalid login credentials' },
    });
    await expect(service.login('a@b.com', 'x')).rejects.toThrow();
  });

  it('token devolve access_token da sessão ou null', async () => {
    supabaseFalso.auth.getSession.mockResolvedValue({ data: { session: { access_token: 'abc' } } });
    expect(await service.token()).toBe('abc');
    supabaseFalso.auth.getSession.mockResolvedValue({ data: { session: null } });
    expect(await service.token()).toBeNull();
  });

  it('enviarRedefinicao usa redirectTo origin + /definir-senha', async () => {
    supabaseFalso.auth.resetPasswordForEmail.mockResolvedValue({ data: {}, error: null });
    await service.enviarRedefinicao('a@b.com');
    expect(supabaseFalso.auth.resetPasswordForEmail).toHaveBeenCalledWith(
      'a@b.com',
      expect.objectContaining({ redirectTo: `${window.location.origin}/definir-senha` }),
    );
  });
});
