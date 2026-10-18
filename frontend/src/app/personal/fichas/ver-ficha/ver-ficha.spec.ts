import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { Ficha } from '../ficha.model';
import { FichaService } from '../ficha.service';
import { VerFicha } from './ver-ficha';

const ficha = (extra: Partial<Ficha> = {}): Ficha => ({
  id: 'f1',
  alunoId: 'a1',
  nome: 'Hipertrofia',
  objetivo: 'Ganhar massa',
  frequenciaSemanal: 3,
  dataInicio: '2026-10-01',
  dataFim: '2026-12-01',
  status: 'ATIVA',
  vencida: false,
  divisoes: [
    {
      id: 'd1',
      nome: 'Treino A',
      ordem: 1,
      itens: [
        {
          id: 'i1',
          exercicioId: 'e1',
          exercicioNome: 'Supino reto',
          grupoMuscular: 'PEITO',
          ordem: 1,
          series: 3,
          repeticoes: '10',
          cargaKg: 22.5,
          descansoSeg: 60,
          observacao: 'Controlar a descida',
        },
        {
          id: 'i2',
          exercicioId: 'e2',
          exercicioNome: 'Agachamento',
          grupoMuscular: 'QUADRICEPS',
          ordem: 2,
          series: 4,
          repeticoes: '8-12',
          cargaKg: 40,
          descansoSeg: null,
          observacao: null,
        },
        {
          id: 'i3',
          exercicioId: 'e3',
          exercicioNome: 'Flexão',
          grupoMuscular: 'PEITO',
          ordem: 3,
          series: 3,
          repeticoes: '15',
          cargaKg: null,
          descansoSeg: null,
          observacao: null,
        },
      ],
    },
  ],
  ...extra,
});

describe('VerFicha', () => {
  let buscar: ReturnType<typeof vi.fn>;
  let encerrar: ReturnType<typeof vi.fn>;
  let open: ReturnType<typeof vi.fn>;
  let dialogOpen: ReturnType<typeof vi.fn>;

  async function montar(inicial: Ficha, confirmado = true) {
    buscar = vi.fn().mockResolvedValue(inicial);
    encerrar = vi.fn().mockResolvedValue(ficha({ status: 'ENCERRADA' }));
    open = vi.fn();
    dialogOpen = vi.fn().mockReturnValue({ afterClosed: () => of(confirmado) });
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: FichaService, useValue: { buscar, encerrar } },
        { provide: MatSnackBar, useValue: { open } },
        { provide: MatDialog, useValue: { open: dialogOpen } },
      ],
    });
    const fixture = TestBed.createComponent(VerFicha);
    fixture.componentRef.setInput('id', 'f1');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  const el = (f: { nativeElement: HTMLElement }) => f.nativeElement;
  const botao = (f: { nativeElement: HTMLElement }, texto: string) =>
    Array.from(el(f).querySelectorAll('button, a')).find((b) => b.textContent?.trim() === texto) as
      | HTMLElement
      | undefined;

  it('mostra divisões e itens com 3 x 10, 22,5 kg e 60 s', async () => {
    const f = await montar(ficha());
    const texto = el(f).textContent!;
    expect(texto).toContain('Hipertrofia');
    expect(texto).toContain('Ativa');
    expect(texto).toContain('Ganhar massa');
    expect(texto).toContain('01/10/2026');
    expect(texto).toContain('01/12/2026');
    expect(texto).toContain('3x por semana');
    expect(texto).toContain('Treino A');
    expect(texto).toContain('Supino reto');
    expect(texto).toContain('3 x 10');
    expect(texto).toContain('22,5 kg');
    expect(texto).toContain('60 s');
    expect(texto).toContain('Controlar a descida');
    expect(texto).toContain('4 x 8-12');
    expect(texto).toContain('40 kg');
    expect(texto).not.toContain('null');
    expect(el(f).querySelectorAll('.item')[2].textContent).not.toContain('kg');
  });

  it('mostra Vencida quando a ficha está vencida', async () => {
    const f = await montar(ficha({ vencida: true }));
    expect(el(f).textContent).toContain('Vencida');
  });

  it('ficha encerrada não mostra Editar nem Encerrar', async () => {
    const f = await montar(ficha({ status: 'ENCERRADA' }));
    expect(el(f).textContent).toContain('Encerrada');
    expect(botao(f, 'Editar')).toBeFalsy();
    expect(botao(f, 'Encerrar')).toBeFalsy();
  });

  it('ficha ativa mostra Editar apontando para a edição', async () => {
    const f = await montar(ficha());
    expect(botao(f, 'Editar')!.getAttribute('href')).toBe('/personal/fichas/f1/editar');
  });

  it('Encerrar sem confirmação não chama o serviço', async () => {
    const f = await montar(ficha(), false);
    botao(f, 'Encerrar')!.click();
    await f.whenStable();
    expect(dialogOpen.mock.calls[0][1].data).toEqual({
      titulo: 'Encerrar ficha',
      mensagem: 'A ficha "Hipertrofia" deixará de ser a ficha ativa do aluno.',
      confirmar: 'Encerrar',
    });
    expect(encerrar).not.toHaveBeenCalled();
  });

  it('Encerrar confirmado atualiza a tela com a ficha devolvida', async () => {
    const f = await montar(ficha(), true);
    botao(f, 'Encerrar')!.click();
    await f.whenStable();
    f.detectChanges();
    expect(encerrar).toHaveBeenCalledWith('f1');
    expect(open).toHaveBeenCalledWith('Ficha encerrada.', undefined, expect.anything());
    expect(el(f).textContent).toContain('Encerrada');
    expect(botao(f, 'Encerrar')).toBeFalsy();
  });

  it('Voltar ao aluno aponta para o aluno da ficha', async () => {
    const f = await montar(ficha());
    expect(botao(f, 'Voltar ao aluno')!.getAttribute('href')).toBe('/personal/alunos/a1');
  });
});
