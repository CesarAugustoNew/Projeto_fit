import { TestBed } from '@angular/core/testing';
import { TemaService } from '../../core/tema/tema.service';
import { AlternarTema } from './alternar-tema';

describe('AlternarTema', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => localStorage.clear());

  async function montar(comTexto = false) {
    const fixture = TestBed.createComponent(AlternarTema);
    fixture.componentRef.setInput('comTexto', comTexto);
    await fixture.whenStable();
    return fixture;
  }

  it('no modo claro oferece o modo escuro e alterna ao clicar', async () => {
    TestBed.inject(TemaService).definir('claro');
    const fixture = await montar();
    const botao = (fixture.nativeElement as HTMLElement).querySelector('button')!;
    expect(botao.getAttribute('aria-label')).toBe('Usar modo escuro');

    botao.click();
    await fixture.whenStable();

    expect(TestBed.inject(TemaService).escuro()).toBe(true);
    expect(botao.getAttribute('aria-label')).toBe('Usar modo claro');
  });

  it('com texto mostra o rótulo visível', async () => {
    TestBed.inject(TemaService).definir('escuro');
    const fixture = await montar(true);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Usar modo claro');
  });
});
