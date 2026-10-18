import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { Aluno } from '../aluno.model';
import { AlunoService } from '../aluno.service';
import { DetalheAluno } from './detalhe-aluno';

const aluno = (extra: Partial<Aluno> = {}): Aluno => ({
  id: 'a1',
  nome: 'Ana',
  email: 'ana@x.com',
  telefone: null,
  dataNascimento: null,
  objetivo: null,
  observacoes: null,
  status: 'CONVIDADO',
  criadoEm: '2026-10-01T10:00:00Z',
  ...extra,
});

describe('DetalheAluno', () => {
  let buscar: ReturnType<typeof vi.fn>;
  let alterarStatus: ReturnType<typeof vi.fn>;
  let reenviarConvite: ReturnType<typeof vi.fn>;
  let open: ReturnType<typeof vi.fn>;
  let dialogOpen: ReturnType<typeof vi.fn>;

  async function montar(inicial: Aluno, confirmado = true) {
    buscar = vi.fn().mockResolvedValue(inicial);
    alterarStatus = vi.fn().mockResolvedValue(aluno({ status: 'INATIVO' }));
    reenviarConvite = vi.fn().mockResolvedValue(undefined);
    open = vi.fn();
    dialogOpen = vi.fn().mockReturnValue({ afterClosed: () => of(confirmado) });
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AlunoService, useValue: { buscar, alterarStatus, reenviarConvite } },
        { provide: MatSnackBar, useValue: { open } },
        { provide: MatDialog, useValue: { open: dialogOpen } },
      ],
    });
    const fixture = TestBed.createComponent(DetalheAluno);
    fixture.componentRef.setInput('id', 'a1');
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

  it('mostra os dados e os campos vazios como —', async () => {
    const f = await montar(aluno({ dataNascimento: '1990-05-01', objetivo: 'Emagrecer' }));
    const texto = el(f).textContent!;
    expect(texto).toContain('Ana');
    expect(texto).toContain('ana@x.com');
    expect(texto).toContain('Convite pendente');
    expect(texto).toContain('01/05/1990');
    expect(texto).toContain('Emagrecer');
    expect(el(f).querySelector('[data-campo="telefone"]')?.textContent).toContain('—');
    expect(el(f).querySelector('[data-campo="observacoes"]')?.textContent).toContain('—');
  });

  it('CONVIDADO mostra Reenviar convite e Desativar, sem Reativar', async () => {
    const f = await montar(aluno());
    expect(botao(f, 'Reenviar convite')).toBeTruthy();
    expect(botao(f, 'Desativar')).toBeTruthy();
    expect(botao(f, 'Reativar')).toBeFalsy();
    expect(botao(f, 'Editar')).toBeTruthy();
    expect(botao(f, 'Voltar')).toBeTruthy();
  });

  it('Desativar pede confirmação e só altera se confirmado', async () => {
    const f = await montar(aluno(), false);
    botao(f, 'Desativar')!.click();
    await f.whenStable();
    expect(dialogOpen).toHaveBeenCalled();
    expect(dialogOpen.mock.calls[0][1].data).toEqual({
      titulo: 'Desativar aluno',
      mensagem: 'Ana perderá o acesso ao app até ser reativado.',
      confirmar: 'Desativar',
    });
    expect(alterarStatus).not.toHaveBeenCalled();
  });

  it('Desativar confirmado altera para INATIVO e mostra o aluno devolvido', async () => {
    const f = await montar(aluno(), true);
    botao(f, 'Desativar')!.click();
    await f.whenStable();
    f.detectChanges();
    expect(alterarStatus).toHaveBeenCalledWith('a1', 'INATIVO');
    expect(open).toHaveBeenCalledWith('Aluno desativado.', undefined, expect.anything());
    expect(el(f).textContent).toContain('Inativo');
    expect(botao(f, 'Reativar')).toBeTruthy();
  });

  it('Reativar altera para ATIVO e mostra Aluno reativado.', async () => {
    const f = await montar(aluno({ status: 'INATIVO' }));
    alterarStatus.mockResolvedValue(aluno({ status: 'CONVIDADO' }));
    botao(f, 'Reativar')!.click();
    await f.whenStable();
    f.detectChanges();
    expect(alterarStatus).toHaveBeenCalledWith('a1', 'ATIVO');
    expect(open).toHaveBeenCalledWith('Aluno reativado.', undefined, expect.anything());
    expect(botao(f, 'Reenviar convite')).toBeTruthy();
  });

  it('Reenviar convite chama o serviço e mostra Convite reenviado.', async () => {
    const f = await montar(aluno());
    botao(f, 'Reenviar convite')!.click();
    await f.whenStable();
    expect(reenviarConvite).toHaveBeenCalledWith('a1');
    expect(open).toHaveBeenCalledWith('Convite reenviado.', undefined, expect.anything());
  });
});
