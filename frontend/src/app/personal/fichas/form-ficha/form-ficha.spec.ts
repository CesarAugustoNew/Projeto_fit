import { NonNullableFormBuilder } from '@angular/forms';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { Ficha, FichaResumo } from '../ficha.model';
import { FichaService } from '../ficha.service';
import { criarItemForm, DivisaoForm } from '../form-ficha.model';
import { ESCOLHER_EXERCICIO } from '../editor-divisao/editor-divisao';
import { FormFicha } from './form-ficha';

const ficha = (extra: Partial<Ficha> = {}): Ficha => ({
  id: 'f1',
  alunoId: 'a1',
  nome: 'Hipertrofia',
  objetivo: null,
  frequenciaSemanal: 4,
  dataInicio: '2026-10-01',
  dataFim: null,
  status: 'ATIVA',
  vencida: false,
  encerradaEm: null,
  divisoes: [
    {
      id: 'd1',
      nome: 'Treino A',
      ordem: 1,
      itens: [
        {
          id: 'i1',
          exercicioId: 'e1',
          exercicioNome: 'Supino',
          grupoMuscular: 'PEITO',
          ordem: 1,
          series: 4,
          repeticoes: '8-12',
          cargaKg: 20,
          descansoSeg: 60,
          observacao: null,
        },
      ],
    },
  ],
  ...extra,
});

const resumo = (extra: Partial<FichaResumo> = {}): FichaResumo => ({
  id: 'f0',
  nome: 'Antiga',
  status: 'ATIVA',
  dataInicio: '2026-09-01',
  dataFim: null,
  frequenciaSemanal: 3,
  quantidadeDivisoes: 1,
  vencida: false,
  encerradaEm: null,
  ...extra,
});

interface Opcoes {
  alunoId?: string;
  id?: string;
  existentes?: FichaResumo[];
  confirma?: boolean;
  falhaCarga?: boolean;
  falhaSalvar?: boolean;
  listar?: ReturnType<typeof vi.fn>;
  carregada?: Partial<Ficha>;
}

