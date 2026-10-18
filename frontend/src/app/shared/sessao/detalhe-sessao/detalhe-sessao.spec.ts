import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Sessao } from '../sessao.model';
import { SessaoService } from '../sessao.service';
import { DetalheSessao } from './detalhe-sessao';

const serie = (exercicioId: string, nome: string, ordem: number, numeroSerie: number, cargaKg: number | null) => ({
  exercicioId,
  exercicioNome: nome,
  itemDivisaoId: null,
  ordem,
  numeroSerie,
  repeticoes: 10,
  cargaKg,
});

const sessao: Sessao = {
  id: 's1',
  clientId: 'c1',
  alunoId: 'a1',
  divisaoId: 'd1',
  divisaoNome: 'Treino A',
  iniciadaEm: '2026-10-18T10:00:00',
  finalizadaEm: '2026-10-18T10:45:00',
  observacao: 'Foi pesado',
  series: [
    serie('e1', 'Supino reto', 1, 1, 22.5),
    serie('e1', 'Supino reto', 2, 2, 22.5),
    serie('e2', 'Agachamento', 3, 1, null),
  ],
};

describe('DetalheSessao', () => {
  async function montar(resultado: Sessao | Error, voltarPara?: string) {
    const buscar =
      resultado instanceof Error ? vi.fn().mockRejectedValue(resultado) : vi.fn().mockResolvedValue(resultado);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: SessaoService, useValue: { buscar } }],
    });
    const fixture = TestBed.createComponent(DetalheSessao);
    fixture.componentRef.setInput('id', 's1');
    if (voltarPara) fixture.componentRef.setInput('voltarPara', voltarPara);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, buscar };
  }

  const el = (f: { nativeElement: HTMLElement }) => f.nativeElement;

  it('agrupa séries por exercício com reps e carga', async () => {
    const { fixture: f, buscar } = await montar(sessao);
    expect(buscar).toHaveBeenCalledWith('s1');
    const grupos = Array.from(el(f).querySelectorAll('.exercicio'));
    expect(grupos.length).toBe(2);
    expect(grupos[0].querySelector('h2')!.textContent).toContain('Supino reto');
    expect(grupos[0].querySelectorAll('li').length).toBe(2);
    expect(grupos[0].textContent).toContain('Série 1: 10 reps · 22,5 kg');
    expect(grupos[1].textContent).toContain('Agachamento');
    expect(grupos[1].textContent).toContain('Série 1: 10 reps');
    expect(grupos[1].textContent).not.toContain('kg');
  });

  it('mostra duração e observação', async () => {
    const { fixture: f } = await montar(sessao);
    const texto = el(f).textContent!;
    expect(texto).toContain('Treino A');
    expect(texto).toContain('18/10/2026 10:00');
    expect(texto).toContain('45 min');
    expect(texto).toContain('Observação');
    expect(texto).toContain('Foi pesado');
  });

  it('link Voltar usa voltarPara ou o padrão', async () => {
    const { fixture: f } = await montar(sessao);
    const voltar = el(f).querySelector('a.voltar') as HTMLAnchorElement;
    expect(voltar.textContent!.trim()).toBe('Voltar');
    expect(voltar.getAttribute('href')).toBe('/aluno/historico');
    TestBed.resetTestingModule();
    const { fixture: g } = await montar(sessao, '/personal/alunos');
    expect((el(g).querySelector('a.voltar') as HTMLAnchorElement).getAttribute('href')).toBe('/personal/alunos');
  });

  it('mostra erro quando falha', async () => {
    const { fixture: f } = await montar(new Error('x'));
    expect(el(f).textContent).toContain('Não foi possível carregar o treino.');
  });
});
