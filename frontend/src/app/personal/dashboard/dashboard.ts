import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UsuarioService } from '../../core/usuario/usuario.service';
import { DashboardAluno } from '../../shared/desempenho/desempenho.model';
import { DesempenhoService } from '../../shared/desempenho/desempenho.service';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  protected readonly usuarios = inject(UsuarioService);
  private readonly desempenho = inject(DesempenhoService);

  protected readonly alunos = signal<DashboardAluno[]>([]);
  protected readonly carregando = signal(true);
  protected readonly erro = signal(false);

  protected readonly sumiram = computed(() => this.alunos().filter((a) => a.sumiu).length);
  protected readonly fichasVencidas = computed(() => this.alunos().filter((a) => a.fichaVencida).length);

  constructor() {
    void this.carregar();
  }

  protected treinosSemana(a: DashboardAluno): string {
    if (a.planejadoSemana > 0) return `${a.realizadosSemana}/${a.planejadoSemana} treinos nesta semana`;
    const treinos = a.realizadosSemana === 1 ? '1 treino' : `${a.realizadosSemana} treinos`;
    return `${treinos} nesta semana · Sem ficha`;
  }

  protected ultimoTreino(dias: number | null): string {
    if (dias === null) return 'Nunca treinou';
    if (dias === 0) return 'Último treino hoje';
    if (dias === 1) return 'Último treino ontem';
    return `Último treino há ${dias} dias`;
  }

  private async carregar(): Promise<void> {
    try {
      this.alunos.set(await this.desempenho.dashboard());
    } catch {
      this.erro.set(true);
    } finally {
      this.carregando.set(false);
    }
  }
}
