import { ChangeDetectionStrategy, Component, input } from '@angular/core';

let sequencia = 0;

/** O raio da marca, em SVG (nítido em qualquer tamanho e nos dois modos). Decorativo por padrão. */
@Component({
  selector: 'app-logo-raio',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { 'aria-hidden': 'true', '[style.width.px]': 'tamanho()', '[style.height.px]': 'tamanho()' },
  styles: `
    :host { display: inline-block; flex: none; line-height: 0; }
    svg { width: 100%; height: 100%; }
    .brilho { fill: none; stroke: #fff; stroke-linecap: round; }
  `,
  template: `
    <svg viewBox="0 0 64 64" focusable="false">
      <defs>
        <linearGradient [attr.id]="corpo" x1="20" y1="6" x2="44" y2="60" gradientUnits="userSpaceOnUse">
          <stop offset="0" stop-color="#b3e1ff" />
          <stop offset=".45" stop-color="#74bdf7" />
          <stop offset="1" stop-color="#2f7fe8" />
        </linearGradient>
        <radialGradient [attr.id]="sombra" cx="32" cy="32" r="13" gradientUnits="userSpaceOnUse">
          <stop offset="0" stop-color="#1f6feb" stop-opacity=".4" />
          <stop offset="1" stop-color="#1f6feb" stop-opacity="0" />
        </radialGradient>
      </defs>
      <path [attr.fill]="'url(#' + corpo + ')'" [attr.d]="contorno" />
      <path [attr.fill]="'url(#' + sombra + ')'" [attr.d]="contorno" />
      <path d="M23.5 27.9L36.1 15.3" class="brilho" stroke-opacity=".75" stroke-width="1.8" />
      <path d="M39.7 38.3L34.9 44.4" class="brilho" stroke-opacity=".55" stroke-width="1.5" />
    </svg>
  `,
})
export class LogoRaio {
  readonly tamanho = input(32);

  // Ids únicos: vários logos na mesma página não disputam o mesmo degradê.
  protected readonly corpo = `raio-corpo-${++sequencia}`;
  protected readonly sombra = `raio-sombra-${sequencia}`;
  protected readonly contorno =
    'M44.59 6.24L44.91 6.26Q47.5 6.5 46.13 8.71L37.85 22.03Q36 25 39.37 25.96L44.23 27.35Q50 29 46.28 33.71' +
    'L27.11 57.96Q25.5 60 23.09 59.03L22.91 58.97Q20.5 58 21.57 55.63L28.06 41.19Q29.5 38 26.06 37.37' +
    'L18.9 36.07Q13 35 17.24 30.76L40.16 7.84Q42 6 44.59 6.24Z';
}
