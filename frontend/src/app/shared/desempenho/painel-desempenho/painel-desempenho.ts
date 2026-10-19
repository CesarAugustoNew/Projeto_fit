import { Component, OnInit, inject, input, signal } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import {
  AlvoDesempenho,
  ExercicioExecutado,
  PontoEvolucao,
  SemanaAderencia,
} from '../desempenho.model';
import { DesempenhoService } from '../desempenho.service';
import { GraficoAderencia } from '../grafico-aderencia/grafico-aderencia';
import { GraficoEvolucao } from '../grafico-evolucao/grafico-evolucao';

@Component({
  selector: 'app-painel-desempenho',
  imports: [MatFormFieldModule, MatSelectModule, GraficoAderencia, GraficoEvolucao],
  templateUrl: './painel-desempenho.html',
  styleUrl: './painel-desempenho.scss',
})
export class PainelDesempenho implements OnInit {
  private readonly servico = inject(DesempenhoService);

  readonly alvo = input.required<AlvoDesempenho>();

  protected readonly carregando = signal(true);
  protected readonly erro = signal(false);
  protected readonly semanas = signal<SemanaAderencia[]>([]);
  protected readonly exercicios = signal<ExercicioExecutado[]>([]);
  protected readonly exercicioId = signal<string | null>(null);
  protected readonly pontos = signal<PontoEvolucao[]>([]);
  protected readonly carregandoEvolucao = signal(false);
  protected readonly erroEvolucao = signal(false);

  private carga = 0;

  async ngOnInit(): Promise<void> {
    const alvo = this.alvo();
    try {
      const [semanas, exercicios] = await Promise.all([
        this.servico.aderencia(alvo, 8),
        this.servico.exercicios(alvo),
      ]);
      this.semanas.set(semanas);
      this.exercicios.set(exercicios);
      if (exercicios.length > 0) {
        void this.trocarExercicio(exercicios[0].id);
      }
    } catch {
      this.erro.set(true);
    } finally {
      this.carregando.set(false);
    }
  }

  protected async trocarExercicio(id: string): Promise<void> {
    const carga = ++this.carga;
    this.exercicioId.set(id);
    this.carregandoEvolucao.set(true);
    this.erroEvolucao.set(false);
    try {
      const pontos = await this.servico.evolucao(this.alvo(), id);
      if (carga !== this.carga) return;
      this.pontos.set(pontos);
    } catch {
      if (carga !== this.carga) return;
      this.pontos.set([]);
      this.erroEvolucao.set(true);
    } finally {
      if (carga === this.carga) this.carregandoEvolucao.set(false);
    }
  }
}
