import { Component, InjectionToken, inject, input, output } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Exercicio, ROTULO_GRUPO } from '../../exercicios/exercicio.model';
import { criarItemForm, DivisaoForm, ItemForm } from '../form-ficha.model';
import { escolherExercicio } from '../seletor-exercicio/seletor-exercicio';

/** Ponto de substituição do seletor de exercício (trocado nos testes). */
export const ESCOLHER_EXERCICIO = new InjectionToken<() => Promise<Exercicio | null>>('ESCOLHER_EXERCICIO', {
  factory: () => {
    const dialog = inject(MatDialog);
    return () => escolherExercicio(dialog);
  },
});

@Component({
  selector: 'app-editor-divisao',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule],
  templateUrl: './editor-divisao.html',
  styleUrl: './editor-divisao.scss',
})
export class EditorDivisao {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly escolher = inject(ESCOLHER_EXERCICIO);

  readonly form = input.required<DivisaoForm>();
  readonly posicao = input.required<number>();
  readonly remover = output<void>();
  readonly subir = output<void>();
  readonly descer = output<void>();

  protected readonly rotulo = ROTULO_GRUPO;

  protected async adicionar(): Promise<void> {
    const exercicio = await this.escolher();
    if (!exercicio) return;
    const itens = this.form().controls.itens;
    itens.push(
      criarItemForm(this.fb, {
        exercicioId: exercicio.id,
        exercicioNome: exercicio.nome,
        grupoMuscular: exercicio.grupoMuscular,
      }),
    );
    itens.markAsDirty();
  }

  protected async trocar(item: ItemForm): Promise<void> {
    const exercicio = await this.escolher();
    if (!exercicio) return;
    item.patchValue({
      exercicioId: exercicio.id,
      exercicioNome: exercicio.nome,
      grupoMuscular: exercicio.grupoMuscular,
    });
    item.markAsDirty();
  }

  protected mover(indice: number, delta: -1 | 1): void {
    const itens = this.form().controls.itens;
    const alvo = indice + delta;
    if (alvo < 0 || alvo >= itens.length) return;
    const item = itens.at(indice);
    itens.removeAt(indice);
    itens.insert(alvo, item);
    itens.markAsDirty();
  }

  protected removerItem(indice: number): void {
    const itens = this.form().controls.itens;
    itens.removeAt(indice);
    itens.markAsDirty();
    itens.markAsTouched();
  }
}
