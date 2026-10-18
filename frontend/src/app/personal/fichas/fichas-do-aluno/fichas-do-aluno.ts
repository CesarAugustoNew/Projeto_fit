import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { FichaResumo, formatarData } from '../ficha.model';
import { FichaService } from '../ficha.service';

@Component({
  selector: 'app-fichas-do-aluno',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './fichas-do-aluno.html',
  styleUrl: './fichas-do-aluno.scss',
})
export class FichasDoAluno implements OnInit {
  private readonly fichas = inject(FichaService);

  readonly alunoId = input.required<string>();
  readonly alunoAtivo = input(true);

  protected readonly formatarData = formatarData;
  protected readonly lista = signal<FichaResumo[]>([]);
  protected readonly carregando = signal(true);
  protected readonly erro = signal(false);

  protected readonly ativa = computed(() => this.lista().find((f) => f.status === 'ATIVA') ?? null);
  protected readonly anteriores = computed(() => this.lista().filter((f) => f.status !== 'ATIVA'));

  async ngOnInit(): Promise<void> {
    try {
      this.lista.set(await this.fichas.listarDoAluno(this.alunoId()));
    } catch {
      this.erro.set(true);
    } finally {
      this.carregando.set(false);
    }
  }

  protected rotuloDivisoes(n: number): string {
    return n === 1 ? '1 divisão' : `${n} divisões`;
  }
}
