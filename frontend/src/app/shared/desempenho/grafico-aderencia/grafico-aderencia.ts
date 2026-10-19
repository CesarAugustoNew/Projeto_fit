import { Component, computed, inject, input } from '@angular/core';
import { ChartData, ChartOptions } from 'chart.js';
import { BaseChartDirective, provideCharts, withDefaultRegisterables } from 'ng2-charts';
import { TemaService } from '../../../core/tema/tema.service';
import { coresGrafico, eixo } from '../cores-grafico';
import { dadosAderencia } from '../dados-graficos';
import { aderenciaMedia, SemanaAderencia } from '../desempenho.model';

@Component({
  selector: 'app-grafico-aderencia',
  imports: [BaseChartDirective],
  providers: [provideCharts(withDefaultRegisterables())],
  templateUrl: './grafico-aderencia.html',
  styleUrl: './grafico-aderencia.scss',
})
export class GraficoAderencia {
  private readonly tema = inject(TemaService);

  readonly semanas = input.required<SemanaAderencia[]>();

  private readonly cores = computed(() => coresGrafico(this.tema.escuro()));

  protected readonly dados = computed<ChartData<'bar'>>(() => {
    const dados = dadosAderencia(this.semanas());
    const cores = this.cores();
    return {
      ...dados,
      datasets: dados.datasets.map((d) => ({
        ...d,
        backgroundColor: cores.barra,
        borderRadius: 6,
        maxBarThickness: 28,
      })),
    };
  });
  protected readonly media = computed(() => aderenciaMedia(this.semanas()));
  protected readonly temSemanaSemFicha = computed(() => this.semanas().some((s) => s.percentual === null));
  protected readonly opcoes = computed<ChartOptions<'bar'>>(() => {
    const cores = this.cores();
    return {
      responsive: true,
      maintainAspectRatio: false,
      // Sem legenda: o título "Aderência semanal" e o eixo de 0 a 100 já dizem o que é a barra.
      plugins: { legend: { display: false } },
      scales: { x: eixo(cores, false), y: { ...eixo(cores), min: 0, max: 100 } },
    };
  });
}
