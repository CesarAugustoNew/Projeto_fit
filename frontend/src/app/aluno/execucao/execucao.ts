import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { confirmar } from '../../shared/confirmacao/confirmacao-dialog';
import { ExercicioEmAndamento } from '../sessao-local/sessao-local.model';
import { SessaoLocalService } from '../sessao-local/sessao-local.service';

/** Limites aceitos pelo back. */
const MAX_CARGA = 9999.99;
const MAX_REPETICOES = 999;
const ROTA_TREINO = '/aluno/treino';
const MENSAGENS = {
  enviado: 'Treino registrado!',
  pendente: 'Treino salvo. Ele será enviado quando houver conexão.',
  recusado: 'O treino não pôde ser registrado. Veja o aviso na tela de treino.',
};

@Component({
  selector: 'app-execucao',
  imports: [MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule],
  templateUrl: './execucao.html',
  styleUrl: './execucao.scss',
})
export class Execucao implements OnInit {
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly local = inject(SessaoLocalService);

  protected readonly finalizando = signal(false);
  protected readonly erro = signal<string | null>(null);
  private readonly agora = signal(Date.now());

  protected readonly minutos = computed(() => {
    const inicio = this.local.emAndamento()?.iniciadaEm;
    if (!inicio) return 0;
    return Math.max(0, Math.floor((this.agora() - new Date(inicio).getTime()) / 60_000));
  });

  constructor() {
    const timer = setInterval(() => this.agora.set(Date.now()), 60_000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  ngOnInit(): void {
    if (!this.local.emAndamento()) this.voltar();
  }

  protected prescricao(e: ExercicioEmAndamento): string {
    const p = e.prescricao;
    let texto = `${p.series} x ${p.repeticoes}`;
    if (p.cargaKg !== null) texto += ` · ${p.cargaKg.toLocaleString('pt-BR')} kg`;
    if (p.descansoSeg !== null) texto += ` · ${p.descansoSeg} s`;
    return texto;
  }

  /**
   * `final = false` (evento `input`): só grava se o texto já é um valor completo e válido,
   * para não apagar o que o usuário está digitando. `final = true` (`change`): grava o
   * valor ou `null` e limpa o campo quando inválido.
   */
  protected mudarCarga(ex: number, serie: number, evento: Event, final: boolean): void {
    const campo = evento.target as HTMLInputElement;
    const n = campo.value === '' ? NaN : Number(campo.value);
    const valida = Number.isFinite(n) && n >= 0 && n <= MAX_CARGA;
    if (!valida && !final) return;
    if (!valida) campo.value = '';
    this.local.atualizarSerie(ex, serie, { cargaKg: valida ? Math.round(n * 100) / 100 : null });
  }

  protected mudarRepeticoes(ex: number, serie: number, evento: Event, final: boolean): void {
    const campo = evento.target as HTMLInputElement;
    const n = campo.value === '' ? NaN : Math.trunc(Number(campo.value));
    const valida = Number.isFinite(n) && n >= 0 && n <= MAX_REPETICOES;
    if (valida) {
      this.local.atualizarSerie(ex, serie, { repeticoes: n });
    } else if (final) {
      campo.value = '';
      // Série sem repetições válidas nunca fica marcada como concluída.
      this.local.atualizarSerie(ex, serie, { repeticoes: null, concluida: false });
    }
  }

  protected alternar(ex: number, serie: number, concluida: boolean): void {
    this.local.atualizarSerie(ex, serie, { concluida: !concluida });
  }

  protected async finalizar(): Promise<void> {
    if (this.finalizando()) return;
    // Garante que um `change` pendente do campo em foco seja gravado antes de finalizar.
    (document.activeElement as HTMLElement | null)?.blur();
    const andamento = this.local.emAndamento();
    if (!andamento) return;
    this.finalizando.set(true);
    this.erro.set(null);
    try {
      const temPendentes = andamento.exercicios.some((e) => e.series.some((s) => !s.concluida));
      if (temPendentes) {
        const ok = await confirmar(this.dialog, {
          titulo: 'Finalizar treino',
          mensagem: 'Algumas séries não foram concluídas e não serão registradas.',
          confirmar: 'Finalizar',
        });
        if (!ok) return;
      }
      const resultado = await this.local.finalizar();
      this.snackBar.open(MENSAGENS[resultado], undefined, { duration: 5000 });
      this.voltar();
    } catch (e) {
      this.erro.set(e instanceof Error ? e.message : 'Não foi possível finalizar o treino.');
    } finally {
      this.finalizando.set(false);
    }
  }

  private voltar(): void {
    void this.router.navigate([ROTA_TREINO], { replaceUrl: true });
  }
}
