import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { Divisao } from '../../personal/fichas/ficha.model';
import { confirmar } from '../../shared/confirmacao/confirmacao-dialog';
import { FichaAtiva } from '../../shared/sessao/sessao.model';
import { SessaoService } from '../../shared/sessao/sessao.service';
import { SessaoLocalService } from '../sessao-local/sessao-local.service';

const ROTA_EXECUCAO = '/aluno/treino/execucao';

@Component({
  selector: 'app-treino',
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './treino.html',
  styleUrl: './treino.scss',
})
export class Treino implements OnInit {
  private readonly sessaoService = inject(SessaoService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  protected readonly local = inject(SessaoLocalService);

  protected readonly ativa = signal<FichaAtiva | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erro = signal(false);
  private readonly escolhida = signal<string | null>(null);

  protected readonly divisaoEscolhidaId = computed(
    () =>
      this.escolhida() ??
      this.ativa()?.proximaDivisaoId ??
      this.ativa()?.ficha.divisoes[0]?.id ??
      null,
  );

  async ngOnInit(): Promise<void> {
    void this.local.enviarPendentes();
    try {
      this.ativa.set(await this.sessaoService.fichaAtiva());
    } catch {
      this.erro.set(true);
    } finally {
      this.carregando.set(false);
    }
  }

  protected escolher(id: string): void {
    this.escolhida.set(id);
  }

  protected rotuloExercicios(n: number): string {
    return n === 1 ? '1 exercício' : `${n} exercícios`;
  }

  protected rotuloPendentes(n: number): string {
    return n === 1 ? '1 treino aguardando envio' : `${n} treinos aguardando envio`;
  }

  protected continuar(): void {
    void this.router.navigate([ROTA_EXECUCAO]);
  }

  protected async descartar(): Promise<void> {
    const ok = await confirmar(this.dialog, {
      titulo: 'Descartar treino',
      mensagem: 'As séries marcadas neste treino serão perdidas.',
      confirmar: 'Descartar',
    });
    if (ok) this.local.descartar();
  }

  protected comecar(): void {
    const ficha = this.ativa()?.ficha;
    const divisao: Divisao | undefined = ficha?.divisoes.find(
      (d) => d.id === this.divisaoEscolhidaId(),
    );
    if (!ficha || !divisao || this.local.emAndamento()) return;
    this.local.iniciar(ficha, divisao);
    void this.router.navigate([ROTA_EXECUCAO]);
  }
}
