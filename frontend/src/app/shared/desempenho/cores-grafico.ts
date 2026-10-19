/** Cores dos gráficos por modo. O Chart.js desenha em canvas e não lê variáveis CSS. */
export interface CoresGrafico {
  destaque: string;
  preenchimento: string;
  barra: string;
  grade: string;
  texto: string;
}

const CLARO: CoresGrafico = {
  destaque: '#1f6feb',
  preenchimento: 'rgba(31, 111, 235, 0.12)',
  barra: '#3b82f6',
  grade: 'rgba(11, 28, 49, 0.08)',
  texto: '#4f5f78',
};

const ESCURO: CoresGrafico = {
  destaque: '#5aa2ff',
  preenchimento: 'rgba(90, 162, 255, 0.18)',
  barra: '#4aa3ff',
  grade: 'rgba(183, 199, 228, 0.12)',
  texto: '#9cacc8',
};

export function coresGrafico(escuro: boolean): CoresGrafico {
  return escuro ? ESCURO : CLARO;
}

/** Grade e rótulos de um eixo cartesiano nas cores do modo atual (`grade: false` esconde as linhas). */
export function eixo(cores: CoresGrafico, grade = true) {
  return {
    grid: { display: grade, color: cores.grade },
    border: { display: false },
    ticks: { color: cores.texto },
  };
}
