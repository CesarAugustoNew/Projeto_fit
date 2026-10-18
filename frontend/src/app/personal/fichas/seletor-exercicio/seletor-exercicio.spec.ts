import { TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { Exercicio, GrupoMuscular } from '../../exercicios/exercicio.model';
import { ExercicioService } from '../../exercicios/exercicio.service';
import { SeletorExercicio, escolherExercicio } from './seletor-exercicio';

const ex = (id: string, nome: string, grupoMuscular: GrupoMuscular = 'PEITO'): Exercicio => ({
  id,
  nome,
  grupoMuscular,
  videoUrl: null,
  observacao: null,
  ativo: true,
});

const catalogo = [ex('1', 'Supino reto'), ex('2', 'Tríceps testa', 'TRICEPS'), ex('3', 'Remada', 'COSTAS')];

describe('SeletorExercicio', () => {
  let listar: ReturnType<typeof vi.fn>;
  let close: ReturnType<typeof vi.fn>;

  async function criar() {
    listar = vi.fn().mockResolvedValue(catalogo);
    close = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        { provide: ExercicioService, useValue: { listar } },
        { provide: MatDialogRef, useValue: { close } },
      ],
    });
    const fixture = TestBed.createComponent(SeletorExercicio);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  const texto = (f: { nativeElement: HTMLElement }) => f.nativeElement.textContent ?? '';

  it('carrega só ativos e lista nome e grupo', async () => {
    const f = await criar();
    expect(listar).toHaveBeenCalledTimes(1);
    expect(listar).toHaveBeenCalledWith({ ativo: true });
    expect(texto(f)).toContain('Supino reto');
    expect(texto(f)).toContain('Peito');
    expect(texto(f)).toContain('Tríceps testa');
  });

  it('busca ignora acentos', async () => {
    const f = await criar();
    const input = f.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = 'triceps';
    input.dispatchEvent(new Event('input'));
    await f.whenStable();
    f.detectChanges();
    expect(texto(f)).toContain('Tríceps testa');
    expect(texto(f)).not.toContain('Supino reto');
  });

  it('mostra mensagem quando nada combina', async () => {
    const f = await criar();
    const input = f.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = 'zzz';
    input.dispatchEvent(new Event('input'));
    await f.whenStable();
    f.detectChanges();
    expect(texto(f)).toContain('Nenhum exercício encontrado.');
  });

  it('filtra por grupo', async () => {
    const f = await criar();
    (f.nativeElement.querySelector('.mat-mdc-select-trigger') as HTMLElement).click();
    f.detectChanges();
    const opcoes = Array.from(document.querySelectorAll('mat-option')) as HTMLElement[];
    expect(opcoes[0].textContent).toContain('Todos');
    opcoes.find((o) => o.textContent?.includes('Costas'))!.click();
    await f.whenStable();
    f.detectChanges();
    expect(texto(f)).toContain('Remada');
    expect(texto(f)).not.toContain('Supino reto');
  });

  it('clicar no exercício fecha devolvendo-o', async () => {
    const f = await criar();
    const itens = Array.from(f.nativeElement.querySelectorAll('button.item')) as HTMLButtonElement[];
    itens.find((b) => b.textContent?.includes('Remada'))!.click();
    expect(close).toHaveBeenCalledWith(catalogo[2]);
  });
});

describe('escolherExercicio', () => {
  async function renderizar() {
    TestBed.tick();
    await new Promise((r) => setTimeout(r));
    TestBed.tick();
  }

  it('no celular ocupa a altura visível da tela (dvh)', async () => {
    const original = window.matchMedia;
    window.matchMedia = vi.fn().mockReturnValue({ matches: true }) as never;
    try {
      const open = vi.fn().mockReturnValue({ afterClosed: () => of(null) });
      expect(await escolherExercicio({ open } as unknown as MatDialog)).toBeNull();
      expect(open.mock.calls[0][1]).toMatchObject({ height: '100dvh', maxHeight: '100dvh' });
    } finally {
      window.matchMedia = original;
    }
  });

  it('devolve null ao cancelar', async () => {
    TestBed.configureTestingModule({
      providers: [{ provide: ExercicioService, useValue: { listar: vi.fn().mockResolvedValue(catalogo) } }],
    });
    const p = escolherExercicio(TestBed.inject(MatDialog));
    await renderizar();
    expect(document.querySelector('mat-dialog-container')?.textContent).toContain('Escolher exercício');
    const cancelar = Array.from(document.querySelectorAll('mat-dialog-container button')).find(
      (b) => b.textContent?.trim() === 'Cancelar',
    ) as HTMLButtonElement;
    cancelar.click();
    expect(await p).toBeNull();
  });
});
