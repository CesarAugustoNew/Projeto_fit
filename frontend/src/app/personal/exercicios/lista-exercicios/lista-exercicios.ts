import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { Exercicio, GRUPOS, GrupoMuscular, ROTULO_GRUPO, linkSeguro } from '../exercicio.model';
import { ExercicioService } from '../exercicio.service';

type Filtro = 'ATIVOS' | 'INATIVOS';

@Component({
  selector: 'app-lista-exercicios',
  imports: [
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './lista-exercicios.html',
  styleUrl: './lista-exercicios.scss',
})
export class ListaExercicios {
  private readonly servico = inject(ExercicioService);
  private carga = 0;

  protected readonly grupos = GRUPOS;
  protected readonly rotulos = ROTULO_GRUPO;
  protected readonly linkSeguro = linkSeguro;
  protected readonly filtro = signal<Filtro>('ATIVOS');
  protected readonly grupo = signal<GrupoMuscular | null>(null);
  protected readonly busca = signal('');
  protected readonly exercicios = signal<Exercicio[]>([]);
  protected readonly carregando = signal(true);
  protected readonly erro = signal(false);

  protected readonly visiveis = computed(() => {
    const termo = this.busca().trim().toLowerCase();
    if (!termo) return this.exercicios();
    return this.exercicios().filter((e) => e.nome.toLowerCase().includes(termo));
  });

  constructor() {
    void this.carregar();
  }

  protected trocarFiltro(filtro: Filtro): void {
    this.filtro.set(filtro);
    void this.carregar();
  }

  protected trocarGrupo(grupo: GrupoMuscular | null): void {
    this.grupo.set(grupo);
    void this.carregar();
  }

  private async carregar(): Promise<void> {
    const carga = ++this.carga;
    this.carregando.set(true);
    this.erro.set(false);
    try {
      const grupo = this.grupo();
      const lista = await this.servico.listar({
        ativo: this.filtro() === 'ATIVOS',
        ...(grupo ? { grupo } : {}),
      });
      if (carga !== this.carga) return;
      this.exercicios.set(lista);
    } catch {
      if (carga !== this.carga) return;
      this.exercicios.set([]);
      this.erro.set(true);
    } finally {
      if (carga === this.carga) this.carregando.set(false);
    }
  }
}
