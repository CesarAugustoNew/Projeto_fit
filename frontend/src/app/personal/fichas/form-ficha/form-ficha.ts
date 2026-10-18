import { Component, OnInit, inject, input, signal } from '@angular/core';
import {
  AbstractControl,
  FormArray,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { confirmar } from '../../../shared/confirmacao/confirmacao-dialog';
import { EditorDivisao } from '../editor-divisao/editor-divisao';
import { FichaRequest, hojeLocal } from '../ficha.model';
import { FichaService } from '../ficha.service';
import { criarDivisaoForm, DivisaoForm, paraDivisaoRequest } from '../form-ficha.model';

const MAX_DIVISOES = 10;

@Component({
  selector: 'app-form-ficha',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    EditorDivisao,
  ],
  templateUrl: './form-ficha.html',
  styleUrl: './form-ficha.scss',
})
export class FormFicha implements OnInit {
  private readonly fichas = inject(FichaService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(NonNullableFormBuilder);

  readonly alunoId = input<string>();
  readonly id = input<string>();

  protected readonly frequencias = [1, 2, 3, 4, 5, 6, 7];
  protected readonly maxDivisoes = MAX_DIVISOES;
  protected readonly form = this.fb.group({
    nome: ['', [naoVazio, Validators.maxLength(120)]],
    objetivo: ['', Validators.maxLength(500)],
    frequenciaSemanal: [3, Validators.required],
    dataInicio: [hojeLocal(), Validators.required],
    dataFim: ['', fimNaoAnteriorAoInicio],
    divisoes: this.fb.array<DivisaoForm>([], aoMenosUma),
  });
  protected readonly enviando = signal(false);
  protected readonly carregando = signal(false);
  protected readonly tentouEnviar = signal(false);
  private readonly alunoDaFicha = signal<string | null>(null);
  private fichaAtivaDoAluno: { nome: string } | null = null;

  protected get editando(): boolean {
    return !!this.id();
  }

  protected get divisoes(): FormArray<DivisaoForm> {
    return this.form.controls.divisoes;
  }

  async ngOnInit(): Promise<void> {
    this.form.controls.dataInicio.valueChanges.subscribe(() =>
      this.form.controls.dataFim.updateValueAndValidity(),
    );
    const id = this.id();
    if (id) {
      await this.carregarEdicao(id);
      return;
    }
    this.divisoes.push(criarDivisaoForm(this.fb, { nome: 'Treino A' }));
    const alunoId = this.alunoId();
    if (alunoId) {
      try {
        const lista = await this.fichas.listarDoAluno(alunoId);
        this.fichaAtivaDoAluno = lista.find((f) => f.status === 'ATIVA') ?? null;
      } catch {
        // erroInterceptor mostra a mensagem; segue sem saber da ficha ativa
      }
    }
  }

  private async carregarEdicao(id: string): Promise<void> {
    this.carregando.set(true);
    this.form.disable();
    try {
      const f = await this.fichas.buscar(id);
      this.alunoDaFicha.set(f.alunoId);
      this.form.patchValue({
        nome: f.nome,
        objetivo: f.objetivo ?? '',
        frequenciaSemanal: f.frequenciaSemanal,
        dataInicio: f.dataInicio,
        dataFim: f.dataFim ?? '',
      });
      this.divisoes.clear();
      f.divisoes.forEach((d) => this.divisoes.push(criarDivisaoForm(this.fb, d)));
      this.form.enable();
      this.carregando.set(false);
    } catch {
      // erroInterceptor mostra a mensagem; sem dados nao ha o que editar
      await this.router.navigate(['/personal/alunos']);
    }
  }

  protected adicionarDivisao(): void {
    const n = this.divisoes.length;
    if (n >= MAX_DIVISOES) return;
    this.divisoes.push(criarDivisaoForm(this.fb, { nome: `Treino ${String.fromCharCode(65 + n)}` }));
    this.divisoes.markAsDirty();
  }

  protected removerDivisao(indice: number): void {
    this.divisoes.removeAt(indice);
    this.divisoes.markAsDirty();
    this.divisoes.markAsTouched();
  }

  protected moverDivisao(indice: number, delta: -1 | 1): void {
    const alvo = indice + delta;
    if (alvo < 0 || alvo >= this.divisoes.length) return;
    const atual = this.divisoes.at(indice);
    this.divisoes.removeAt(indice);
    this.divisoes.insert(alvo, atual);
    this.divisoes.markAsDirty();
  }

  protected async salvar(): Promise<void> {
    if (this.enviando() || this.carregando()) {
      return;
    }
    if (this.form.invalid) {
      this.tentouEnviar.set(true);
      this.form.markAllAsTouched();
      return;
    }
    this.tentouEnviar.set(false);
    this.enviando.set(true);
    try {
      const id = this.id();
      if (!id && this.fichaAtivaDoAluno) {
        const ok = await confirmar(this.dialog, {
          titulo: 'Substituir ficha ativa',
          mensagem: `A ficha "${this.fichaAtivaDoAluno.nome}" será encerrada e a nova passará a ser a ficha ativa.`,
          confirmar: 'Criar e encerrar a atual',
        });
        if (!ok) return;
      }
      const v = this.form.getRawValue();
      const dados: FichaRequest = {
        nome: v.nome.trim(),
        objetivo: v.objetivo.trim() || null,
        frequenciaSemanal: v.frequenciaSemanal,
        dataInicio: v.dataInicio,
        dataFim: v.dataFim || null,
        divisoes: this.divisoes.controls.map(paraDivisaoRequest),
      };
      const salva = id
        ? await this.fichas.atualizar(id, dados)
        : await this.fichas.criar(this.alunoId()!, dados);
      this.snackBar.open(id ? 'Ficha atualizada.' : 'Ficha criada.', undefined, { duration: 4000 });
      await this.router.navigate(['/personal/fichas', salva.id]);
    } catch {
      // erroInterceptor ja mostra a mensagem; permanece no formulario
    } finally {
      this.enviando.set(false);
    }
  }

  protected async cancelar(): Promise<void> {
    const aluno = this.editando ? this.alunoDaFicha() : (this.alunoId() ?? null);
    await this.router.navigate(aluno ? ['/personal/alunos', aluno] : ['/personal/alunos']);
  }
}

/** Obrigatorio ignorando espacos nas pontas. */
function naoVazio(control: AbstractControl<string>): ValidationErrors | null {
  return control.value.trim() === '' ? { required: true } : null;
}

function aoMenosUma(control: AbstractControl): ValidationErrors | null {
  return (control as FormArray).length > 0 ? null : { vazio: true };
}

/** Data de fim e opcional; se informada, nao pode ser anterior ao inicio. */
function fimNaoAnteriorAoInicio(control: AbstractControl<string>): ValidationErrors | null {
  const inicio = control.parent?.get('dataInicio')?.value as string | undefined;
  return control.value && inicio && control.value < inicio ? { fimAnterior: true } : null;
}
