import { TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { Exercicio } from '../../exercicios/exercicio.model';
import { criarDivisaoForm, criarItemForm, DivisaoForm, paraDivisaoRequest } from '../form-ficha.model';
import { EditorDivisao, ESCOLHER_EXERCICIO } from './editor-divisao';

const supino: Exercicio = {
  id: 'e9',
  nome: 'Supino reto',
  grupoMuscular: 'PEITO',
  videoUrl: null,
  observacao: null,
  ativo: true,
};

describe('EditorDivisao', () => {
  const fb = new FormBuilder().nonNullable;

  function item(n: number) {
    return criarItemForm(fb, { exercicioId: `e${n}`, exercicioNome: `Exercicio ${n}`, grupoMuscular: 'COSTAS' });
  }

  const escolherExercicio = vi.fn<() => Promise<Exercicio | null>>();

  function montar(itens = 0) {
    TestBed.configureTestingModule({ providers: [{ provide: ESCOLHER_EXERCICIO, useValue: escolherExercicio }] });
    const form: DivisaoForm = criarDivisaoForm(fb, { nome: 'A' });
    for (let i = 1; i <= itens; i++) form.controls.itens.push(item(i));
    const fixture = TestBed.createComponent(EditorDivisao);
    fixture.componentRef.setInput('form', form);
    fixture.componentRef.setInput('posicao', 0);
    fixture.detectChanges();
    return { fixture, form, el: fixture.nativeElement as HTMLElement };
  }

  const botao = (el: HTMLElement, texto: string, i = 0) =>
    Array.from(el.querySelectorAll('button')).filter(
      (b) => b.textContent?.trim() === texto || b.getAttribute('aria-label') === texto,
    )[i] as HTMLButtonElement;
  const nomes = (form: DivisaoForm) => form.controls.itens.controls.map((c) => c.controls.exercicioNome.value);

  it('adicionar exercício acrescenta item com padrões 3 x 10 e 60 s', async () => {
    escolherExercicio.mockResolvedValue(supino);
    const { fixture, form, el } = montar();
    botao(el, 'Adicionar exercício').click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(form.controls.itens.at(0).getRawValue()).toMatchObject({
      exercicioId: 'e9',
      exercicioNome: 'Supino reto',
      series: 3,
      repeticoes: '10',
      descansoSeg: 60,
    });
    expect(el.textContent).toContain('Supino reto');
  });

  it('cancelar o seletor não acrescenta nada', async () => {
    escolherExercicio.mockResolvedValue(null);
    const { fixture, form, el } = montar();
    botao(el, 'Adicionar exercício').click();
    await fixture.whenStable();
    expect(form.controls.itens.length).toBe(0);
  });

  it('duplo toque em Adicionar/Trocar exercício abre um só seletor', async () => {
    let fechar!: (e: Exercicio | null) => void;
    escolherExercicio.mockReset();
    escolherExercicio.mockReturnValue(new Promise((r) => (fechar = r)));
    const { fixture, form, el } = montar(1);
    botao(el, 'Adicionar exercício').click();
    botao(el, 'Adicionar exercício').click();
    botao(el, 'Trocar exercício').click();
    expect(escolherExercicio).toHaveBeenCalledTimes(1);
    fechar(supino);
    await fixture.whenStable();
    expect(form.controls.itens.length).toBe(2);

    escolherExercicio.mockResolvedValue(null);
    botao(el, 'Trocar exercício').click();
    await fixture.whenStable();
    expect(escolherExercicio).toHaveBeenCalledTimes(2);
  });

  it('subir e descer reordenam os itens', () => {
    const { fixture, form, el } = montar(2);
    expect(botao(el, 'Subir', 0).disabled).toBe(true);
    expect(botao(el, 'Descer', 1).disabled).toBe(true);
    botao(el, 'Descer', 0).click();
    fixture.detectChanges();
    expect(nomes(form)).toEqual(['Exercicio 2', 'Exercicio 1']);
    botao(el, 'Subir', 1).click();
    fixture.detectChanges();
    expect(nomes(form)).toEqual(['Exercicio 1', 'Exercicio 2']);
  });

  it('remover exercício tira o item', () => {
    const { fixture, form, el } = montar(2);
    botao(el, 'Remover exercício', 0).click();
    fixture.detectChanges();
    expect(nomes(form)).toEqual(['Exercicio 2']);
  });

  it('trocar exercício substitui o exercício do item', async () => {
    escolherExercicio.mockResolvedValue(supino);
    const { form, fixture, el } = montar(1);
    botao(el, 'Trocar exercício').click();
    await fixture.whenStable();
    expect(form.controls.itens.at(0).getRawValue()).toMatchObject({
      exercicioId: 'e9',
      exercicioNome: 'Supino reto',
      grupoMuscular: 'PEITO',
    });
  });

  it('repetições inválidas mostram erro', () => {
    const { fixture, form, el } = montar(1);
    const c = el.querySelector('[formControlName="repeticoes"]') as HTMLInputElement;
    c.value = 'dez';
    c.dispatchEvent(new Event('input'));
    form.controls.itens.at(0).controls.repeticoes.markAsTouched();
    fixture.detectChanges();
    expect(el.textContent).toContain('Use um número (ex.: 10) ou uma faixa (ex.: 8-12).');
  });

  it('valores numéricos inválidos mostram as mensagens de cada campo', () => {
    const { fixture, form, el } = montar(1);
    const i = form.controls.itens.at(0).controls;
    i.series.setValue(2.5);
    i.cargaKg.setValue(1.234);
    i.descansoSeg.setValue(30.5);
    form.markAllAsTouched();
    fixture.detectChanges();
    expect(el.textContent).toContain('Informe o número de séries (1 a 20).');
    expect(el.textContent).toContain('Informe uma carga entre 0 e 9999,99 kg.');
    expect(el.textContent).toContain('Informe o descanso em segundos (0 a 3600).');
  });

  it('nome da divisão só com espaços mostra erro', () => {
    const { fixture, form, el } = montar(1);
    form.controls.nome.setValue('   ');
    form.controls.nome.markAsTouched();
    fixture.detectChanges();
    expect(el.textContent).toContain('Informe o nome da divisão.');
  });

  it('cargaDecimalEhEnviadaComoNumero', () => {
    const { fixture, form, el } = montar(1);
    const c = el.querySelector('[formControlName="cargaKg"]') as HTMLInputElement;
    c.value = '22.5';
    c.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(paraDivisaoRequest(form).itens[0].cargaKg).toBe(22.5);
  });

  it('divisão sem exercícios mostra Adicione pelo menos um exercício.', () => {
    const { fixture, form, el } = montar();
    form.markAllAsTouched();
    fixture.detectChanges();
    expect(el.textContent).toContain('Adicione pelo menos um exercício.');
  });

  it('paraDivisaoRequest limpa textos e vazios', () => {
    const { form } = montar(1);
    form.controls.nome.setValue('  Treino A ');
    const i = form.controls.itens.at(0).controls;
    i.observacao.setValue('  ');
    i.cargaKg.setValue(null);
    i.descansoSeg.setValue(null);
    expect(paraDivisaoRequest(form)).toEqual({
      id: null,
      nome: 'Treino A',
      itens: [
        { id: null, exercicioId: 'e1', series: 3, repeticoes: '10', cargaKg: null, descansoSeg: null, observacao: null },
      ],
    });
  });
});
