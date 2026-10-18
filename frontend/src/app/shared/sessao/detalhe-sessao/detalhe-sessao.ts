import { Component, effect, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { Sessao, SerieExecutada, duracaoMinutos, formatarDataHora } from '../sessao.model';
import { SessaoService } from '../sessao.service';

interface GrupoExercicio {
  exercicioId: string;
  nome: string;
  series: SerieExecutada[];
}

@Component({
  selector: 'app-detalhe-sessao',
  imports: [RouterLink, MatButtonModule],
  templateUrl: './detalhe-sessao.html',
  styleUrl: './detalhe-sessao.scss',
})
export class DetalheSessao {
  private readonly servico = inject(SessaoService);
  private carga = 0;

  readonly id = input.required<string>();
  readonly voltarPara = input<string>();

  protected readonly sessao = signal<Sessao | null>(null);
  protected readonly grupos = signal<GrupoExercicio[]>([]);
  protected readonly carregando = signal(true);
  protected readonly erro = signal(false);

  protected readonly formatarDataHora = formatarDataHora;
  protected readonly duracaoMinutos = duracaoMinutos;

  constructor() {
    effect(() => {
      void this.carregar(this.id());
    });
  }

  protected formatarCarga(kg: number): string {
    return `${String(kg).replace('.', ',')} kg`;
  }

  private async carregar(id: string): Promise<void> {
    const carga = ++this.carga;
    this.carregando.set(true);
    this.erro.set(false);
    try {
      const sessao = await this.servico.buscar(id);
      if (carga !== this.carga) return;
      this.sessao.set(sessao);
      this.grupos.set(this.agrupar(sessao.series));
    } catch {
      if (carga !== this.carga) return;
      this.sessao.set(null);
      this.erro.set(true);
    } finally {
      if (carga === this.carga) this.carregando.set(false);
    }
  }

  private agrupar(series: SerieExecutada[]): GrupoExercicio[] {
    const grupos = new Map<string, GrupoExercicio>();
    for (const s of series) {
      let grupo = grupos.get(s.exercicioId);
      if (!grupo) {
        grupo = { exercicioId: s.exercicioId, nome: s.exercicioNome, series: [] };
        grupos.set(s.exercicioId, grupo);
      }
      grupo.series.push(s);
    }
    return [...grupos.values()];
  }
}
