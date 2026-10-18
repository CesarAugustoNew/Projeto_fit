import { Component, HostListener, OnInit, inject, input, signal } from '@angular/core';
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
import { criarDivisaoForm, DivisaoForm, naoVazio, paraDivisaoRequest } from '../form-ficha.model';

const MAX_DIVISOES = 10;
const ERRO_CONSULTA_ATIVA = 'Não foi possível verificar a ficha ativa. Tente de novo.';

type FichaAtiva = { nome: string } | null;

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
  /** Consulta da ficha ativa do aluno (modo nova); aguardada no salvar para nunca pular a confirmação. */
  private fichaAtivaDoAluno: Promise<FichaAtiva> = Promise.resolve(null);
  private salvo = false;

  /** Alterações feitas no formulário que ainda não foram salvas com sucesso. */
  temAlteracoesNaoSalvas(): boolean {
    return this.form.dirty && !this.salvo;
  }

  @HostListener('window:beforeunload', ['$event'])
  protected aoSairDaPagina(event: BeforeUnloadEvent): void {
    if (this.temAlteracoesNaoSalvas()) {
      event.preventDefault();
    }
  }

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
    this.fichaAtivaDoAluno = this.consultarFichaAtiva();
    // erroInterceptor mostra a mensagem; o salvar tenta de novo antes de enviar
    this.fichaAtivaDoAluno.catch(() => undefined);
  }

  private async consultarFichaAtiva(): Promise<FichaAtiva> {
    const alunoId = this.alunoId();
    if (!alunoId) return null;
    const lista = await this.fichas.listarDoAluno(alunoId);
    return lista.find((f) => f.status === 'ATIVA') ?? null;
  }

  /** A ficha ativa do aluno; se a consulta inicial falhou, consulta de novo. Lança se falhar outra vez. */
  private async fichaAtiva(): Promise<FichaAtiva> {
    try {
      return await this.fichaAtivaDoAluno;
    } catch {
      this.fichaAtivaDoAluno = this.consultarFichaAtiva();
      this.fichaAtivaDoAluno.catch(() => undefined);
      return await this.fichaAtivaDoAluno;
    }
  }

  private async carregarEdicao(id: string): Promise<void> {
    this.carregando.set(true);
    this.form.disable();
    try {
      const f = await this.fichas.buscar(id);
      if (f.status === 'ENCERRADA') {
        this.snackBar.open('Esta ficha está encerrada e não pode ser editada.', undefined, { duration: 4000 });
        await this.router.navigate(['/personal/fichas', f.id], { replaceUrl: true });
        return;
      }
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
    if (this.divisoes.length >= MAX_DIVISOES) return;
    const usados = new Set(this.divisoes.controls.map((d) => d.controls.nome.value.trim()));
    const letra = 'ABCDEFGHIJ'.split('').find((l) => !usados.has(`Treino ${l}`));
    this.divisoes.push(criarDivisaoForm(this.fb, { nome: letra ? `Treino ${letra}` : '' }));
    this.divisoes.markAsDirty();
  }

  protected async removerDivisao(divisao: DivisaoForm): Promise<void> {
    const n = divisao.controls.itens.length;
    if (n > 0) {
      const ok = await confirmar(this.dialog, {
        titulo: 'Remover divisão',
        mensagem: `A divisão "${divisao.controls.nome.value.trim()}" e seus ${
          n === 1 ? '1 exercício' : `${n} exercícios`
        } serão removidos.`,
        confirmar: 'Remover',
      });
      if (!ok) return;
    }
    // Procura pelo controle: a lista pode ter mudado enquanto o diálogo estava aberto.
    const indice = this.divisoes.controls.indexOf(divisao);
    if (indice < 0) return;
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
      if (!id) {
        let ativa: FichaAtiva;
        try {
          ativa = await this.fichaAtiva();
        } catch {
          this.snackBar.open(ERRO_CONSULTA_ATIVA, undefined, { duration: 4000 });
          return;
        }
        if (ativa) {
          const ok = await confirmar(this.dialog, {
            titulo: 'Substituir ficha ativa',
            mensagem: `A ficha "${ativa.nome}" será encerrada e a nova passará a ser a ficha ativa.`,
            confirmar: 'Criar e encerrar a atual',
          });
          if (!ok) return;
        }
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
      this.salvo = true;
      this.snackBar.open(id ? 'Ficha atualizada.' : 'Ficha criada.', undefined, { duration: 4000 });
      // replaceUrl: voltar não reabre o formulário da ficha já salva
      await this.router.navigate(['/personal/fichas', salva.id], { replaceUrl: true });
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

function aoMenosUma(control: AbstractControl): ValidationErrors | null {
  return (control as FormArray).length > 0 ? null : { vazio: true };
}

/** Data de fim e opcional; se informada, nao pode ser anterior ao inicio. */
function fimNaoAnteriorAoInicio(control: AbstractControl<string>): ValidationErrors | null {
  const inicio = control.parent?.get('dataInicio')?.value as string | undefined;
  return control.value && inicio && control.value < inicio ? { fimAnterior: true } : null;
}
