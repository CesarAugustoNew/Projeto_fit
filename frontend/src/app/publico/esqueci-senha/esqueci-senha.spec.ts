import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { EsqueciSenha } from './esqueci-senha';

describe('EsqueciSenha', () => {
  let fixture: ComponentFixture<EsqueciSenha>;
  let el: HTMLElement;
  const auth = { enviarRedefinicao: vi.fn() };

  async function enviar() {
    const input = el.querySelector('input') as HTMLInputElement;
    input.value = 'a@b.com';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    (el.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  beforeEach(() => {
    vi.resetAllMocks();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
    fixture = TestBed.createComponent(EsqueciSenha);
    el = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('envia o link e mostra a mensagem de sucesso', async () => {
    auth.enviarRedefinicao.mockResolvedValue(undefined);
    await enviar();
    expect(auth.enviarRedefinicao).toHaveBeenCalledWith('a@b.com');
    expect(el.textContent).toContain(
      'Se o e-mail estiver cadastrado, você receberá um link para redefinir a senha.',
    );
  });

  it('falha ao enviar mostra mensagem de erro', async () => {
    auth.enviarRedefinicao.mockRejectedValue(new Error('x'));
    await enviar();
    expect(el.textContent).toContain('Não foi possível enviar o link. Tente novamente.');
  });
});
