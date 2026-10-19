import { WritableSignal, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { Ficha } from '../../personal/fichas/ficha.model';
import { FichaAtiva } from '../../shared/sessao/sessao.model';
import { SessaoService } from '../../shared/sessao/sessao.service';
import { EnvioComErro } from '../sessao-local/sessao-local.model';
import { SessaoLocalService } from '../sessao-local/sessao-local.service';
import { Treino } from './treino';

const item = (id: string) => ({
  id,
  exercicioId: 'e' + id,
  exercicioNome: 'Ex ' + id,
  ordem: 1,
  series: 3,
  repeticoes: '10',
  cargaKg: null,
  descansoSeg: 60,
  observacao: null,
});

const ficha = {
  id: 'f1',
  alunoId: 'a1',
  nome: 'Hipertrofia A/B',
  divisoes: [
    { id: 'd1', nome: 'Treino A', ordem: 1, itens: [item('1'), item('2')] },
    { id: 'd2', nome: 'Treino B', ordem: 2, itens: [item('3')] },
  ],
} as unknown as Ficha;

describe('Treino', () => {
  async function montar(
    ativa: FichaAtiva | null | Error | (FichaAtiva | null)[],
    local: {
      emAndamento?: unknown;
      pendentes?: number | WritableSignal<number>;
      comErro?: EnvioComErro[];
      enviarPendentes?: () => Promise<void>;
    } = {},
    confirma = true,
  ) {
    const fichaAtiva = vi.fn();
    if (ativa instanceof Error) fichaAtiva.mockRejectedValue(ativa);
    else if (Array.isArray(ativa)) ativa.forEach((a) => fichaAtiva.mockResolvedValueOnce(a));
    else fichaAtiva.mockResolvedValue(ativa);
    const sessaoLocal = {
      emAndamento: signal(local.emAndamento ?? null),
      pendentes:
        typeof local.pendentes === 'function' ? local.pendentes : signal(local.pendentes ?? 0),
      comErro: signal(local.comErro ?? []),
      iniciar: vi.fn(),
      descartar: vi.fn(),
      enviarPendentes: vi.fn(local.enviarPendentes ?? (() => Promise.resolve())),
      descartarComErro: vi.fn(),
      tentarDeNovoComErro: vi.fn().mockResolvedValue(undefined),
    };
    TestBed.configureTestingModule({
      providers: [
        { provide: SessaoService, useValue: { fichaAtiva } },
        { provide: SessaoLocalService, useValue: sessaoLocal },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(confirma) }) } },
      ],
    });
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(Treino);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, sessaoLocal, navigate, fichaAtiva };
  }

  const el = (f: { nativeElement: HTMLElement }) => f.nativeElement;
  const botao = (f: { nativeElement: HTMLElement }, texto: string) =>
    Array.from(el(f).querySelectorAll('button')).find((b) => b.textContent?.trim() === texto);

  it('sem ficha ativa mostra a mensagem', async () => {
    const { fixture: f } = await montar(null);
    expect(el(f).textContent).toContain('Você ainda não tem uma ficha ativa. Fale com seu personal.');
  });

  it('mostra erro quando falha', async () => {
    const { fixture: f } = await montar(new Error('x'));
    expect(el(f).textContent).toContain('Não foi possível carregar seu treino.');
  });

  it('pré-seleciona a divisão sugerida e marca Sugerido', async () => {
    const { fixture: f, sessaoLocal } = await montar({ ficha, proximaDivisaoId: 'd2' });
    const t = el(f).textContent!;
    expect(t).toContain('Hipertrofia A/B');
    expect(t).toContain('2 exercícios');
    expect(t).toContain('1 exercício');
    expect(t).not.toContain('1 exercícios');
    expect(t.match(/Sugerido/g)?.length).toBe(1);
    const radios = el(f).querySelectorAll<HTMLInputElement>('input[type=radio]');
    expect(radios[0].checked).toBe(false);
    expect(radios[1].checked).toBe(true);
    botao(f, 'Começar treino')!.click();
    expect(sessaoLocal.iniciar).toHaveBeenCalledWith(ficha, ficha.divisoes[1]);
  });

  it('Começar treino inicia a sessão local e vai para a execução', async () => {
    const { fixture: f, sessaoLocal, navigate } = await montar({ ficha, proximaDivisaoId: null });
    el(f).querySelectorAll<HTMLInputElement>('input[type=radio]')[1].click();
    f.detectChanges();
    botao(f, 'Começar treino')!.click();
    expect(sessaoLocal.iniciar).toHaveBeenCalledWith(ficha, ficha.divisoes[1]);
    expect(navigate).toHaveBeenCalledWith(['/aluno/treino/execucao']);
  });

  it('sem sugestão pré-seleciona a primeira divisão', async () => {
    const { fixture: f, sessaoLocal } = await montar({ ficha, proximaDivisaoId: null });
    botao(f, 'Começar treino')!.click();
    expect(sessaoLocal.iniciar).toHaveBeenCalledWith(ficha, ficha.divisoes[0]);
  });

  it('treino em andamento mostra Continuar e Descartar e bloqueia Começar', async () => {
    const { fixture: f, sessaoLocal, navigate } = await montar(
      { ficha, proximaDivisaoId: 'd1' },
      { emAndamento: { divisaoNome: 'Treino A' } },
    );
    expect(el(f).textContent).toContain('Treino em andamento: Treino A');
    expect(botao(f, 'Começar treino')!.disabled).toBe(true);
    botao(f, 'Continuar')!.click();
    expect(navigate).toHaveBeenCalledWith(['/aluno/treino/execucao']);
    botao(f, 'Descartar')!.click();
    await f.whenStable();
    expect(sessaoLocal.descartar).toHaveBeenCalled();
  });

  it('Descartar cancelado não descarta', async () => {
    const { fixture: f, sessaoLocal } = await montar(
      { ficha, proximaDivisaoId: 'd1' },
      { emAndamento: { divisaoNome: 'Treino A' } },
      false,
    );
    botao(f, 'Descartar')!.click();
    await f.whenStable();
    expect(sessaoLocal.descartar).not.toHaveBeenCalled();
  });

  it('mostraTreinosNaoEnviados', async () => {
    const { fixture: f, sessaoLocal } = await montar(
      { ficha, proximaDivisaoId: null },
      { comErro: [{ sessao: {} as never, mensagem: 'Divisão inválida.' }] },
    );
    expect(el(f).textContent).toContain('Um treino não pôde ser enviado: Divisão inválida.');
    botao(f, 'Descartar')!.click();
    expect(sessaoLocal.descartarComErro).toHaveBeenCalledWith(0);
  });

  it('treino recusado pode ser enviado de novo', async () => {
    const { fixture: f, sessaoLocal } = await montar(
      { ficha, proximaDivisaoId: null },
      { comErro: [{ sessao: {} as never, mensagem: 'Divisão inválida.' }] },
    );
    botao(f, 'Tentar de novo')!.click();
    expect(sessaoLocal.tentarDeNovoComErro).toHaveBeenCalledWith(0);
  });

  /** Pendentes que somem quando o envio disparado na abertura termina (`liberar`). */
  function envioNaAbertura() {
    const pendentes = signal(1);
    let liberar!: () => void;
    const enviarPendentes = () =>
      new Promise<void>((resolve) => {
        liberar = () => {
          pendentes.set(0);
          resolve();
        };
      });
    return { pendentes, enviarPendentes, liberar: () => liberar() };
  }

  it('recarrega a sugestão quando os pendentes enviados na abertura terminam', async () => {
    const envio = envioNaAbertura();
    const { fixture: f, fichaAtiva } = await montar(
      [
        { ficha, proximaDivisaoId: 'd1' },
        { ficha, proximaDivisaoId: 'd2' },
      ],
      envio,
    );
    expect(el(f).querySelectorAll<HTMLInputElement>('input[type=radio]')[0].checked).toBe(true);

    envio.liberar();
    await f.whenStable();
    f.detectChanges();

    expect(fichaAtiva).toHaveBeenCalledTimes(2);
    const radios = el(f).querySelectorAll<HTMLInputElement>('input[type=radio]');
    expect(radios[1].checked).toBe(true);
    const sugerido = Array.from(el(f).querySelectorAll('label.divisao')).find((l) =>
      l.textContent?.includes('Sugerido'),
    );
    expect(sugerido?.textContent).toContain('Treino B');
  });

  it('a recarga não troca a divisão escolhida pelo aluno', async () => {
    const envio = envioNaAbertura();
    const { fixture: f, sessaoLocal } = await montar(
      [
        { ficha, proximaDivisaoId: 'd2' },
        { ficha, proximaDivisaoId: 'd2' },
      ],
      envio,
    );
    el(f).querySelectorAll<HTMLInputElement>('input[type=radio]')[0].click();
    f.detectChanges();

    envio.liberar();
    await f.whenStable();
    f.detectChanges();

    expect(el(f).querySelectorAll<HTMLInputElement>('input[type=radio]')[0].checked).toBe(true);
    botao(f, 'Começar treino')!.click();
    expect(sessaoLocal.iniciar).toHaveBeenCalledWith(ficha, ficha.divisoes[0]);
  });

  it('sem pendentes enviados não recarrega a ficha', async () => {
    const { fixture: f, fichaAtiva } = await montar({ ficha, proximaDivisaoId: 'd1' }, { pendentes: 2 });
    await f.whenStable();
    expect(fichaAtiva).toHaveBeenCalledTimes(1);
  });

  it('pendentes mostra aviso e Enviar agora', async () => {
    const { fixture: f, sessaoLocal } = await montar({ ficha, proximaDivisaoId: null }, { pendentes: 2 });
    expect(el(f).textContent).toContain('2 treinos aguardando envio');
    sessaoLocal.enviarPendentes.mockClear();
    botao(f, 'Enviar agora')!.click();
    expect(sessaoLocal.enviarPendentes).toHaveBeenCalled();
  });

  it('1 pendente usa singular', async () => {
    const { fixture: f } = await montar(null, { pendentes: 1 });
    expect(el(f).textContent).toContain('1 treino aguardando envio');
  });

  it('ao abrir tenta enviar pendentes', async () => {
    const { sessaoLocal, fichaAtiva } = await montar(null);
    expect(sessaoLocal.enviarPendentes).toHaveBeenCalled();
    expect(fichaAtiva).toHaveBeenCalled();
  });
});
