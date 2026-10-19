import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { SessaoEmAndamento } from '../sessao-local/sessao-local.model';
import { SessaoLocalService } from '../sessao-local/sessao-local.service';
import { Execucao } from './execucao';

const sessao = (concluida = false): SessaoEmAndamento => ({
  clientId: 'c1',
  fichaId: 'f1',
  divisaoId: 'd1',
  divisaoNome: 'Treino A',
  iniciadaEm: new Date(Date.now() - 5 * 60_000).toISOString(),
  observacao: '',
  exercicios: [
    {
      itemDivisaoId: 'i1',
      exercicioId: 'e1',
      exercicioNome: 'Supino',
      prescricao: { series: 3, repeticoes: '8-12', cargaKg: 40, descansoSeg: 60, observacao: 'Controle a descida' },
      series: [
        { numero: 1, cargaKg: 40, repeticoes: 8, concluida },
        { numero: 2, cargaKg: 40, repeticoes: 8, concluida: false },
        { numero: 3, cargaKg: 40, repeticoes: null, concluida: false },
      ],
    },
  ],
});

describe('Execucao', () => {
  async function montar(
    emAndamento: SessaoEmAndamento | null,
    finalizar: () => Promise<string> = () => Promise.resolve('enviado'),
    confirma = true,
  ) {
    const andamento = signal(emAndamento);
    const sessaoLocal = {
      emAndamento: andamento,
      atualizarSerie: vi.fn((ex: number, si: number, m: object) =>
        andamento.update((a) =>
          a && {
            ...a,
            exercicios: a.exercicios.map((e, i) =>
              i !== ex ? e : { ...e, series: e.series.map((x, j) => (j === si ? { ...x, ...m } : x)) },
            ),
          },
        ),
      ),
      adicionarSerie: vi.fn(),
      atualizarObservacao: vi.fn(),
      finalizar: vi.fn(finalizar),
    };
    const open = vi.fn(() => ({ afterClosed: () => of(confirma) }));
    const abrir = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        { provide: SessaoLocalService, useValue: sessaoLocal },
        { provide: MatDialog, useValue: { open } },
        { provide: MatSnackBar, useValue: { open: abrir } },
      ],
    });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(Execucao);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, sessaoLocal, navigate, open, abrir };
  }

  const el = (f: { nativeElement: HTMLElement }) => f.nativeElement;
  const botao = (f: { nativeElement: HTMLElement }, texto: string) =>
    Array.from(el(f).querySelectorAll('button')).find((b) => b.textContent?.trim().endsWith(texto));
  const campo = (f: { nativeElement: HTMLElement }, rotulo: string, i: number) =>
    el(f).querySelectorAll<HTMLInputElement>(`input[aria-label="${rotulo}"]`)[i];

  it('sem treino em andamento volta para o treino de hoje', async () => {
    const { navigate } = await montar(null);
    expect(navigate).toHaveBeenCalledWith(['/aluno/treino'], { replaceUrl: true });
  });

  it('mostra a prescrição 3 x 8-12 · 40 kg · 60 s', async () => {
    const { fixture: f } = await montar(sessao());
    const t = el(f).textContent!;
    expect(t).toContain('Treino A');
    expect(t).toContain('5 min');
    expect(t).toContain('Supino');
    expect(t).toContain('3 x 8-12 · 40 kg · 60 s');
    expect(t).toContain('Controle a descida');
  });

  it('Concluir marca a série e grava', async () => {
    const { fixture: f, sessaoLocal } = await montar(sessao());
    botao(f, 'Concluir')!.click();
    expect(sessaoLocal.atualizarSerie).toHaveBeenCalledWith(0, 0, { concluida: true });
  });

  it('série concluída mostra Concluída e toque desfaz', async () => {
    const { fixture: f, sessaoLocal } = await montar(sessao(true));
    botao(f, 'Concluída')!.click();
    expect(sessaoLocal.atualizarSerie).toHaveBeenCalledWith(0, 0, { concluida: false });
  });

  it('editar carga grava na hora', async () => {
    const { fixture: f, sessaoLocal } = await montar(sessao());
    const c = campo(f, 'Carga (kg)', 0);
    c.value = '42.5';
    c.dispatchEvent(new Event('change'));
    expect(sessaoLocal.atualizarSerie).toHaveBeenCalledWith(0, 0, { cargaKg: 42.5 });
  });

  it('input com repetições válidas grava e libera Concluir sem change', async () => {
    const { fixture: f, sessaoLocal } = await montar(sessao());
    const r = campo(f, 'Repetições', 2);
    r.value = '10';
    r.dispatchEvent(new Event('input'));
    expect(sessaoLocal.atualizarSerie).toHaveBeenCalledWith(0, 2, { repeticoes: 10 });
    f.detectChanges();
    const linha = r.closest('.serie')!;
    expect(linha.querySelector<HTMLButtonElement>('button.concluir')!.disabled).toBe(false);
  });

  it('input não canônico ("42.50") não grava nem reescreve o campo', async () => {
    const s = sessao();
    s.exercicios[0].series[0].cargaKg = 42.5;
    const { fixture: f, sessaoLocal } = await montar(s);
    const c = campo(f, 'Carga (kg)', 0);
    // o navegador/DOM já reporta "42." como "" (ignorado); "42.50" é o caso observável
    c.value = '42.50';
    c.dispatchEvent(new Event('input'));
    f.detectChanges();
    expect(sessaoLocal.atualizarSerie).not.toHaveBeenCalled();
    expect(c.value).toBe('42.50');
  });

  it('input "42.567" não grava; change grava 42.57', async () => {
    const { fixture: f, sessaoLocal } = await montar(sessao());
    const c = campo(f, 'Carga (kg)', 0);
    c.value = '42.567';
    c.dispatchEvent(new Event('input'));
    expect(sessaoLocal.atualizarSerie).not.toHaveBeenCalled();
    c.dispatchEvent(new Event('change'));
    expect(sessaoLocal.atualizarSerie).toHaveBeenCalledWith(0, 0, { cargaKg: 42.57 });
  });

  it('input vazio ou inválido não grava nem apaga o campo', async () => {
    const { fixture: f, sessaoLocal } = await montar(sessao());
    const r = campo(f, 'Repetições', 0);
    r.value = '';
    r.dispatchEvent(new Event('input'));
    const c = campo(f, 'Carga (kg)', 0);
    c.value = '-5';
    c.dispatchEvent(new Event('input'));
    expect(sessaoLocal.atualizarSerie).not.toHaveBeenCalled();
    expect(c.value).toBe('-5');
  });

  it('change inválido grava null e limpa o campo', async () => {
    const { fixture: f, sessaoLocal } = await montar(sessao());
    const c = campo(f, 'Carga (kg)', 2);
    c.value = '-5';
    c.dispatchEvent(new Event('change'));
    expect(sessaoLocal.atualizarSerie).toHaveBeenCalledWith(0, 2, { cargaKg: null });
    expect(c.value).toBe('');
  });

  it('finalizar tira o foco do campo ativo', async () => {
    const { fixture: f } = await montar(sessao());
    document.body.appendChild(el(f));
    try {
      const c = campo(f, 'Repetições', 0);
      c.focus();
      expect(document.activeElement).toBe(c);
      botao(f, 'Finalizar treino')!.click();
      expect(document.activeElement).not.toBe(c);
      await f.whenStable();
    } finally {
      el(f).remove();
    }
  });

  it('carga negativa ou acima do limite vira vazia', async () => {
    const { fixture: f, sessaoLocal } = await montar(sessao());
    const c = campo(f, 'Carga (kg)', 0);
    for (const v of ['-5', '10000']) {
      c.value = v;
      c.dispatchEvent(new Event('change'));
      expect(sessaoLocal.atualizarSerie).toHaveBeenLastCalledWith(0, 0, { cargaKg: null });
    }
    c.value = '42.567';
    c.dispatchEvent(new Event('change'));
    expect(sessaoLocal.atualizarSerie).toHaveBeenLastCalledWith(0, 0, { cargaKg: 42.57 });
  });

  it('repetições negativas ou acima de 999 bloqueiam Concluir', async () => {
    const { fixture: f, sessaoLocal } = await montar(sessao());
    const r = campo(f, 'Repetições', 1);
    r.value = '-3';
    r.dispatchEvent(new Event('change'));
    expect(sessaoLocal.atualizarSerie).toHaveBeenLastCalledWith(0, 1, { repeticoes: null, concluida: false });
    f.detectChanges();
    const concluir = Array.from(el(f).querySelectorAll('button')).filter((b) =>
      b.textContent?.includes('Concluir'),
    );
    expect(concluir[1].disabled).toBe(true);
    r.value = '1000';
    r.dispatchEvent(new Event('change'));
    expect(sessaoLocal.atualizarSerie).toHaveBeenLastCalledWith(0, 1, { repeticoes: null, concluida: false });
  });

  it('apagar as repetições desmarca a série e Concluir fica bloqueado', async () => {
    const { fixture: f, sessaoLocal } = await montar(sessao(true));
    const r = campo(f, 'Repetições', 0);
    r.value = '';
    r.dispatchEvent(new Event('change'));
    f.detectChanges();
    expect(sessaoLocal.atualizarSerie).toHaveBeenCalledWith(0, 0, { repeticoes: null, concluida: false });
    // série 3 tem repetições vazias
    const concluir = Array.from(el(f).querySelectorAll('button')).filter((b) =>
      b.textContent?.includes('Concluir'),
    );
    expect(concluir[0].disabled).toBe(true);
    expect(concluir[1].disabled).toBe(false);
    expect(concluir[2].disabled).toBe(true);
    expect(el(f).textContent).toContain('Informe as repetições.');
  });

  it('abrirExecucaoComTreinoSalvoMostraSeriesConcluidas', async () => {
    const { fixture: f } = await montar(sessao(true));
    expect(el(f).textContent).toContain('Concluída');
    expect(campo(f, 'Carga (kg)', 0).value).toBe('40');
  });

  it('Adicionar série e observação gravam', async () => {
    const { fixture: f, sessaoLocal } = await montar(sessao());
    botao(f, 'Adicionar série')!.click();
    expect(sessaoLocal.adicionarSerie).toHaveBeenCalledWith(0);
    const o = el(f).querySelector<HTMLTextAreaElement>('textarea')!;
    o.value = 'Foi bom';
    o.dispatchEvent(new Event('input'));
    expect(sessaoLocal.atualizarObservacao).toHaveBeenCalledWith('Foi bom');
  });

  it('finalizar com séries pendentes pede confirmação', async () => {
    const { fixture: f, sessaoLocal, open } = await montar(sessao(), undefined, false);
    botao(f, 'Finalizar treino')!.click();
    await f.whenStable();
    expect(open).toHaveBeenCalled();
    expect(sessaoLocal.finalizar).not.toHaveBeenCalled();
  });

  it('finalizar enviado mostra Treino registrado! e volta', async () => {
    const { fixture: f, navigate, abrir, open } = await montar(sessao());
    botao(f, 'Finalizar treino')!.click();
    await f.whenStable();
    expect(open).toHaveBeenCalled();
    expect(abrir).toHaveBeenCalledWith('Treino registrado!', undefined, expect.anything());
    expect(navigate).toHaveBeenCalledWith(['/aluno/treino'], { replaceUrl: true });
  });

  it('finalizar pendente avisa que será enviado depois', async () => {
    const { fixture: f, abrir, navigate } = await montar(sessao(), () => Promise.resolve('pendente'));
    botao(f, 'Finalizar treino')!.click();
    await f.whenStable();
    expect(abrir).toHaveBeenCalledWith(
      'Treino salvo. Ele será enviado quando houver conexão.',
      undefined,
      expect.anything(),
    );
    expect(navigate).toHaveBeenCalledWith(['/aluno/treino'], { replaceUrl: true });
  });

  it('finalizar recusado avisa e volta', async () => {
    const { fixture: f, abrir, navigate } = await montar(sessao(), () => Promise.resolve('recusado'));
    botao(f, 'Finalizar treino')!.click();
    await f.whenStable();
    expect(abrir).toHaveBeenCalledWith(
      'O treino não pôde ser registrado. Veja o aviso na tela de treino.',
      undefined,
      expect.anything(),
    );
    expect(navigate).toHaveBeenCalledWith(['/aluno/treino'], { replaceUrl: true });
  });

  it('sem série concluída mostra o erro e fica', async () => {
    const { fixture: f, navigate } = await montar(sessao(), () =>
      Promise.reject(new Error('Conclua pelo menos uma série para finalizar o treino.')),
    );
    navigate.mockClear();
    botao(f, 'Finalizar treino')!.click();
    await f.whenStable();
    f.detectChanges();
    expect(el(f).textContent).toContain('Conclua pelo menos uma série para finalizar o treino.');
    expect(navigate).not.toHaveBeenCalled();
    expect(botao(f, 'Finalizar treino')!.disabled).toBe(false);
  });

  it('erro de armazenamento mostra a mensagem e fica', async () => {
    const msg = 'Não foi possível salvar o treino no aparelho. Libere espaço e tente de novo.';
    const { fixture: f, navigate } = await montar(sessao(), () => Promise.reject(new Error(msg)));
    navigate.mockClear();
    botao(f, 'Finalizar treino')!.click();
    await f.whenStable();
    f.detectChanges();
    expect(el(f).textContent).toContain(msg);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('mostra Enviando treino… enquanto finaliza, depois que o treino sai do andamento', async () => {
    let liberar!: (v: string) => void;
    let local!: { emAndamento: { set(v: SessaoEmAndamento | null): void } };
    const m = await montar(sessao(true), () => {
      // o serviço limpa o em andamento antes de esperar o envio
      local.emAndamento.set(null);
      return new Promise<string>((r) => (liberar = r));
    });
    local = m.sessaoLocal;
    const f = m.fixture;
    botao(f, 'Finalizar treino')!.click();
    await f.whenStable();
    f.detectChanges();
    expect(el(f).textContent).toContain('Enviando treino…');
    liberar('enviado');
    await f.whenStable();
    expect(m.navigate).toHaveBeenCalledWith(['/aluno/treino'], { replaceUrl: true });
  });

  it('desabilita Finalizar treino enquanto finaliza', async () => {
    let liberar!: (v: string) => void;
    const { fixture: f, sessaoLocal } = await montar(
      sessao(),
      () => new Promise<string>((r) => (liberar = r)),
    );
    botao(f, 'Finalizar treino')!.click();
    await f.whenStable();
    f.detectChanges();
    expect(botao(f, 'Finalizar treino')!.disabled).toBe(true);
    botao(f, 'Finalizar treino')!.click();
    expect(sessaoLocal.finalizar).toHaveBeenCalledTimes(1);
    liberar('enviado');
    await f.whenStable();
  });
});
