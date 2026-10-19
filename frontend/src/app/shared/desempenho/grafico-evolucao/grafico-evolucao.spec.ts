import { TestBed } from '@angular/core/testing';
import { PontoEvolucao } from '../desempenho.model';
import { GraficoEvolucao } from './grafico-evolucao';

const pontos: PontoEvolucao[] = [
  { data: '2026-10-05', sessaoId: 's1', cargaMaxima: 40, volume: 800 },
  { data: '2026-10-12', sessaoId: 's2', cargaMaxima: 42.5, volume: 900 },
];

describe('GraficoEvolucao', () => {
  const getContextOriginal = HTMLCanvasElement.prototype.getContext;

  beforeEach(() => {
    // jsdom não tem canvas.
    HTMLCanvasElement.prototype.getContext = (() => null) as typeof getContextOriginal;
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
  });

  afterEach(() => {
    HTMLCanvasElement.prototype.getContext = getContextOriginal;
    vi.restoreAllMocks();
  });

  async function montar(p: PontoEvolucao[]) {
    const fixture = TestBed.createComponent(GraficoEvolucao);
    fixture.componentRef.setInput('pontos', p);
    await fixture.whenStable();
    return fixture;
  }

  it('sem pontos mostra a mensagem de vazio', async () => {
    const fixture = await montar([]);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Ainda não há treinos com este exercício no período.');
    expect(el.querySelector('canvas')).toBeNull();
  });

  it('exercício sem carga no modo Carga mostra mensagem e Volume segue disponível', async () => {
    const fixture = await montar([
      { data: '2026-10-05', sessaoId: 's1', cargaMaxima: null, volume: 0 },
      { data: '2026-10-12', sessaoId: 's2', cargaMaxima: null, volume: 0 },
    ]);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Exercício sem carga registrada. Veja o volume ou o histórico de treinos.');
    expect(el.querySelector('canvas')).toBeNull();

    const botoes = Array.from(el.querySelectorAll('mat-button-toggle button')) as HTMLButtonElement[];
    botoes.find((b) => b.textContent?.includes('Volume'))!.click();
    await fixture.whenStable();
    expect(el.textContent).not.toContain('Exercício sem carga registrada');
    expect(el.querySelector('canvas')).not.toBeNull();
  });

  it('toggle Volume troca a medida', async () => {
    const fixture = await montar(pontos);
    const el = fixture.nativeElement as HTMLElement;
    const dados = () => (fixture.componentInstance as unknown as { dados: () => { datasets: { data: unknown[] }[] } }).dados();
    expect(el.querySelector('canvas')).not.toBeNull();
    expect(dados().datasets[0].data).toEqual([40, 42.5]);

    const botoes = Array.from(el.querySelectorAll('mat-button-toggle button')) as HTMLButtonElement[];
    const volume = botoes.find((b) => b.textContent?.includes('Volume'))!;
    volume.click();
    await fixture.whenStable();

    expect(dados().datasets[0].data).toEqual([800, 900]);
  });
});
