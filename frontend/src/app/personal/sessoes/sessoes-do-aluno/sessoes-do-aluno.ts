import { Component, OnInit, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import {
  SessaoResumo,
  acrescentarPagina,
  duracaoMinutos,
  formatarDataHora,
} from '../../../shared/sessao/sessao.model';
import { SessaoService } from '../../../shared/sessao/sessao.service';

@Component({
  selector: 'app-sessoes-do-aluno',
  imports: [RouterLink, MatButtonModule],
  templateUrl: './sessoes-do-aluno.html',
  styleUrl: './sessoes-do-aluno.scss',
})
export class SessoesDoAluno implements OnInit {
  private readonly servico = inject(SessaoService);

  readonly alunoId = input.required<string>();

  protected readonly sessoes = signal<SessaoResumo[]>([]);
  protected readonly carregando = signal(true);
  protected readonly erro = signal(false);
  protected readonly temMais = signal(false);

  protected readonly formatarDataHora = formatarDataHora;
  protected readonly duracaoMinutos = duracaoMinutos;

  private proximaPagina = 0;

  ngOnInit(): void {
    void this.carregar();
  }

  protected carregarMais(): void {
    if (this.carregando()) return;
    void this.carregar();
  }

  private async carregar(): Promise<void> {
    this.carregando.set(true);
    this.erro.set(false);
    try {
      const pagina = await this.servico.doAluno(this.alunoId(), this.proximaPagina);
      this.sessoes.update((atuais) => acrescentarPagina(atuais, pagina.content));
      this.proximaPagina = pagina.page.number + 1;
      this.temMais.set(this.proximaPagina < pagina.page.totalPages);
    } catch {
      this.erro.set(true);
    } finally {
      this.carregando.set(false);
    }
  }
}
