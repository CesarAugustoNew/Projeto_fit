import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { firstValueFrom } from 'rxjs';
import { normalizarBusca } from '../../../shared/texto/normalizar-busca';
import { Exercicio, GRUPOS, GrupoMuscular, ROTULO_GRUPO } from '../../exercicios/exercicio.model';
import { ExercicioService } from '../../exercicios/exercicio.service';

@Component({
  selector: 'app-seletor-exercicio',
  imports: [FormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './seletor-exercicio.html',
  styleUrl: './seletor-exercicio.scss',
})
export class SeletorExercicio {
  private readonly servico = inject(ExercicioService);
  private readonly ref = inject<MatDialogRef<SeletorExercicio, Exercicio | null>>(MatDialogRef);

  protected readonly grupos = GRUPOS;
  protected readonly rotulos = ROTULO_GRUPO;
  protected readonly busca = signal('');
  protected readonly grupo = signal<GrupoMuscular | null>(null);
  protected readonly exercicios = signal<Exercicio[]>([]);
  protected readonly carregando = signal(true);
  protected readonly erro = signal(false);

  protected readonly visiveis = computed(() => {
    const termo = normalizarBusca(this.busca());
    const grupo = this.grupo();
    return this.exercicios().filter(
      (e) => (!grupo || e.grupoMuscular === grupo) && (!termo || normalizarBusca(e.nome).includes(termo)),
    );
  });

  constructor() {
    void this.carregar();
  }

  protected escolher(exercicio: Exercicio): void {
    this.ref.close(exercicio);
  }

  private async carregar(): Promise<void> {
    try {
      this.exercicios.set(await this.servico.listar({ ativo: true }));
    } catch {
      this.erro.set(true);
    } finally {
      this.carregando.set(false);
    }
  }
}

export async function escolherExercicio(dialog: MatDialog): Promise<Exercicio | null> {
  const celular = window.matchMedia?.('(max-width: 599.98px)').matches ?? false;
  const ref = dialog.open<SeletorExercicio, undefined, Exercicio | null>(
    SeletorExercicio,
    celular
      ? // dvh: a altura visível, sem ficar atrás da barra do navegador do celular
        { width: '100%', maxWidth: '100vw', height: '100dvh', maxHeight: '100dvh' }
      : { width: '560px', maxHeight: '90vh' },
  );
  return (await firstValueFrom(ref.afterClosed())) ?? null;
}
