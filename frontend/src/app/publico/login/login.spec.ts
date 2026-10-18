import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { UsuarioService } from '../../core/usuario/usuario.service';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let el: HTMLElement;
  const auth = { login: vi.fn(), estaLogado: vi.fn(), logout: vi.fn() };
  const usuario = { carregar: vi.fn() };
  let router: Router;

  async function criar() {
    fixture = TestBed.createComponent(Login);
    el = fixture.nativeElement;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function preencher(email: string, senha: string) {
    const inputs = el.querySelectorAll('input');
    inputs[0].value = email;
    inputs[0].dispatchEvent(new Event('input'));
    inputs[1].value = senha;
    inputs[1].dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  const botao = () => el.querySelector('button[type="submit"]') as HTMLButtonElement;

  beforeEach(() => {
    vi.resetAllMocks();
    auth.estaLogado.mockResolvedValue(false);
    auth.logout.mockResolvedValue(undefined);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: UsuarioService, useValue: usuario },
      ],
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  it('botão desabilitado com formulário inválido', async () => {
    await criar();
    expect(botao().disabled).toBe(true);
    preencher('personal@teste.com', '12345678');
    expect(botao().disabled).toBe(false);
  });

  it('login válido carrega /me e navega para a rota do papel', async () => {
    usuario.carregar.mockResolvedValue({ papel: 'PERSONAL' });
    auth.login.mockResolvedValue(undefined);
    await criar();
    preencher('personal@teste.com', '12345678');
    botao().click();
    await fixture.whenStable();
    expect(auth.login).toHaveBeenCalledWith('personal@teste.com', '12345678');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/personal/dashboard');
  });

  it('credenciais inválidas mostram mensagem', async () => {
    auth.login.mockRejectedValue(new Error('x'));
    await criar();
    preencher('personal@teste.com', '12345678');
    botao().click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(el.textContent).toContain('E-mail ou senha inválidos.');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(auth.logout).not.toHaveBeenCalled();
  });

  async function entrarComFalhaNoMe(status: number) {
    auth.login.mockResolvedValue(undefined);
    usuario.carregar.mockRejectedValue(new HttpErrorResponse({ status }));
    await criar();
    preencher('personal@teste.com', '12345678');
    botao().click();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('/me com 403 desloga e avisa que a conta não tem acesso', async () => {
    await entrarComFalhaNoMe(403);
    expect(auth.logout).toHaveBeenCalled();
    expect(el.textContent).toContain('Sua conta não tem acesso ao app.');
    expect(el.textContent).not.toContain('E-mail ou senha inválidos.');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('/me com 500 desloga e pede para tentar de novo', async () => {
    await entrarComFalhaNoMe(500);
    expect(auth.logout).toHaveBeenCalled();
    expect(el.textContent).toContain('Não foi possível entrar. Tente novamente.');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('/me sem conexão (status 0) desloga e pede para tentar de novo', async () => {
    await entrarComFalhaNoMe(0);
    expect(auth.logout).toHaveBeenCalled();
    expect(el.textContent).toContain('Não foi possível entrar. Tente novamente.');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('com sessão ativa ao abrir, redireciona direto', async () => {
    auth.estaLogado.mockResolvedValue(true);
    usuario.carregar.mockResolvedValue({ papel: 'ALUNO' });
    await criar();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/aluno/treino');
  });

  it('com sessão ativa ao abrir e /me falhando, permanece no login sem quebrar', async () => {
    auth.estaLogado.mockResolvedValue(true);
    usuario.carregar.mockRejectedValue(new HttpErrorResponse({ status: 500 }));
    await criar();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(el.querySelector('form')).not.toBeNull();
    expect(el.querySelector('.erro')).toBeNull();
  });
});
