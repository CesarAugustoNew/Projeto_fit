import { TestBed } from '@angular/core/testing';
import { CHAVE_TEMA, TemaService } from './tema.service';

describe('TemaService', () => {
  const matchMediaOriginal = window.matchMedia;
  let ouvintes: ((e: { matches: boolean }) => void)[];

  function simularSistema(escuro: boolean) {
    ouvintes = [];
    window.matchMedia = vi.fn().mockReturnValue({
      matches: escuro,
      addEventListener: (_: string, fn: (e: { matches: boolean }) => void) => ouvintes.push(fn),
    }) as never;
  }

  beforeEach(() => {
    localStorage.clear();
    document.documentElement.style.colorScheme = '';
    simularSistema(false);
  });

  afterEach(() => {
    window.matchMedia = matchMediaOriginal;
    localStorage.clear();
  });

  it('sem escolha salva segue o sistema', () => {
    simularSistema(true);
    const tema = TestBed.inject(TemaService);
    expect(tema.preferencia()).toBe('sistema');
    expect(tema.escuro()).toBe(true);
    expect(document.documentElement.style.colorScheme).toBe('light dark');
  });

  it('acompanha a troca de modo do sistema', () => {
    const tema = TestBed.inject(TemaService);
    expect(tema.escuro()).toBe(false);
    ouvintes.forEach((fn) => fn({ matches: true }));
    expect(tema.escuro()).toBe(true);
  });

  it('usa a escolha salva', () => {
    localStorage.setItem(CHAVE_TEMA, 'escuro');
    const tema = TestBed.inject(TemaService);
    expect(tema.preferencia()).toBe('escuro');
    expect(tema.escuro()).toBe(true);
    expect(document.documentElement.style.colorScheme).toBe('dark');
  });

  it('alternar troca o modo da tela e guarda a escolha', () => {
    const tema = TestBed.inject(TemaService);
    tema.alternar();
    expect(tema.escuro()).toBe(true);
    expect(localStorage.getItem(CHAVE_TEMA)).toBe('escuro');
    expect(document.documentElement.style.colorScheme).toBe('dark');

    tema.alternar();
    expect(tema.escuro()).toBe(false);
    expect(localStorage.getItem(CHAVE_TEMA)).toBe('claro');
    expect(document.documentElement.style.colorScheme).toBe('light');
  });

  it('voltar ao sistema apaga a escolha salva', () => {
    localStorage.setItem(CHAVE_TEMA, 'claro');
    const tema = TestBed.inject(TemaService);
    tema.definir('sistema');
    expect(localStorage.getItem(CHAVE_TEMA)).toBeNull();
    expect(document.documentElement.style.colorScheme).toBe('light dark');
  });

  it('valor desconhecido salvo vira sistema', () => {
    localStorage.setItem(CHAVE_TEMA, 'roxo');
    expect(TestBed.inject(TemaService).preferencia()).toBe('sistema');
  });
});
