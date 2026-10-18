import { Component, OnInit, inject, input, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { GRUPOS, GrupoMuscular, NovoExercicio, ROTULO_GRUPO } from '../exercicio.model';
import { ExercicioService } from '../exercicio.service';

@Component({
  selector: 'app-form-exercicio',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
  ],
  templateUrl: './form-exercicio.html',
  styleUrl: './form-exercicio.scss',
})
export class FormExercicio implements OnInit {
  private readonly exercicios = inject(ExercicioService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  readonly id = input<string>();

  protected readonly grupos = GRUPOS;
  protected readonly rotulo = ROTULO_GRUPO;
  protected readonly form = inject(FormBuilder).nonNullable.group({
    nome: ['', [naoVazio, Validators.maxLength(120)]],
    grupoMuscular: [null as GrupoMuscular | null, Validators.required],
    videoUrl: ['', [Validators.maxLength(500), linkHttp]],
    observacao: ['', Validators.maxLength(1000)],
    ativo: [true],
  });
  protected readonly enviando = signal(false);
  protected readonly carregando = signal(false);

  protected get editando(): boolean {
    return !!this.id();
  }

  async ngOnInit(): Promise<void> {
    const id = this.id();
    if (!id) {
      return;
    }
    this.carregando.set(true);
    this.form.disable();
    try {
      const e = await this.exercicios.buscar(id);
      this.form.reset({
        nome: e.nome,
        grupoMuscular: e.grupoMuscular,
        videoUrl: e.videoUrl ?? '',
        observacao: e.observacao ?? '',
        ativo: e.ativo,
      });
      this.form.enable();
      this.carregando.set(false);
    } catch {
      // erroInterceptor mostra a mensagem; sem dados nao ha o que editar
      await this.router.navigate(['/personal/exercicios']);
    }
  }

  protected async salvar(): Promise<void> {
    if (this.enviando() || this.carregando()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    try {
      const v = this.form.getRawValue();
      const dados: NovoExercicio = {
        nome: v.nome.trim(),
        grupoMuscular: v.grupoMuscular!,
        videoUrl: vazioParaNull(v.videoUrl),
        observacao: vazioParaNull(v.observacao),
      };
      const id = this.id();
      if (id) {
        await this.exercicios.atualizar(id, { ...dados, ativo: v.ativo });
        this.snackBar.open('Exercício atualizado.', undefined, { duration: 4000 });
      } else {
        await this.exercicios.criar(dados);
        this.snackBar.open('Exercício criado.', undefined, { duration: 4000 });
      }
      await this.router.navigate(['/personal/exercicios']);
    } catch {
      // erroInterceptor ja mostra a mensagem; permanece no formulario
    } finally {
      this.enviando.set(false);
    }
  }

  protected async cancelar(): Promise<void> {
    await this.router.navigate(['/personal/exercicios']);
  }
}

function vazioParaNull(valor: string): string | null {
  const v = valor.trim();
  return v === '' ? null : v;
}

/** Obrigatorio ignorando espacos nas pontas. */
function naoVazio(control: AbstractControl<string>): ValidationErrors | null {
  return control.value.trim() === '' ? { required: true } : null;
}

/** Vazio e valido (opcional); informado precisa ser http(s). */
function linkHttp(control: AbstractControl<string>): ValidationErrors | null {
  const v = control.value.trim();
  return v === '' || /^https?:\/\/\S+$/i.test(v) ? null : { linkHttp: true };
}
