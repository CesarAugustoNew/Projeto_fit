import { Component, computed, inject, input, signal } from '@angular/core';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { ChartData, ChartOptions } from 'chart.js';
import { BaseChartDirective, provideCharts, withDefaultRegisterables } from 'ng2-charts';
import { TemaService } from '../../../core/tema/tema.service';
import { coresGrafico, eixo } from '../cores-grafico';
import { dadosEvolucao, MedidaEvolucao } from '../dados-graficos';
import { PontoEvolucao } from '../desempenho.model';

@Component({
  selector: 'app-grafico-evolucao',
  imports: [BaseChartDirective, MatButtonToggleModule],
  providers: [provideCharts(withDefaultRegisterables())],
  templateUrl: './grafico-evolucao.html',
  styleUrl: './grafico-evolucao.scss',
})
export class GraficoEvolucao {
  private readonly tema = inject(TemaService);

  readonly pontos = input.required<PontoEvolucao[]>();

  protected readonly medida = signal<MedidaEvolucao>('carga');
  private readonly cores = computed(() => coresGrafico(this.tema.escuro()));

  protected readonly dados = computed<ChartData<'line'>>(() => {
    const dados = dadosEvolucao(this.pontos(), this.medida());
    const cores = this.cores();
    return {
      ...dados,
      datasets: dados.datasets.map((d) => ({
        ...d,
        borderColor: cores.destaque,
        backgroundColor: cores.preenchimento,
        pointBackgroundColor: cores.destaque,
        pointRadius: 3,
        borderWidth: 2.5,
        fill: 'origin',
      })),
    };
  });
  protected readonly semCarga = computed(
    () => this.medida() === 'carga' && this.pontos().every((p) => p.cargaMaxima === null),
  );
  protected readonly opcoes = computed<ChartOptions<'line'>>(() => {
    const cores = this.cores();
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { labels: { color: cores.texto } } },
      scales: { x: eixo(cores, false), y: { ...eixo(cores), beginAtZero: true } },
    };
  });
}
