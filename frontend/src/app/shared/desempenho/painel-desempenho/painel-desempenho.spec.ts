import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AlvoDesempenho, ExercicioExecutado, PontoEvolucao, SemanaAderencia } from '../desempenho.model';
import { DesempenhoService } from '../desempenho.service';
import { PainelDesempenho } from './painel-desempenho';

const alvo: AlvoDesempenho = { tipo: 'aluno', alunoId: 'a1' };
const exercicios: ExercicioExecutado[] = [
  { id: 'e1', nome: 'Supino', grupoMuscular: 'PEITO' },
  { id: 'e2', nome: 'Agachamento', grupoMuscular: 'QUADRICEPS' },
];
const semanas: SemanaAderencia[] = [
  { inicio: '2026-10-12', fim: '2026-10-18', realizados: 2, planejado: 3, percentual: 67 },
];
const pontos = (carga: number): PontoEvolucao[] => [
  { data: '2026-10-05', sessaoId: 's1', cargaMaxima: carga, volume: 100 },
];

// As chamadas ao serviço são promises simples: esvazia a fila antes de reavaliar a tela.
async function estabilizar(f: ComponentFixture<unknown>): Promise<void> {
  await new Promise((r) => setTimeout(r));
  f.detectChanges();
}

describe('PainelDesempenho', () => {
  const getContextOriginal = HTMLCanvasElement.prototype.getContext;
  let svc: { exercicios: ReturnType<typeof vi.fn>; evolucao: ReturnType<typeof vi.fn>; aderencia: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    HTMLCanvasElement.prototype.getContext = (() => null) as typeof getContextOriginal;
    const original = console.error;
    vi.spyOn(console, 'error').mockImplementation((...args: unknown[]) => {
      if (String(args[0]).includes('Failed to create chart')) return;
      original(...args);
    });
    svc = {
      exercicios: vi.fn().mockResolvedValue(exercicios),
      evolucao: vi.fn().mockResolvedValue(pontos(40)),
      aderencia: vi.fn().mockResolvedValue(semanas),
    };
    TestBed.configureTestingModule({ providers: [{ provide: DesempenhoService, useValue: svc }] });
  });

  afterEach(() => {
    HTMLCanvasElement.prototype.getContext = getContextOriginal;
    vi.restoreAllMocks();
  });

  async function montar() {
    const fixture = TestBed.createComponent(PainelDesempenho);
    fixture.componentRef.setInput('alvo', alvo);
    fixture.detectChanges();
    await estabilizar(fixture);
    fixture.detectChanges();
    return fixture;
  }

  it('carrega aderência e exercícios e mostra a evolução do primeiro', async () => {
    const f = await montar();
    const el = f.nativeElement as HTMLElement;
    expect(svc.aderencia).toHaveBeenCalledWith(alvo, 8);
    expect(svc.exercicios).toHaveBeenCalledWith(alvo);
    expect(svc.evolucao).toHaveBeenCalledWith(alvo, 'e1');
    expect(el.textContent).toContain('Desempenho');
    expect(el.textContent).toContain('Aderência semanal');
    expect(el.textContent).toContain('Evolução por exercício');
    expect(el.querySelector('mat-select')).not.toBeNull();
    expect(el.querySelector('app-grafico-evolucao')).not.toBeNull();
    expect(el.querySelector('app-grafico-aderencia')).not.toBeNull();
  });

  it('trocar o exercício recarrega a evolução', async () => {
    const f = await montar();
    (f.componentInstance as unknown as { trocarExercicio(id: string): void }).trocarExercicio('e2');
    await estabilizar(f);
    expect(svc.evolucao).toHaveBeenLastCalledWith(alvo, 'e2');
  });

  it('descarta resposta atrasada ao trocar de exercício', async () => {
    const f = await montar();
    const comp = f.componentInstance as unknown as {
      trocarExercicio(id: string): void;
      pontos: () => PontoEvolucao[];
    };
    let resolverLento!: (p: PontoEvolucao[]) => void;
    svc.evolucao.mockReturnValueOnce(new Promise((r) => (resolverLento = r)));
    comp.trocarExercicio('e2');
    svc.evolucao.mockResolvedValueOnce(pontos(99));
    comp.trocarExercicio('e1');
    await estabilizar(f);
    resolverLento(pontos(1));
    await estabilizar(f);
    expect(comp.pontos()[0].cargaMaxima).toBe(99);
  });

  it('sem exercícios executados mostra a mensagem', async () => {
    svc.exercicios.mockResolvedValue([]);
    const f = await montar();
    const el = f.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Nenhum treino registrado ainda.');
    expect(el.querySelector('mat-select')).toBeNull();
    expect(svc.evolucao).not.toHaveBeenCalled();
  });

  it('erro mostra a mensagem de falha', async () => {
    svc.aderencia.mockRejectedValue(new Error('x'));
    const f = await montar();
    expect((f.nativeElement as HTMLElement).textContent).toContain(
      'Não foi possível carregar o desempenho.',
    );
  });
});
