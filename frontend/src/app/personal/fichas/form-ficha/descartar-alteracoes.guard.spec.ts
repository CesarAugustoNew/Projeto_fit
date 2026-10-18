import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { personalRoutes } from '../../personal.routes';
import { descartarAlteracoesGuard } from './descartar-alteracoes.guard';
import { FormFicha } from './form-ficha';

describe('descartarAlteracoesGuard', () => {
  let dialogOpen: ReturnType<typeof vi.fn>;

  function executar(alterado: boolean, confirma = true) {
    dialogOpen = vi.fn().mockReturnValue({ afterClosed: () => of(confirma) });
    TestBed.configureTestingModule({ providers: [{ provide: MatDialog, useValue: { open: dialogOpen } }] });
    const componente = { temAlteracoesNaoSalvas: () => alterado } as FormFicha;
    return TestBed.runInInjectionContext(() =>
      descartarAlteracoesGuard(componente, null as never, null as never, null as never),
    );
  }

  it('sem alterações deixa sair sem perguntar', async () => {
    expect(await executar(false)).toBe(true);
    expect(dialogOpen).not.toHaveBeenCalled();
  });

  it('com alterações pede confirmação e deixa sair se confirmado', async () => {
    expect(await executar(true, true)).toBe(true);
    expect(dialogOpen.mock.calls[0][1].data).toEqual({
      titulo: 'Descartar alterações?',
      mensagem: 'As alterações desta ficha ainda não foram salvas.',
      confirmar: 'Descartar',
    });
  });

  it('com alterações e cancelado permanece no editor', async () => {
    expect(await executar(true, false)).toBe(false);
  });

  it('está registrado nas rotas de nova ficha e de edição', () => {
    const filhas = personalRoutes[0].children ?? [];
    for (const caminho of ['alunos/:alunoId/fichas/nova', 'fichas/:id/editar']) {
      const rota = filhas.find((r) => r.path === caminho);
      expect(rota).toBeDefined();
      expect(rota!.canDeactivate ?? []).toContain(descartarAlteracoesGuard);
    }
  });
});
