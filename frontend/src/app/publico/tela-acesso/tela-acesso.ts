import { ChangeDetectionStrategy, Component, ViewEncapsulation, input } from '@angular/core';
import { LogoRaio } from '../../shared/marca/logo-raio';
import { AlternarTema } from '../../shared/tema/alternar-tema';

/**
 * Moldura das telas públicas (login, senha, indisponível): marca no topo e um cartão com o conteúdo.
 * Sem encapsulamento para estilizar o formulário projetado; tudo fica sob `.tela-acesso`.
 */
@Component({
  selector: 'app-tela-acesso',
  imports: [LogoRaio, AlternarTema],
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  templateUrl: './tela-acesso.html',
  styleUrl: './tela-acesso.scss',
})
export class TelaAcesso {
  readonly titulo = input.required<string>();
  readonly subtitulo = input<string>();
}
