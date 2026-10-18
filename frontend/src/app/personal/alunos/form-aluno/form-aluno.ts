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
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { DadosAluno } from '../aluno.model';
import { AlunoService } from '../aluno.service';

@Component({
  selector: 'app-form-aluno',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './form-aluno.html',
  styleUrl: './form-aluno.scss',
})
export class FormAluno implements OnInit {
  private readonly alunos = inject(AlunoService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  readonly id = input<string>();

  protected readonly form = inject(FormBuilder).nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email]],
    telefone: ['', Validators.maxLength(20)],
    dataNascimento: ['', dataNoPassado],
    objetivo: ['', Validators.maxLength(500)],
    observacoes: ['', Validators.maxLength(2000)],
  });
  protected readonly enviando = signal(false);
  /** Data maxima do seletor: ontem, no fuso local. */
  protected readonly ontem = ontemLocal();

  protected get editando(): boolean {
    return !!this.id();
  }

  async ngOnInit(): Promise<void> {
    const id = this.id();
    if (!id) {
      return;
    }
    this.form.controls.email.disable();
    try {
      const a = await this.alunos.buscar(id);
      this.form.reset({
        nome: a.nome,
        email: a.email,
        telefone: a.telefone ?? '',
        dataNascimento: a.dataNascimento ?? '',
        objetivo: a.objetivo ?? '',
        observacoes: a.observacoes ?? '',
      });
    } catch {
      // erroInterceptor mostra a mensagem; sem dados nao ha o que editar
      await this.router.navigate(['/personal/alunos']);
    }
  }

  protected async salvar(): Promise<void> {
    if (this.enviando()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    try {
      const v = this.form.getRawValue();
      const dados: DadosAluno = {
        nome: v.nome.trim(),
        telefone: vazioParaNull(v.telefone),
        dataNascimento: vazioParaNull(v.dataNascimento),
        objetivo: vazioParaNull(v.objetivo),
        observacoes: vazioParaNull(v.observacoes),
      };
      const id = this.id();
      if (id) {
        await this.alunos.atualizar(id, dados);
        this.snackBar.open('Dados do aluno atualizados.', undefined, { duration: 4000 });
        await this.router.navigate(['/personal/alunos', id]);
      } else {
        const email = v.email.trim();
        const criado = await this.alunos.criar({ ...dados, email });
        this.snackBar.open(`Convite enviado para ${email}.`, undefined, { duration: 4000 });
        await this.router.navigate(['/personal/alunos', criado.id]);
      }
    } catch {
      // erroInterceptor ja mostra a mensagem; permanece no formulario
    } finally {
      this.enviando.set(false);
    }
  }

  protected async cancelar(): Promise<void> {
    const id = this.id();
    await this.router.navigate(id ? ['/personal/alunos', id] : ['/personal/alunos']);
  }
}

function vazioParaNull(valor: string): string | null {
  const v = valor.trim();
  return v === '' ? null : v;
}

/** `yyyy-MM-dd` no fuso local (`toISOString()` usaria UTC e erraria o dia a noite). */
function dataLocal(d: Date): string {
  const mes = String(d.getMonth() + 1).padStart(2, '0');
  const dia = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mes}-${dia}`;
}

function ontemLocal(): string {
  const hoje = new Date();
  return dataLocal(new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate() - 1));
}

/** Rejeita hoje ou datas futuras; vazio e valido (campo opcional). */
function dataNoPassado(control: AbstractControl<string>): ValidationErrors | null {
  const v = control.value;
  return v && v >= dataLocal(new Date()) ? { dataNoPassado: true } : null;
}
