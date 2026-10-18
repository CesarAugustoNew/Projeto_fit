import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { confirmar } from './confirmacao-dialog';

describe('ConfirmacaoDialog', () => {
  const dados = { titulo: 'Desativar aluno', mensagem: 'Ana perderá o acesso.', confirmar: 'Desativar' };

  function botao(texto: string): HTMLButtonElement {
    const botoes = Array.from(document.querySelectorAll('mat-dialog-container button'));
    return botoes.filter((b) => b.textContent?.trim() === texto).at(-1) as HTMLButtonElement;
  }

  async function renderizar() {
    TestBed.tick();
    await new Promise((r) => setTimeout(r));
    TestBed.tick();
  }

  it('confirmar devolve true ao clicar no botão de confirmação e false ao cancelar', async () => {
    const dialog = TestBed.inject(MatDialog);

    const p1 = confirmar(dialog, dados);
    await renderizar();
    expect(document.querySelector('mat-dialog-container')?.textContent).toContain('Desativar aluno');
    expect(document.querySelector('mat-dialog-container')?.textContent).toContain('Ana perderá o acesso.');
    botao('Desativar').click();
    expect(await p1).toBe(true);

    const p2 = confirmar(dialog, dados);
    await renderizar();
    botao('Cancelar').click();
    expect(await p2).toBe(false);
  });
});
