import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { Aluno, ROTULO_STATUS } from '../aluno.model';
import { AlunoService } from '../aluno.service';

type Filtro = 'ATIVOS' | 'INATIVOS';

@Component({
  selector: 'app-lista-alunos',
  imports: [
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  templateUrl: './lista-alunos.html',
  styleUrl: './lista-alunos.scss',
})
export class ListaAlunos {
  private readonly servico = inject(AlunoService);
  private carga = 0;

  protected readonly rotulos = ROTULO_STATUS;
  protected readonly filtro = signal<Filtro>('ATIVOS');
  protected readonly busca = signal('');
  protected readonly alunos = signal<Aluno[]>([]);
  protected readonly carregando = signal(true);
  protected readonly erro = signal(false);

  protected readonly visiveis = computed(() => {
    const termo = this.busca().trim().toLowerCase();
    if (!termo) return this.alunos();
    return this.alunos().filter(
      (a) => a.nome.toLowerCase().includes(termo) || a.email.toLowerCase().includes(termo),
    );
  });

  constructor() {
    void this.carregar();
  }

  protected trocarFiltro(filtro: Filtro): void {
    this.filtro.set(filtro);
    void this.carregar();
  }

  private async carregar(): Promise<void> {
    const carga = ++this.carga;
    this.carregando.set(true);
    this.erro.set(false);
    try {
      const lista =
        this.filtro() === 'INATIVOS' ? await this.servico.listar('INATIVO') : await this.servico.listar();
      if (carga !== this.carga) return;
      this.alunos.set(lista);
    } catch {
      if (carga !== this.carga) return;
      this.alunos.set([]);
      this.erro.set(true);
    } finally {
      if (carga === this.carga) this.carregando.set(false);
    }
  }
}
