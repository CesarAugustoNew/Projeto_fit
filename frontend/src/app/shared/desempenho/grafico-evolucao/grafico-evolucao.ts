import { Component, computed, input, signal } from '@angular/core';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { ChartOptions } from 'chart.js';
import { BaseChartDirective, provideCharts, withDefaultRegisterables } from 'ng2-charts';
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
  readonly pontos = input.required<PontoEvolucao[]>();

  protected readonly medida = signal<MedidaEvolucao>('carga');
  protected readonly dados = computed(() => dadosEvolucao(this.pontos(), this.medida()));
  protected readonly semCarga = computed(
    () => this.medida() === 'carga' && this.pontos().every((p) => p.cargaMaxima === null),
  );
  protected readonly opcoes: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    scales: { y: { beginAtZero: true } },
  };
}
