import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { UsuarioService } from '../../core/usuario/usuario.service';
import { DefinirSenha } from './definir-senha';

describe('DefinirSenha', () => {
  let fixture: ComponentFixture<DefinirSenha>;
  let el: HTMLElement;
  const auth = { definirSenha: vi.fn() };
  const usuario = { carregar: vi.fn() };
  let router: Router;

  function preencher(senha: string, confirmar: string) {
    const inputs = el.querySelectorAll('input');
    inputs[0].value = senha;
    inputs[0].dispatchEvent(new Event('input'));
    inputs[1].value = confirmar;
    inputs[1].dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  const botao = () => el.querySelector('button[type="submit"]') as HTMLButtonElement;

  beforeEach(() => {
    vi.resetAllMocks();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: UsuarioService, useValue: usuario },
      ],
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(DefinirSenha);
    el = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('senhas diferentes mostram erro e não chamam definirSenha', async () => {
    preencher('novaSenha1', 'outraSenha2');
    botao().click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(el.textContent).toContain('As senhas não conferem.');
    expect(auth.definirSenha).not.toHaveBeenCalled();
  });

  it('senha válida salva, carrega /me e navega', async () => {
    auth.definirSenha.mockResolvedValue(undefined);
    usuario.carregar.mockResolvedValue({ papel: 'ALUNO' });
    preencher('novaSenha1', 'novaSenha1');
    botao().click();
    await fixture.whenStable();
    expect(auth.definirSenha).toHaveBeenCalledWith('novaSenha1');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/aluno/treino');
  });

  it('falha ao salvar mostra mensagem', async () => {
    auth.definirSenha.mockRejectedValue(new Error('x'));
    preencher('novaSenha1', 'novaSenha1');
    botao().click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(el.textContent).toContain('Não foi possível salvar a senha. O link pode ter expirado.');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
