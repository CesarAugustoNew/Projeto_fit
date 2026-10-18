import { signal } from '@angular/core';
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
    ativa: FichaAtiva | null | Error,
    local: { emAndamento?: unknown; pendentes?: number; comErro?: EnvioComErro[] } = {},
    confirma = true,
  ) {
    const fichaAtiva =
      ativa instanceof Error ? vi.fn().mockRejectedValue(ativa) : vi.fn().mockResolvedValue(ativa);
    const sessaoLocal = {
      emAndamento: signal(local.emAndamento ?? null),
      pendentes: signal(local.pendentes ?? 0),
      comErro: signal(local.comErro ?? []),
      iniciar: vi.fn(),
      descartar: vi.fn(),
      enviarPendentes: vi.fn().mockResolvedValue(undefined),
      descartarComErro: vi.fn(),
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
