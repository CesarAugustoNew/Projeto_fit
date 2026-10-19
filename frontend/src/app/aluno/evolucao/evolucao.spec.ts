import { TestBed } from '@angular/core/testing';
import { PainelDesempenho } from '../../shared/desempenho/painel-desempenho/painel-desempenho';
import { DesempenhoService } from '../../shared/desempenho/desempenho.service';
import { Evolucao } from './evolucao';

describe('Evolucao', () => {
  it('usa o alvo aluno-logado', async () => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: DesempenhoService,
          useValue: {
            exercicios: vi.fn().mockResolvedValue([]),
            aderencia: vi.fn().mockResolvedValue([]),
            evolucao: vi.fn(),
          },
        },
      ],
    });
    const f = TestBed.createComponent(Evolucao);
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect((f.nativeElement as HTMLElement).textContent).toContain('Minha evolução');
    const painel = f.debugElement.children
      .flatMap((c) => [c, ...c.children])
      .find((d) => d.componentInstance instanceof PainelDesempenho)!;
    expect(painel.componentInstance.alvo()).toEqual({ tipo: 'aluno-logado' });
  });
});
