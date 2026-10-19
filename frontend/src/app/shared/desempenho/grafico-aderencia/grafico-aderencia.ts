import { Component, computed, input } from '@angular/core';
import { ChartOptions } from 'chart.js';
import { BaseChartDirective, provideCharts, withDefaultRegisterables } from 'ng2-charts';
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
  readonly semanas = input.required<SemanaAderencia[]>();

  protected readonly dados = computed(() => dadosAderencia(this.semanas()));
  protected readonly media = computed(() => aderenciaMedia(this.semanas()));
  protected readonly temSemanaSemFicha = computed(() => this.semanas().some((s) => s.percentual === null));
  protected readonly opcoes: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    scales: { y: { min: 0, max: 100 } },
  };
}
