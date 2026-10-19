import { Component } from '@angular/core';
import { AlvoDesempenho } from '../../shared/desempenho/desempenho.model';
import { PainelDesempenho } from '../../shared/desempenho/painel-desempenho/painel-desempenho';

@Component({
  selector: 'app-evolucao',
  imports: [PainelDesempenho],
  template: `
    <section class="evolucao">
      <h1>Minha evolução</h1>
      <app-painel-desempenho [alvo]="alvo" />
    </section>
  `,
  styles: `
    .evolucao {
      padding: 16px;
      max-width: 720px;
      margin: 0 auto;
    }
    h1 {
      margin: 0;
      font-size: 1.5rem;
    }
  `,
})
export class Evolucao {
  protected readonly alvo: AlvoDesempenho = { tipo: 'aluno-logado' };
}
