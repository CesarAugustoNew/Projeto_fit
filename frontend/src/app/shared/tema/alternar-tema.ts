import {
  ChangeDetectionStrategy,
  Component,
  booleanAttribute,
  computed,
  inject,
  input,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TemaService } from '../../core/tema/tema.service';

/** Botão que troca entre modo claro e escuro. `comTexto` mostra o rótulo ao lado do ícone. */
@Component({
  selector: 'app-alternar-tema',
  imports: [MatButtonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (comTexto()) {
      <button type="button" class="com-texto" (click)="tema.alternar()">
        <mat-icon fontSet="material-symbols-outlined">{{ icone() }}</mat-icon>
        <span>{{ rotulo() }}</span>
      </button>
    } @else {
      <button mat-icon-button type="button" [attr.aria-label]="rotulo()" (click)="tema.alternar()">
        <mat-icon fontSet="material-symbols-outlined">{{ icone() }}</mat-icon>
      </button>
    }
  `,
  styles: `
    :host { display: contents; }
    .com-texto {
      display: flex;
      align-items: center;
      gap: 12px;
      width: 100%;
      min-height: 48px;
      padding: 0 16px;
      border: 0;
      border-radius: 12px;
      background: none;
      color: var(--mat-sys-on-surface-variant);
      font: var(--mat-sys-label-large);
      cursor: pointer;
      text-align: left;
    }
    .com-texto:hover { background: var(--fit-brilho); color: var(--mat-sys-on-surface); }
    .com-texto:focus-visible { outline: 2px solid var(--mat-sys-primary); outline-offset: -2px; }
  `,
})
export class AlternarTema {
  protected readonly tema = inject(TemaService);

  readonly comTexto = input(false, { transform: booleanAttribute });

  protected readonly icone = computed(() => (this.tema.escuro() ? 'light_mode' : 'dark_mode'));
  protected readonly rotulo = computed(() => (this.tema.escuro() ? 'Usar modo claro' : 'Usar modo escuro'));
}
