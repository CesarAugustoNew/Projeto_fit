import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';

export interface DadosConfirmacao {
  titulo: string;
  mensagem: string;
  confirmar: string;
}

@Component({
  selector: 'app-confirmacao-dialog',
  imports: [MatButtonModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>{{ dados.titulo }}</h2>
    <mat-dialog-content>{{ dados.mensagem }}</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="false">Cancelar</button>
      <button mat-flat-button type="button" [mat-dialog-close]="true">{{ dados.confirmar }}</button>
    </mat-dialog-actions>
  `,
})
export class ConfirmacaoDialog {
  protected readonly dados = inject<DadosConfirmacao>(MAT_DIALOG_DATA);
}

export async function confirmar(dialog: MatDialog, dados: DadosConfirmacao): Promise<boolean> {
  const ref = dialog.open<ConfirmacaoDialog, DadosConfirmacao, boolean>(ConfirmacaoDialog, {
    data: dados,
  });
  return (await firstValueFrom(ref.afterClosed())) === true;
}
