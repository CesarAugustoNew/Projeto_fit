import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Pagina, SessaoResumo } from '../../../shared/sessao/sessao.model';
import { SessaoService } from '../../../shared/sessao/sessao.service';
import { SessoesDoAluno } from './sessoes-do-aluno';

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

describe('SessoesDoAluno', () => {
  async function montar(doAluno: ReturnType<typeof vi.fn>) {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: SessaoService, useValue: { doAluno } }],
    });
    const fixture = TestBed.createComponent(SessoesDoAluno);
    fixture.componentRef.setInput('alunoId', 'a1');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  const el = (f: { nativeElement: HTMLElement }) => f.nativeElement;
  const botao = (f: { nativeElement: HTMLElement }, texto: string) =>
    Array.from(el(f).querySelectorAll('button')).find((b) => b.textContent?.trim() === texto);

  it('lista os treinos do aluno com link para o detalhe', async () => {
    const doAluno = vi.fn().mockResolvedValue(pagina([resumo('s1', 'Treino A')], 0, 1));
    const f = await montar(doAluno);
    expect(doAluno).toHaveBeenCalledWith('a1', 0);
    const texto = el(f).textContent!;
    expect(texto).toContain('Histórico de treinos');
    expect(texto).toContain('18/10/2026 10:00');
    expect(texto).toContain('Treino A');
    expect(texto).toContain('45 min');
    expect(texto).toContain('5 exercícios');
    expect(el(f).querySelector('a.item')!.getAttribute('href')).toBe('/personal/sessoes/s1');
    expect(botao(f, 'Carregar mais')).toBeUndefined();
  });

  it('Carregar mais ignora sessões já listadas', async () => {
    const doAluno = vi
      .fn()
      .mockResolvedValueOnce(pagina([resumo('s1', 'Treino A'), resumo('s2', 'Treino B')], 0, 2))
      .mockResolvedValueOnce(pagina([resumo('s2', 'Treino B'), resumo('s3', 'Treino C')], 1, 2));
    const f = await montar(doAluno);
    botao(f, 'Carregar mais')!.click();
    await f.whenStable();
    f.detectChanges();
    const itens = Array.from(el(f).querySelectorAll('a.item'));
    expect(itens.length).toBe(3);
    expect(itens.map((a) => a.getAttribute('href')!.split('/').at(-1))).toEqual(['s1', 's2', 's3']);
  });

  it('Carregar mais busca a próxima página', async () => {
    const doAluno = vi
      .fn()
      .mockResolvedValueOnce(pagina([resumo('s1', 'Treino A')], 0, 2))
      .mockResolvedValueOnce(pagina([resumo('s2', 'Treino B')], 1, 2));
    const f = await montar(doAluno);
    botao(f, 'Carregar mais')!.click();
    await f.whenStable();
    f.detectChanges();
    expect(doAluno).toHaveBeenLastCalledWith('a1', 1);
    expect(el(f).querySelectorAll('a.item').length).toBe(2);
    expect(botao(f, 'Carregar mais')).toBeUndefined();
  });

  it('sem treinos mostra a mensagem', async () => {
    const f = await montar(vi.fn().mockResolvedValue(pagina([], 0, 0)));
    expect(el(f).textContent).toContain('Nenhum treino registrado ainda.');
  });
});
