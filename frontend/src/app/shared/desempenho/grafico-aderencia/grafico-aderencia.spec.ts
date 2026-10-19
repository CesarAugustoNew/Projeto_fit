import { TestBed } from '@angular/core/testing';
import { SemanaAderencia } from '../desempenho.model';
import { GraficoAderencia } from './grafico-aderencia';

const semana = (inicio: string, percentual: number | null): SemanaAderencia => ({
  inicio,
  fim: inicio,
  realizados: 0,
  planejado: percentual === null ? 0 : 4,
  percentual,
});

describe('GraficoAderencia', () => {
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

  async function montar(semanas: SemanaAderencia[]) {
    const fixture = TestBed.createComponent(GraficoAderencia);
    fixture.componentRef.setInput('semanas', semanas);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('mostra a aderência média', async () => {
    const el = await montar([semana('2026-10-05', 100), semana('2026-10-12', 50)]);
    expect(el.textContent).toContain('Aderência média: 75%');
  });

  it('sem ficha no período', async () => {
    const el = await montar([semana('2026-10-05', null)]);
    expect(el.textContent).toContain('Sem ficha no período');
    expect(el.textContent).not.toContain('Aderência média');
  });
});