describe('FormFicha', () => {
  let criar: ReturnType<typeof vi.fn>;
  let atualizar: ReturnType<typeof vi.fn>;
  let buscar: ReturnType<typeof vi.fn>;
  let listarDoAluno: ReturnType<typeof vi.fn>;
  let dialogOpen: ReturnType<typeof vi.fn>;
  let open: ReturnType<typeof vi.fn>;
  let navigate: ReturnType<typeof vi.fn>;

  async function montar(o: Opcoes = {}) {
    criar = vi.fn().mockResolvedValue(ficha({ id: 'nova' }));
    atualizar = vi.fn().mockResolvedValue(ficha());
    buscar = vi.fn().mockResolvedValue(ficha(o.carregada));
    if (o.falhaCarga) buscar.mockRejectedValue(new Error('404'));
    if (o.falhaSalvar) {
      criar.mockRejectedValue(new Error('409'));
      atualizar.mockRejectedValue(new Error('409'));
    }
    listarDoAluno = o.listar ?? vi.fn().mockResolvedValue(o.existentes ?? []);
    dialogOpen = vi.fn().mockReturnValue({ afterClosed: () => of(o.confirma ?? true) });
    open = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: FichaService, useValue: { criar, atualizar, buscar, listarDoAluno } },
        { provide: MatSnackBar, useValue: { open } },
        { provide: MatDialog, useValue: { open: dialogOpen } },
        { provide: ESCOLHER_EXERCICIO, useValue: async () => null },
      ],
    });
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true) as never;
    const fixture = TestBed.createComponent(FormFicha);
    if (o.alunoId) fixture.componentRef.setInput('alunoId', o.alunoId);
    if (o.id) fixture.componentRef.setInput('id', o.id);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  type Fx = { nativeElement: HTMLElement; componentInstance: unknown; detectChanges(): void; whenStable(): Promise<unknown> };
  const el = (f: Fx) => f.nativeElement;
  const divisoes = (f: Fx) =>
    (f.componentInstance as { divisoes: { controls: DivisaoForm[] } }).divisoes.controls;
  const nomes = (f: Fx) => divisoes(f).map((d) => d.controls.nome.value);
  const digitar = (f: Fx, nome: string, valor: string) => {
    const c = el(f).querySelector(`[formControlName="nome"]`) as HTMLInputElement;
    expect(nome).toBe('nome');
    c.value = valor;
    c.dispatchEvent(new Event('input'));
  };
  const addItem = (f: Fx, exercicioId: string) => {
    const fb = TestBed.inject(NonNullableFormBuilder);
    divisoes(f)[0].controls.itens.push(
      criarItemForm(fb, { exercicioId, exercicioNome: exercicioId, grupoMuscular: 'PEITO' }),
    );
  };
  const enviar = async (f: Fx) => {
    (el(f).querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    await f.whenStable();
    f.detectChanges();
  };
  const removerDivisaoBtn = (f: Fx, i: number) =>
    el(f).querySelectorAll<HTMLButtonElement>('button[aria-label="Remover divisão"]')[i];
  const componente = (f: Fx) => f.componentInstance as FormFicha;
  const tick = () => new Promise((r) => setTimeout(r));
  const salvarBtn = (f: Fx) => el(f).querySelector('button[type="submit"]') as HTMLButtonElement;
  const botao = (f: Fx, texto: string) =>
    Array.from(el(f).querySelectorAll('button')).find((b) => b.textContent?.trim() === texto);

  it('nova: começa com Treino A e Adicionar divisão cria Treino B', async () => {
    const f = await montar({ alunoId: 'a1' });
    expect(el(f).textContent).toContain('Nova ficha');
    expect(nomes(f)).toEqual(['Treino A']);
    botao(f, 'Adicionar divisão')!.click();
    f.detectChanges();
    expect(nomes(f)).toEqual(['Treino A', 'Treino B']);
  });

  it('esconde Adicionar divisão ao chegar em 10 divisões', async () => {
    const f = await montar({ alunoId: 'a1' });
    for (let i = 0; i < 9; i++) botao(f, 'Adicionar divisão')!.click();
    f.detectChanges();
    expect(nomes(f)[9]).toBe('Treino J');
    expect(botao(f, 'Adicionar divisão')).toBeUndefined();
  });

  it('nova sem ficha ativa envia criar com divisões e itens na ordem', async () => {
    const f = await montar({ alunoId: 'a1', existentes: [resumo({ status: 'ENCERRADA' })] });
    digitar(f, 'nome', '  Força  ');
    addItem(f, 'e1');
    addItem(f, 'e2');
    await enviar(f);
    expect(dialogOpen).not.toHaveBeenCalled();
    expect(criar).toHaveBeenCalledTimes(1);
    const [aluno, dados] = criar.mock.calls[0];
    expect(aluno).toBe('a1');
    expect(dados.nome).toBe('Força');
    expect(dados.frequenciaSemanal).toBe(3);
    expect(dados.divisoes).toHaveLength(1);
    expect(dados.divisoes[0].nome).toBe('Treino A');
    expect(dados.divisoes[0].itens.map((i: { exercicioId: string }) => i.exercicioId)).toEqual(['e1', 'e2']);
    expect(open).toHaveBeenCalledWith('Ficha criada.', undefined, expect.anything());
    expect(navigate).toHaveBeenCalledWith(['/personal/fichas', 'nova'], { replaceUrl: true });
  });

  it('nova com ficha ativa pede confirmação e só envia se confirmado', async () => {
    const f = await montar({ alunoId: 'a1', existentes: [resumo()], confirma: false });
    digitar(f, 'nome', 'Nova');
    addItem(f, 'e1');
    await enviar(f);
    const dados = dialogOpen.mock.calls[0][1].data;
    expect(dados.titulo).toBe('Substituir ficha ativa');
    expect(dados.mensagem).toBe(
      'A ficha "Antiga" será encerrada e a nova passará a ser a ficha ativa.',
    );
    expect(dados.confirmar).toBe('Criar e encerrar a atual');
    expect(criar).not.toHaveBeenCalled();
    expect(salvarBtn(f).disabled).toBe(false);

    dialogOpen.mockReturnValue({ afterClosed: () => of(true) });
    await enviar(f);
    expect(criar).toHaveBeenCalledTimes(1);
  });

  it('formulário inválido não envia e mostra Revise os campos destacados.', async () => {
    const f = await montar({ alunoId: 'a1' });
    expect(el(f).textContent).not.toContain('Revise os campos destacados.');
    await enviar(f);
    expect(criar).not.toHaveBeenCalled();
    expect(el(f).textContent).toContain('Revise os campos destacados.');
    expect(el(f).textContent).toContain('Informe o nome da ficha.');
  });

  it('data de fim anterior ao início é inválida', async () => {
    const f = await montar({ alunoId: 'a1' });
    const campos = el(f).querySelectorAll<HTMLInputElement>('input[type="date"]');
    campos[0].value = '2026-10-10';
    campos[0].dispatchEvent(new Event('input'));
    campos[1].value = '2026-10-01';
    campos[1].dispatchEvent(new Event('input'));
    campos[1].dispatchEvent(new Event('blur'));
    f.detectChanges();
    expect(el(f).textContent).toContain('A data de fim deve ser igual ou posterior à data de início.');
  });

  it('editar carrega a ficha e envia atualizar com os ids existentes', async () => {
    const f = await montar({ id: 'f1' });
    expect(el(f).textContent).toContain('Editar ficha');
    expect(listarDoAluno).not.toHaveBeenCalled();
    expect(nomes(f)).toEqual(['Treino A']);
    await enviar(f);
    expect(dialogOpen).not.toHaveBeenCalled();
    expect(atualizar).toHaveBeenCalledTimes(1);
    const [id, dados] = atualizar.mock.calls[0];
    expect(id).toBe('f1');
    expect(dados.nome).toBe('Hipertrofia');
    expect(dados.dataFim).toBeNull();
    expect(dados.divisoes[0].id).toBe('d1');
    expect(dados.divisoes[0].itens[0].id).toBe('i1');
    expect(open).toHaveBeenCalledWith('Ficha atualizada.', undefined, expect.anything());
    expect(navigate).toHaveBeenCalledWith(['/personal/fichas', 'f1'], { replaceUrl: true });
  });

  it('editar: cancelar volta ao detalhe do aluno da ficha', async () => {
    const f = await montar({ id: 'f1' });
    botao(f, 'Cancelar')!.click();
    expect(navigate).toHaveBeenCalledWith(['/personal/alunos', 'a1']);
  });

  it('editar: falha na carga volta para a lista de alunos', async () => {
    await montar({ id: 'f1', falhaCarga: true });
    expect(navigate).toHaveBeenCalledWith(['/personal/alunos']);
  });

  it('erroAoSalvarMantemNoFormulario', async () => {
    const f = await montar({ id: 'f1', falhaSalvar: true });
    await enviar(f);
    expect(atualizar).toHaveBeenCalledTimes(1);
    expect(navigate).not.toHaveBeenCalled();
    expect(open).not.toHaveBeenCalled();
    expect(salvarBtn(f).disabled).toBe(false);
    expect(nomes(f)).toEqual(['Treino A']);
  });

  it('não envia duas vezes enquanto salva', async () => {
    const f = await montar({ id: 'f1' });
    let liberar!: (v: Ficha) => void;
    atualizar.mockReturnValue(new Promise<Ficha>((r) => (liberar = r)));
    const form = el(f).querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    form.dispatchEvent(new Event('submit'));
    f.detectChanges();
    expect(atualizar).toHaveBeenCalledTimes(1);
    expect(salvarBtn(f).disabled).toBe(true);
    liberar(ficha());
    await new Promise((r) => setTimeout(r));
    f.detectChanges();
    expect(salvarBtn(f).disabled).toBe(false);
  });

  it('subir e descer trocam a ordem das divisões', async () => {
    const f = await montar({ alunoId: 'a1' });
    botao(f, 'Adicionar divisão')!.click();
    f.detectChanges();
    (el(f).querySelectorAll('button[aria-label="Descer divisão"]')[0] as HTMLButtonElement).click();
    f.detectChanges();
    expect(nomes(f)).toEqual(['Treino B', 'Treino A']);
  });

  it('submit antes da consulta terminar ainda pede confirmação', async () => {
    let responder!: (l: FichaResumo[]) => void;
    const listar = vi.fn().mockReturnValue(new Promise<FichaResumo[]>((r) => (responder = r)));
    const f = await montar({ alunoId: 'a1', listar, confirma: false });
    digitar(f, 'nome', 'Nova');
    addItem(f, 'e1');
    (el(f).querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    await tick();
    expect(criar).not.toHaveBeenCalled();
    responder([resumo()]);
    await tick();
    expect(dialogOpen).toHaveBeenCalledTimes(1);
    expect(dialogOpen.mock.calls[0][1].data.titulo).toBe('Substituir ficha ativa');
    expect(criar).not.toHaveBeenCalled();
  });

  it('falha na consulta não envia e avisa', async () => {
    const listar = vi.fn().mockRejectedValue(new Error('500'));
    const f = await montar({ alunoId: 'a1', listar });
    digitar(f, 'nome', 'Nova');
    addItem(f, 'e1');
    await enviar(f);
    await tick();
    f.detectChanges();
    expect(listar).toHaveBeenCalledTimes(2);
    expect(criar).not.toHaveBeenCalled();
    expect(open).toHaveBeenCalledWith(
      'Não foi possível verificar a ficha ativa. Tente de novo.',
      undefined,
      expect.anything(),
    );
    expect(salvarBtn(f).disabled).toBe(false);
  });

  it('falha na consulta seguida de sucesso na nova tentativa envia normalmente', async () => {
    const listar = vi.fn().mockRejectedValueOnce(new Error('500')).mockResolvedValue([]);
    const f = await montar({ alunoId: 'a1', listar });
    digitar(f, 'nome', 'Nova');
    addItem(f, 'e1');
    await enviar(f);
    await tick();
    expect(listar).toHaveBeenCalledTimes(2);
    expect(dialogOpen).not.toHaveBeenCalled();
    expect(criar).toHaveBeenCalledTimes(1);
  });

  it('remover divisão com exercícios pede confirmação e cancelado não remove', async () => {
    const f = await montar({ alunoId: 'a1', confirma: false });
    addItem(f, 'e1');
    removerDivisaoBtn(f, 0).click();
    await tick();
    f.detectChanges();
    expect(dialogOpen.mock.calls[0][1].data).toEqual({
      titulo: 'Remover divisão',
      mensagem: 'A divisão "Treino A" e o exercício dela serão removidos.',
      confirmar: 'Remover',
    });
    expect(nomes(f)).toEqual(['Treino A']);
  });

  it('remover divisão com exercícios confirmado remove', async () => {
    const f = await montar({ alunoId: 'a1', confirma: true });
    addItem(f, 'e1');
    addItem(f, 'e2');
    removerDivisaoBtn(f, 0).click();
    await tick();
    f.detectChanges();
    expect(dialogOpen.mock.calls[0][1].data.mensagem).toBe(
      'A divisão "Treino A" e seus 2 exercícios serão removidos.',
    );
    expect(nomes(f)).toEqual([]);
  });

  it('remover divisão sem exercícios remove sem perguntar', async () => {
    const f = await montar({ alunoId: 'a1' });
    removerDivisaoBtn(f, 0).click();
    await tick();
    f.detectChanges();
    expect(dialogOpen).not.toHaveBeenCalled();
    expect(nomes(f)).toEqual([]);
  });

  it('adicionar divisão reaproveita a primeira letra livre', async () => {
    const f = await montar({ alunoId: 'a1' });
    botao(f, 'Adicionar divisão')!.click();
    botao(f, 'Adicionar divisão')!.click();
    f.detectChanges();
    expect(nomes(f)).toEqual(['Treino A', 'Treino B', 'Treino C']);
    removerDivisaoBtn(f, 1).click();
    await tick();
    f.detectChanges();
    botao(f, 'Adicionar divisão')!.click();
    f.detectChanges();
    expect(nomes(f)).toEqual(['Treino A', 'Treino C', 'Treino B']);
  });

  it('sem divisões, salvar mostra Adicione pelo menos uma divisão.', async () => {
    const f = await montar({ alunoId: 'a1' });
    digitar(f, 'nome', 'Nova');
    removerDivisaoBtn(f, 0).click();
    await tick();
    f.detectChanges();
    expect(el(f).textContent).not.toContain('Adicione pelo menos uma divisão.');
    await enviar(f);
    expect(criar).not.toHaveBeenCalled();
    expect(el(f).textContent).toContain('Adicione pelo menos uma divisão.');
  });

  it('editar ficha encerrada volta para a visualização e avisa', async () => {
    await montar({ id: 'f1', carregada: { status: 'ENCERRADA' } });
    expect(navigate).toHaveBeenCalledWith(['/personal/fichas', 'f1'], { replaceUrl: true });
    expect(open).toHaveBeenCalledWith(
      'Esta ficha está encerrada e não pode ser editada.',
      undefined,
      expect.anything(),
    );
  });

  it('temAlteracoesNaoSalvas: falso ao abrir, verdadeiro ao alterar, falso depois de salvar', async () => {
    const f = await montar({ id: 'f1' });
    expect(componente(f).temAlteracoesNaoSalvas()).toBe(false);
    digitar(f, 'nome', 'Outro nome');
    expect(componente(f).temAlteracoesNaoSalvas()).toBe(true);
    await enviar(f);
    expect(atualizar).toHaveBeenCalledTimes(1);
    expect(componente(f).temAlteracoesNaoSalvas()).toBe(false);
  });

  it('temAlteracoesNaoSalvas continua verdadeiro se salvar falhar', async () => {
    const f = await montar({ id: 'f1', falhaSalvar: true });
    digitar(f, 'nome', 'Outro nome');
    await enviar(f);
    expect(componente(f).temAlteracoesNaoSalvas()).toBe(true);
  });

  it('beforeunload só é bloqueado com alterações não salvas', async () => {
    const f = await montar({ alunoId: 'a1' });
    const limpo = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(limpo);
    expect(limpo.defaultPrevented).toBe(false);
    digitar(f, 'nome', 'Nova');
    const sujo = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(sujo);
    expect(sujo.defaultPrevented).toBe(true);
  });
});
