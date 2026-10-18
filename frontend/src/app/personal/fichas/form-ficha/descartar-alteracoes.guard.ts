import { inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { CanDeactivateFn } from '@angular/router';
import { confirmar } from '../../../shared/confirmacao/confirmacao-dialog';
import type { FormFicha } from './form-ficha';

/** Ao sair do editor de ficha com alterações não salvas, pede confirmação antes de descartá-las. */
export const descartarAlteracoesGuard: CanDeactivateFn<FormFicha> = (componente) => {
  if (!componente.temAlteracoesNaoSalvas()) {
    return true;
  }
  return confirmar(inject(MatDialog), {
    titulo: 'Descartar alterações?',
    mensagem: 'As alterações desta ficha ainda não foram salvas.',
    confirmar: 'Descartar',
  });
};
