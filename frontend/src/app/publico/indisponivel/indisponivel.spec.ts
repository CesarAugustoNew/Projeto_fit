import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { Indisponivel } from './indisponivel';

describe('Indisponivel', () => {
  const router = { navigateByUrl: vi.fn() };

  function criar(voltar: string | null) {
    router.navigateByUrl.mockReset();
    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: router },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(voltar ? { voltar } : {}) } },
        },
      ],
    });
    const fixture = TestBed.createComponent(Indisponivel);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('mostra título e texto', () => {
    const el = criar(null);
    expect(el.textContent).toContain('Servidor indisponível');
    expect(el.textContent).toContain(
      'Não foi possível conectar ao servidor. Verifique sua internet e tente de novo.',
    );
  });

  it('Tentar de novo volta para a URL pedida', () => {
    const el = criar('/personal/alunos');
    el.querySelector('button')!.click();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/personal/alunos');
  });

  it('voltar externo é ignorado', () => {
    const el = criar('//evil.com');
    el.querySelector('button')!.click();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });

  it('sem voltar usa /login', () => {
    const el = criar(null);
    el.querySelector('button')!.click();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });
});
