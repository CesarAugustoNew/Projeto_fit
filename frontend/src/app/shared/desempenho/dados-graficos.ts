import { ChartData } from 'chart.js';
import { PontoEvolucao, SemanaAderencia } from './desempenho.model';

export type MedidaEvolucao = 'carga' | 'volume';

/** `yyyy-MM-dd` → `dd/MM`, sem passar por `Date` (evita deslocamento de fuso). */
function rotuloData(data: string): string {
  const [, mes, dia] = data.split('-');
  return `${dia}/${mes}`;
}

export function dadosEvolucao(pontos: PontoEvolucao[], medida: MedidaEvolucao): ChartData<'line'> {
  const carga = medida === 'carga';
  return {
    labels: pontos.map((p) => rotuloData(p.data)),
    datasets: [
      {
        label: carga ? 'Carga máxima (kg)' : 'Volume (kg)',
        data: pontos.map((p) => (carga ? p.cargaMaxima : p.volume)),
        spanGaps: true,
        tension: 0.2,
      },
    ],
  };
}

export function dadosAderencia(semanas: SemanaAderencia[]): ChartData<'bar'> {
  return {
    labels: semanas.map((s) => rotuloData(s.inicio)),
    datasets: [{ label: 'Aderência (%)', data: semanas.map((s) => s.percentual) }],
  };
}
