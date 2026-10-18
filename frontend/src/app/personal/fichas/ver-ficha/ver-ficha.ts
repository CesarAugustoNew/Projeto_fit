import { Component, OnInit, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { confirmar } from '../../../shared/confirmacao/confirmacao-dialog';
import { Ficha, formatarData } from '../ficha.model';
import { FichaService } from '../ficha.service';

@Component({
  selector: 'app-ver-ficha',
  imports: [RouterLink, MatButtonModule],
  templateUrl: './ver-ficha.html',
  styleUrl: './ver-ficha.scss',
})
export class VerFicha implements OnInit {
  private readonly fichas = inject(FichaService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly id = input.required<string>();

  protected readonly ficha = signal<Ficha | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erro = signal(false);
  protected readonly enviando = signal(false);

  protected readonly formatarData = formatarData;

  async ngOnInit(): Promise<void> {
    try {
      this.ficha.set(await this.fichas.buscar(this.id()));
    } catch {
      this.erro.set(true);
    } finally {
      this.carregando.set(false);
    }
  }

  protected formatarCarga(kg: number): string {
    return `${String(kg).replace('.', ',')} kg`;
  }

  protected async encerrar(f: Ficha): Promise<void> {
    const confirmado = await confirmar(this.dialog, {
      titulo: 'Encerrar ficha',
      mensagem: `A ficha "${f.nome}" deixará de ser a ficha ativa do aluno.`,
      confirmar: 'Encerrar',
    });
    if (!confirmado) {
      return;
    }
    this.enviando.set(true);
    try {
      this.ficha.set(await this.fichas.encerrar(this.id()));
      this.snackBar.open('Ficha encerrada.', undefined, { duration: 4000 });
    } catch {
      // erroInterceptor ja mostra a mensagem; a tela permanece como estava
    } finally {
      this.enviando.set(false);
    }
  }
}
