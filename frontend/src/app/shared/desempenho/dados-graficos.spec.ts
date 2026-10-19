import { PontoEvolucao, SemanaAderencia } from './desempenho.model';
import { dadosAderencia, dadosEvolucao } from './dados-graficos';

const ponto = (data: string, cargaMaxima: number | null, volume: number): PontoEvolucao => ({
  data,
  sessaoId: 's-' + data,
  cargaMaxima,
  volume,
});

const semana = (inicio: string, percentual: number | null): SemanaAderencia => ({
  inicio,
  fim: inicio,
  realizados: 0,
  planejado: 0,
  percentual,
});

describe('dados-graficos', () => {
  it('dadosEvolucao em carga usa cargaMaxima e rótulos dd/MM', () => {
    const dados = dadosEvolucao([ponto('2026-10-05', 40, 800), ponto('2026-10-12', 42.5, 900)], 'carga');
    expect(dados.labels).toEqual(['05/10', '12/10']);
    expect(dados.datasets[0].data).toEqual([40, 42.5]);
    expect(dados.datasets[0].label).toBe('Carga máxima (kg)');
  });

  it('pontoSemCargaViraLacuna', () => {
    const dados = dadosEvolucao([ponto('2026-10-05', 40, 800), ponto('2026-10-12', null, 0)], 'carga');
    expect(dados.datasets[0].data).toEqual([40, null]);
    expect(dados.datasets[0].spanGaps).toBe(true);
  });

  it('dadosEvolucao em volume usa volume', () => {
    const dados = dadosEvolucao([ponto('2026-10-05', 40, 800), ponto('2026-10-12', null, 650)], 'volume');
    expect(dados.datasets[0].data).toEqual([800, 650]);
    expect(dados.datasets[0].label).toBe('Volume (kg)');
  });

  it('dadosAderencia usa percentual e nulo vira 0', () => {
    const dados = dadosAderencia([semana('2026-10-05', 100), semana('2026-10-12', null), semana('2026-10-19', 50)]);
    expect(dados.labels).toEqual(['05/10', '12/10', '19/10']);
    expect(dados.datasets[0].data).toEqual([100, 0, 50]);
    expect(dados.datasets[0].label).toBe('Aderência (%)');
  });
});
