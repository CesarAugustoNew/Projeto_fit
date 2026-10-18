import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Pagina, SessaoResumo } from '../../shared/sessao/sessao.model';
import { SessaoService } from '../../shared/sessao/sessao.service';
import { Historico } from './historico';

const resumo = (id: string, divisaoNome: string): SessaoResumo => ({
  id,
  divisaoNome,
  iniciadaEm: '2026-10-18T10:00:00',
  finalizadaEm: '2026-10-18T10:45:00',
  quantidadeExercicios: 5,
  quantidadeSeries: 15,
});

const pagina = (content: SessaoResumo[], number: number, totalPages: number): Pagina<SessaoResumo> => ({
  content,
  page: { size: 20, number, totalElements: content.length, totalPages },
});

describe('Historico', () => {
  async function montar(minhas: ReturnType<typeof vi.fn>) {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: SessaoService, useValue: { minhas } }],
    });
    const fixture = TestBed.createComponent(Historico);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  const el = (f: { nativeElement: HTMLElement }) => f.nativeElement;
  const botao = (f: { nativeElement: HTMLElement }, texto: string) =>
    Array.from(el(f).querySelectorAll('button')).find((b) => b.textContent?.trim() === texto);

  it('lista sessões com data, divisão e duração', async () => {
    const minhas = vi.fn().mockResolvedValue(pagina([resumo('s1', 'Treino A')], 0, 1));
    const f = await montar(minhas);
    expect(minhas).toHaveBeenCalledWith(0);
    const texto = el(f).textContent!;
    expect(texto).toContain('Histórico');
    expect(texto).toContain('18/10/2026 10:00');
    expect(texto).toContain('Treino A');
    expect(texto).toContain('45 min');
    expect(texto).toContain('5 exercícios');
    expect(el(f).querySelector('a.item')!.getAttribute('href')).toBe('/aluno/historico/s1');
    expect(botao(f, 'Carregar mais')).toBeUndefined();
  });

  it('Carregar mais busca a próxima página', async () => {
    const minhas = vi
      .fn()
      .mockResolvedValueOnce(pagina([resumo('s1', 'Treino A')], 0, 2))
      .mockResolvedValueOnce(pagina([resumo('s2', 'Treino B')], 1, 2));
    const f = await montar(minhas);
    botao(f, 'Carregar mais')!.click();
    await f.whenStable();
    f.detectChanges();
    expect(minhas).toHaveBeenLastCalledWith(1);
    expect(el(f).querySelectorAll('a.item').length).toBe(2);
    expect(el(f).textContent).toContain('Treino B');
    expect(botao(f, 'Carregar mais')).toBeUndefined();
  });

  it('sem sessões mostra a mensagem', async () => {
    const f = await montar(vi.fn().mockResolvedValue(pagina([], 0, 0)));
    expect(el(f).textContent).toContain('Você ainda não registrou nenhum treino.');
  });

  it('mostra erro quando falha', async () => {
    const f = await montar(vi.fn().mockRejectedValue(new Error('x')));
    expect(el(f).textContent).toContain('Não foi possível carregar o histórico.');
  });
});
