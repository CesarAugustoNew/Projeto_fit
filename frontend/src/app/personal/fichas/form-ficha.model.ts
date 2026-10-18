import {
  AbstractControl,
  FormArray,
  FormControl,
  FormGroup,
  NonNullableFormBuilder,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { GrupoMuscular } from '../exercicios/exercicio.model';
import { Divisao, DivisaoRequest, ItemDivisao } from './ficha.model';

export type ItemForm = FormGroup<{
  id: FormControl<string | null>;
  exercicioId: FormControl<string>;
  exercicioNome: FormControl<string>;
  grupoMuscular: FormControl<GrupoMuscular>;
  series: FormControl<number | null>;
  repeticoes: FormControl<string>;
  cargaKg: FormControl<number | null>;
  descansoSeg: FormControl<number | null>;
  observacao: FormControl<string>;
}>;

export type DivisaoForm = FormGroup<{
  id: FormControl<string | null>;
  nome: FormControl<string>;
  itens: FormArray<ItemForm>;
}>;

/** Só dígitos: recusa casas decimais (o back exige inteiros). */
const INTEIRO = /^\d+$/;
/** Até 2 casas decimais, como o numeric(6,2) do back. */
const DUAS_CASAS = /^\d+(\.\d{1,2})?$/;

export function criarItemForm(
  fb: NonNullableFormBuilder,
  valor: Partial<ItemDivisao> & { exercicioId: string; exercicioNome: string; grupoMuscular: GrupoMuscular },
): ItemForm {
  return fb.group({
    id: fb.control<string | null>(valor.id ?? null),
    exercicioId: fb.control(valor.exercicioId),
    exercicioNome: fb.control(valor.exercicioNome),
    grupoMuscular: fb.control<GrupoMuscular>(valor.grupoMuscular),
    series: fb.control<number | null>(valor.series ?? 3, [
      Validators.required,
      Validators.min(1),
      Validators.max(20),
      Validators.pattern(INTEIRO),
    ]),
    repeticoes: fb.control(valor.repeticoes ?? '10', [
      Validators.required,
      Validators.pattern(/^\d{1,3}(-\d{1,3})?$/),
    ]),
    cargaKg: fb.control<number | null>(valor.cargaKg ?? null, [
      Validators.min(0),
      Validators.max(9999.99),
      Validators.pattern(DUAS_CASAS),
    ]),
    descansoSeg: fb.control<number | null>(valor.descansoSeg === undefined ? 60 : valor.descansoSeg, [
      Validators.min(0),
      Validators.max(3600),
      Validators.pattern(INTEIRO),
    ]),
    observacao: fb.control(valor.observacao ?? '', [Validators.maxLength(500)]),
  });
}

/** Obrigatório ignorando espaços nas pontas. */
export function naoVazio(control: AbstractControl<string>): ValidationErrors | null {
  return control.value.trim() === '' ? { required: true } : null;
}

function aoMenosUm(controle: AbstractControl): ValidationErrors | null {
  return (controle as FormArray).length > 0 ? null : { vazio: true };
}

export function criarDivisaoForm(fb: NonNullableFormBuilder, valor?: Partial<Divisao>): DivisaoForm {
  return fb.group({
    id: fb.control<string | null>(valor?.id ?? null),
    nome: fb.control(valor?.nome ?? '', [naoVazio, Validators.maxLength(60)]),
    itens: fb.array<ItemForm>((valor?.itens ?? []).map((i) => criarItemForm(fb, i)), aoMenosUm),
  });
}

/** Campos numéricos vazios chegam como null (ou '' em alguns navegadores). */
function numeroOuNull(v: number | string | null): number | null {
  return v === null || v === '' ? null : Number(v);
}

export function paraDivisaoRequest(form: DivisaoForm): DivisaoRequest {
  const v = form.getRawValue();
  return {
    id: v.id,
    nome: v.nome.trim(),
    itens: v.itens.map((i) => ({
      id: i.id,
      exercicioId: i.exercicioId,
      series: Number(i.series),
      repeticoes: i.repeticoes.trim(),
      cargaKg: numeroOuNull(i.cargaKg),
      descansoSeg: numeroOuNull(i.descansoSeg),
      observacao: i.observacao.trim() || null,
    })),
  };
}
