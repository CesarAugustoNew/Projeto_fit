import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { FichaResumo } from '../ficha.model';
import { FichaService } from '../ficha.service';
import { FichasDoAluno } from './fichas-do-aluno';

const ficha = (extra: Partial<FichaResumo> = {}): FichaResumo => ({
  id: 'f1',
  nome: 'Hipertrofia A/B',
  status: 'ATIVA',
  dataInicio: '2026-10-01',
  dataFim: '2026-12-15',
  frequenciaSemanal: 3,
  quantidadeDivisoes: 2,
  vencida: false,
  ...extra,
});

describe('FichasDoAluno', () => {
  async function montar(fichas: FichaResumo[] | Error, alunoAtivo = true) {
    const listarDoAluno =
      fichas instanceof Error ? vi.fn().mockRejectedValue(fichas) : vi.fn().mockResolvedValue(fichas);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: FichaService, useValue: { listarDoAluno } }],
    });
    const fixture = TestBed.createComponent(FichasDoAluno);
    fixture.componentRef.setInput('alunoId', 'a1');
    fixture.componentRef.setInput('alunoAtivo', alunoAtivo);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, listarDoAluno };
  }

  const el = (f: { nativeElement: HTMLElement }) => f.nativeElement;
  const link = (f: { nativeElement: HTMLElement }, texto: string) =>
    Array.from(el(f).querySelectorAll('a')).find((a) => a.textContent?.trim() === texto);

  it('mostra a ficha ativa com frequência, divisões e ações', async () => {
    const { fixture: f, listarDoAluno } = await montar([ficha()]);
    const t = el(f).textContent!;
    expect(listarDoAluno).toHaveBeenCalledWith('a1');
    expect(t).toContain('Hipertrofia A/B');
    expect(t).toContain('Início 01/10/2026');
    expect(t).toContain('Fim 15/12/2026');
    expect(t).toContain('3x por semana');
    expect(t).toContain('2 divisões');
    expect(t).not.toContain('Vencida');
    expect(link(f, 'Ver')?.getAttribute('href')).toBe('/personal/fichas/f1');
    expect(link(f, 'Editar')?.getAttribute('href')).toBe('/personal/fichas/f1/editar');
    expect(link(f, 'Nova ficha')?.getAttribute('href')).toBe('/personal/alunos/a1/fichas/nova');
  });

  it('usa singular para 1 divisão', async () => {
    const { fixture: f } = await montar([ficha({ quantidadeDivisoes: 1 })]);
    expect(el(f).textContent).toContain('1 divisão');
    expect(el(f).textContent).not.toContain('1 divisões');
  });

  it('mostra Vencida quando a ficha venceu', async () => {
    const { fixture: f } = await montar([ficha({ vencida: true })]);
    expect(el(f).textContent).toContain('Vencida');
  });

  it('sem ficha ativa mostra a mensagem', async () => {
    const { fixture: f } = await montar([]);
    expect(el(f).textContent).toContain('Nenhuma ficha ativa.');
    expect(el(f).textContent).not.toContain('Fichas anteriores');
  });

  it('lista fichas anteriores com link para visualizar', async () => {
    const { fixture: f } = await montar([
      ficha(),
      ficha({ id: 'f0', nome: 'Base', status: 'ENCERRADA', dataInicio: '2026-06-01', dataFim: '2026-09-30' }),
    ]);
    const t = el(f).textContent!;
    expect(t).toContain('Fichas anteriores');
    expect(t).toContain('Base');
    expect(t).toContain('01/06/2026');
    expect(t).toContain('30/09/2026');
    const anterior = Array.from(el(f).querySelectorAll('a')).find((a) => a.textContent?.includes('Base'));
    expect(anterior?.getAttribute('href')).toBe('/personal/fichas/f0');
  });

  it('aluno inativo não mostra Nova ficha', async () => {
    const { fixture: f } = await montar([], false);
    expect(link(f, 'Nova ficha')).toBeUndefined();
  });

  it('mostra mensagem de erro quando falha', async () => {
    const { fixture: f } = await montar(new Error('x'));
    expect(el(f).textContent).toContain('Não foi possível carregar as fichas.');
  });
});
