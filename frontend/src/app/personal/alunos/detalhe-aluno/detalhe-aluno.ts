import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { AlvoDesempenho } from '../../../shared/desempenho/desempenho.model';
import { PainelDesempenho } from '../../../shared/desempenho/painel-desempenho/painel-desempenho';
import { confirmar } from '../../../shared/confirmacao/confirmacao-dialog';
import { FichasDoAluno } from '../../fichas/fichas-do-aluno/fichas-do-aluno';
import { SessoesDoAluno } from '../../sessoes/sessoes-do-aluno/sessoes-do-aluno';
import { Aluno, ROTULO_STATUS } from '../aluno.model';
import { AlunoService } from '../aluno.service';

@Component({
  selector: 'app-detalhe-aluno',
  imports: [RouterLink, MatButtonModule, FichasDoAluno, SessoesDoAluno, PainelDesempenho],
  templateUrl: './detalhe-aluno.html',
  styleUrl: './detalhe-aluno.scss',
})
export class DetalheAluno implements OnInit {
  private readonly alunos = inject(AlunoService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly id = input.required<string>();

  protected readonly rotulos = ROTULO_STATUS;
  protected readonly aluno = signal<Aluno | null>(null);
  protected readonly alvoDesempenho = computed<AlvoDesempenho | null>(() => {
    const a = this.aluno();
    return a ? { tipo: 'aluno', alunoId: a.id } : null;
  });
  protected readonly carregando = signal(true);
  protected readonly erro = signal(false);
  protected readonly enviando = signal(false);

  async ngOnInit(): Promise<void> {
    try {
      this.aluno.set(await this.alunos.buscar(this.id()));
    } catch {
      this.erro.set(true);
    } finally {
      this.carregando.set(false);
    }
  }

  protected formatarData(data: string | null): string {
    if (!data) {
      return '—';
    }
    const [ano, mes, dia] = data.split('-');
    return `${dia}/${mes}/${ano}`;
  }

  protected async reenviarConvite(): Promise<void> {
    await this.executar(async () => {
      await this.alunos.reenviarConvite(this.id());
      this.snackBar.open('Convite reenviado.', undefined, { duration: 4000 });
    });
  }

  protected async desativar(a: Aluno): Promise<void> {
    const confirmado = await confirmar(this.dialog, {
      titulo: 'Desativar aluno',
      mensagem: `${a.nome} perderá o acesso ao app até ser reativado.`,
      confirmar: 'Desativar',
    });
    if (!confirmado) {
      return;
    }
    await this.executar(async () => {
      this.aluno.set(await this.alunos.alterarStatus(this.id(), 'INATIVO'));
      this.snackBar.open('Aluno desativado.', undefined, { duration: 4000 });
    });
  }

  protected async reativar(): Promise<void> {
    await this.executar(async () => {
      this.aluno.set(await this.alunos.alterarStatus(this.id(), 'ATIVO'));
      this.snackBar.open('Aluno reativado.', undefined, { duration: 4000 });
    });
  }

  private async executar(acao: () => Promise<void>): Promise<void> {
    this.enviando.set(true);
    try {
      await acao();
    } catch {
      // erroInterceptor ja mostra a mensagem; a tela permanece como estava
    } finally {
      this.enviando.set(false);
    }
  }
}
