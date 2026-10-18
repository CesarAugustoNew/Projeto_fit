import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { WritableSignal, signal } from '@angular/core';
import { UsuarioService } from '../../core/usuario/usuario.service';
import { Usuario } from '../../core/usuario/usuario.model';
import { Divisao, Ficha, ItemDivisao } from '../../personal/fichas/ficha.model';
import { Sessao, SessaoRequest } from '../../shared/sessao/sessao.model';
import { SessaoService } from '../../shared/sessao/sessao.service';
import { repeticoesSugeridas } from './sessao-local.model';
import { SessaoLocalService } from './sessao-local.service';

function item(id: string, parcial: Partial<ItemDivisao> = {}): ItemDivisao {
  return {
    id,
    exercicioId: `e-${id}`,
    exercicioNome: `Exercício ${id}`,
    grupoMuscular: 'PEITO',
    ordem: 1,
    series: 3,
    repeticoes: '8-12',
    cargaKg: 40,
    descansoSeg: 60,
    observacao: null,
    ...parcial,
  };
}

const divisao: Divisao = {
  id: 'd1',
  nome: 'Treino A',
  ordem: 1,
  itens: [item('i1'), item('i2', { series: 2, repeticoes: '10', cargaKg: null })],
};

const ficha: Ficha = {
  id: 'f1',
  alunoId: 'a1',
  nome: 'Hipertrofia',
  objetivo: null,
  frequenciaSemanal: 3,
  dataInicio: '2026-10-01',
  dataFim: null,
  status: 'ATIVA',
  vencida: false,
  encerradaEm: null,
  divisoes: [divisao],
};

function usuario(id: string): Usuario {
  return { id, nome: 'Aluno', email: `${id}@x.com`, papel: 'ALUNO' };
}

function erroHttp(status: number, error: unknown = null): HttpErrorResponse {
  return new HttpErrorResponse({ status, error });
}

const sessaoSalva = { id: 's1' } as Sessao;

/** Deixa rodar promises e microtarefas pendentes. */
const esperar = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('SessaoLocalService', () => {
  let registrar: ReturnType<typeof vi.fn>;
  let usuarioAtual: WritableSignal<Usuario | null>;

  beforeEach(() => {
    localStorage.clear();
    registrar = vi.fn();
    usuarioAtual = signal<Usuario | null>(usuario('u1'));
    TestBed.configureTestingModule({
      providers: [
        { provide: SessaoService, useValue: { registrar } },
        { provide: UsuarioService, useValue: { usuario: usuarioAtual.asReadonly() } },
      ],
    });
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    localStorage.clear();
  });

  const criar = () => TestBed.inject(SessaoLocalService);

  /** Novo serviço (como depois de recarregar a página), lendo o mesmo storage. */
  function recriar(): SessaoLocalService {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        { provide: SessaoService, useValue: { registrar } },
        { provide: UsuarioService, useValue: { usuario: usuarioAtual.asReadonly() } },
      ],
    });
    return TestBed.inject(SessaoLocalService);
  }

  const pendentesNoStorage = (): SessaoRequest[] =>
    JSON.parse(localStorage.getItem('sessoes-pendentes:u1') ?? '[]');

  it('repeticoesSugeridas usa o número ou o início da faixa', () => {
    expect(repeticoesSugeridas('10')).toBe(10);
    expect(repeticoesSugeridas('8-12')).toBe(8);
  });

  it('iniciar pré-preenche séries com a prescrição e grava no storage', () => {
    const servico = criar();

    const sessao = servico.iniciar(ficha, divisao);

    expect(sessao.clientId).toMatch(/^[0-9a-f-]{36}$/);
    expect(sessao.fichaId).toBe('f1');
    expect(sessao.divisaoId).toBe('d1');
    expect(sessao.divisaoNome).toBe('Treino A');
    expect(sessao.observacao).toBe('');
    expect(Number.isNaN(Date.parse(sessao.iniciadaEm))).toBe(false);
    expect(sessao.exercicios.map((e) => e.itemDivisaoId)).toEqual(['i1', 'i2']);
    expect(sessao.exercicios[0].prescricao).toEqual({
      series: 3,
      repeticoes: '8-12',
      cargaKg: 40,
      descansoSeg: 60,
      observacao: null,
    });
    expect(sessao.exercicios[0].series).toEqual([
      { numero: 1, cargaKg: 40, repeticoes: 8, concluida: false },
      { numero: 2, cargaKg: 40, repeticoes: 8, concluida: false },
      { numero: 3, cargaKg: 40, repeticoes: 8, concluida: false },
    ]);
    expect(sessao.exercicios[1].series).toEqual([
      { numero: 1, cargaKg: null, repeticoes: 10, concluida: false },
      { numero: 2, cargaKg: null, repeticoes: 10, concluida: false },
    ]);
    expect(servico.emAndamento()).toEqual(sessao);
    expect(JSON.parse(localStorage.getItem('sessao-em-andamento:u1')!)).toEqual(sessao);
  });

  it('restauraSessaoEmAndamento', () => {
    const servico = criar();
    servico.iniciar(ficha, divisao);
    servico.atualizarSerie(0, 1, { cargaKg: 42.5, concluida: true });
    servico.adicionarSerie(1);
    servico.atualizarObservacao('Pesado hoje');
    const antes = servico.emAndamento();

    const restaurado = recriar();

    expect(restaurado.emAndamento()).toEqual(antes);
    expect(antes!.exercicios[0].series[1]).toEqual({ numero: 2, cargaKg: 42.5, repeticoes: 8, concluida: true });
    expect(antes!.exercicios[1].series[2]).toEqual({ numero: 3, cargaKg: null, repeticoes: 10, concluida: false });
    expect(antes!.observacao).toBe('Pesado hoje');
  });

  it('descartar remove o treino em andamento', () => {
    const servico = criar();
    servico.iniciar(ficha, divisao);

    servico.descartar();

    expect(servico.emAndamento()).toBeNull();
    expect(localStorage.getItem('sessao-em-andamento:u1')).toBeNull();
  });

  it('finalizar envia só as séries concluídas, na ordem', async () => {
    registrar.mockResolvedValue(sessaoSalva);
    const servico = criar();
    const sessao = servico.iniciar(ficha, divisao);
    servico.atualizarSerie(1, 1, { concluida: true, repeticoes: 12 });
    servico.atualizarSerie(0, 0, { concluida: true });
    servico.atualizarSerie(0, 2, { concluida: true, cargaKg: 45 });
    servico.atualizarObservacao('  Bom treino ');

    const resultado = await servico.finalizar();

    expect(resultado).toBe('enviado');
    expect(registrar).toHaveBeenCalledTimes(1);
    const req: SessaoRequest = registrar.mock.calls[0][0];
    expect(req).toEqual({
      clientId: sessao.clientId,
      divisaoId: 'd1',
      divisaoNome: 'Treino A',
      iniciadaEm: sessao.iniciadaEm,
      finalizadaEm: expect.any(String),
      observacao: 'Bom treino',
      series: [
        { itemDivisaoId: 'i1', exercicioId: 'e-i1', numeroSerie: 1, repeticoes: 8, cargaKg: 40 },
        { itemDivisaoId: 'i1', exercicioId: 'e-i1', numeroSerie: 3, repeticoes: 8, cargaKg: 45 },
        { itemDivisaoId: 'i2', exercicioId: 'e-i2', numeroSerie: 2, repeticoes: 12, cargaKg: null },
      ],
    });
    expect(Date.parse(req.finalizadaEm)).toBeGreaterThanOrEqual(Date.parse(req.iniciadaEm));
    expect(servico.emAndamento()).toBeNull();
    expect(localStorage.getItem('sessao-em-andamento:u1')).toBeNull();
    expect(servico.pendentes()).toBe(0);
    expect(pendentesNoStorage()).toEqual([]);
  });

  it('finalizar sem observação envia null', async () => {
    registrar.mockResolvedValue(sessaoSalva);
    const servico = criar();
    servico.iniciar(ficha, divisao);
    servico.atualizarSerie(0, 0, { concluida: true });
    servico.atualizarObservacao('   ');

    await servico.finalizar();

    expect(registrar.mock.calls[0][0].observacao).toBeNull();
  });

  it('finalizar sem série concluída lança erro e mantém o treino', async () => {
    const servico = criar();
    servico.iniciar(ficha, divisao);
    const antes = servico.emAndamento();

    await expect(servico.finalizar()).rejects.toThrow('Conclua pelo menos uma série para finalizar o treino.');

    expect(servico.emAndamento()).toEqual(antes);
    expect(JSON.parse(localStorage.getItem('sessao-em-andamento:u1')!)).toEqual(antes);
    expect(servico.pendentes()).toBe(0);
    expect(registrar).not.toHaveBeenCalled();
  });

  it('falha de rede mantém na fila e devolve pendente', async () => {
    registrar.mockRejectedValue(erroHttp(0));
    const servico = criar();
    const sessao = servico.iniciar(ficha, divisao);
    servico.atualizarSerie(0, 0, { concluida: true });

    const resultado = await servico.finalizar();

    expect(resultado).toBe('pendente');
    expect(servico.emAndamento()).toBeNull();
    expect(servico.pendentes()).toBe(1);
    expect(pendentesNoStorage().map((s) => s.clientId)).toEqual([sessao.clientId]);
    expect(servico.comErro()).toEqual([]);
  });

  it('5xx mantém na fila', async () => {
    registrar.mockRejectedValue(erroHttp(503));
    const servico = criar();
    servico.iniciar(ficha, divisao);
    servico.atualizarSerie(0, 0, { concluida: true });

    expect(await servico.finalizar()).toBe('pendente');
    expect(servico.pendentes()).toBe(1);
    expect(servico.comErro()).toEqual([]);
  });

  it('reenvioDepoisDeFalhaDeRedeNaoDuplica', async () => {
    registrar.mockRejectedValueOnce(erroHttp(0)).mockResolvedValueOnce(sessaoSalva);
    const servico = criar();
    const sessao = servico.iniciar(ficha, divisao);
    servico.atualizarSerie(0, 0, { concluida: true });
    await servico.finalizar();

    await servico.enviarPendentes();

    expect(registrar).toHaveBeenCalledTimes(2);
    expect(registrar.mock.calls[0][0].clientId).toBe(sessao.clientId);
    expect(registrar.mock.calls[1][0].clientId).toBe(sessao.clientId);
    expect(servico.pendentes()).toBe(0);
    expect(pendentesNoStorage()).toEqual([]);
  });

  it('fila restaurada do storage é enviada em ordem', async () => {
    registrar.mockRejectedValue(erroHttp(0));
    const servico = criar();
    const ids: string[] = [];
    for (let i = 0; i < 3; i++) {
      ids.push(servico.iniciar(ficha, divisao).clientId);
      servico.atualizarSerie(0, 0, { concluida: true });
      await servico.finalizar();
    }
    expect(servico.pendentes()).toBe(3);

    registrar.mockReset();
    registrar.mockResolvedValue(sessaoSalva);
    const restaurado = recriar();
    expect(restaurado.pendentes()).toBe(3);
    await restaurado.enviarPendentes();

    expect(registrar.mock.calls.map((c) => c[0].clientId)).toEqual(ids);
    expect(restaurado.pendentes()).toBe(0);
  });

  it('evento online dispara o envio', async () => {
    registrar.mockRejectedValueOnce(erroHttp(0)).mockResolvedValueOnce(sessaoSalva);
    const servico = criar();
    servico.iniciar(ficha, divisao);
    servico.atualizarSerie(0, 0, { concluida: true });
    await servico.finalizar();
    expect(servico.pendentes()).toBe(1);

    window.dispatchEvent(new Event('online'));
    await esperar();

    expect(registrar).toHaveBeenCalledTimes(2);
    expect(servico.pendentes()).toBe(0);
  });

  it('para de escutar online ao ser destruído', async () => {
    registrar.mockRejectedValue(erroHttp(0));
    const servico = criar();
    servico.iniciar(ficha, divisao);
    servico.atualizarSerie(0, 0, { concluida: true });
    await servico.finalizar();
    registrar.mockClear();

    TestBed.resetTestingModule();
    window.dispatchEvent(new Event('online'));
    await esperar();

    expect(registrar).not.toHaveBeenCalled();
  });

  it('recusaDoServidorMoveParaComErro', async () => {
    const detail = 'O horário de fim está no futuro. Confira o relógio do celular.';
    registrar.mockRejectedValue(erroHttp(400, { title: 'Requisição inválida', detail }));
    const servico = criar();
    const sessao = servico.iniciar(ficha, divisao);
    servico.atualizarSerie(0, 0, { concluida: true });

    await servico.finalizar();

    expect(servico.comErro().length).toBe(1);
    expect(servico.comErro()[0].mensagem).toBe(detail);
    expect(servico.comErro()[0].sessao.clientId).toBe(sessao.clientId);
    expect(servico.pendentes()).toBe(0);
    expect(pendentesNoStorage()).toEqual([]);
    expect(JSON.parse(localStorage.getItem('sessoes-com-erro:u1')!)).toEqual(servico.comErro());
    expect(recriar().comErro()).toEqual(servico.comErro());
  });

  it('recusa sem detail usa mensagem padrão e segue para o próximo', async () => {
    registrar.mockRejectedValue(erroHttp(0));
    const servico = criar();
    const primeiro = servico.iniciar(ficha, divisao).clientId;
    servico.atualizarSerie(0, 0, { concluida: true });
    await servico.finalizar();
    const segundo = servico.iniciar(ficha, divisao).clientId;
    servico.atualizarSerie(0, 0, { concluida: true });
    await servico.finalizar();

    registrar.mockReset();
    registrar.mockRejectedValueOnce(erroHttp(422, 'texto qualquer')).mockResolvedValueOnce(sessaoSalva);
    await servico.enviarPendentes();

    expect(registrar.mock.calls.map((c) => c[0].clientId)).toEqual([primeiro, segundo]);
    expect(servico.comErro()).toEqual([
      { sessao: expect.objectContaining({ clientId: primeiro }), mensagem: 'O servidor recusou este treino.' },
    ]);
    expect(servico.pendentes()).toBe(0);
  });

  it('401 mantém na fila', async () => {
    registrar.mockRejectedValue(erroHttp(401, { title: 'Não autenticado', detail: 'Token expirado.' }));
    const servico = criar();
    servico.iniciar(ficha, divisao);
    servico.atualizarSerie(0, 0, { concluida: true });

    expect(await servico.finalizar()).toBe('pendente');

    expect(servico.pendentes()).toBe(1);
    expect(pendentesNoStorage().length).toBe(1);
    expect(servico.comErro()).toEqual([]);
  });

  it('erro de rede interrompe a fila sem tentar os seguintes', async () => {
    registrar.mockRejectedValue(erroHttp(0));
    const servico = criar();
    for (let i = 0; i < 2; i++) {
      servico.iniciar(ficha, divisao);
      servico.atualizarSerie(0, 0, { concluida: true });
      await servico.finalizar();
    }
    registrar.mockClear();

    await servico.enviarPendentes();

    expect(registrar).toHaveBeenCalledTimes(1);
    expect(servico.pendentes()).toBe(2);
  });

  it('envios concorrentes não duplicam', async () => {
    registrar.mockRejectedValue(erroHttp(0));
    const servico = criar();
    for (let i = 0; i < 2; i++) {
      servico.iniciar(ficha, divisao);
      servico.atualizarSerie(0, 0, { concluida: true });
      await servico.finalizar();
    }
    registrar.mockReset();
    let liberar!: () => void;
    const bloqueio = new Promise<void>((resolve) => (liberar = resolve));
    registrar.mockImplementation(async () => {
      await bloqueio;
      return sessaoSalva;
    });

    const a = servico.enviarPendentes();
    const b = servico.enviarPendentes();
    window.dispatchEvent(new Event('online'));
    expect(b).toBe(a);
    liberar();
    await Promise.all([a, b]);

    const ids = registrar.mock.calls.map((c) => c[0].clientId);
    expect(ids.length).toBe(2);
    expect(new Set(ids).size).toBe(2);
    expect(servico.pendentes()).toBe(0);
  });

  it('envio com fila vazia não bloqueia envios seguintes', async () => {
    registrar.mockResolvedValue(sessaoSalva);
    const servico = criar();
    await servico.enviarPendentes();

    servico.iniciar(ficha, divisao);
    servico.atualizarSerie(0, 0, { concluida: true });

    expect(await servico.finalizar()).toBe('enviado');
    expect(registrar).toHaveBeenCalledTimes(1);
  });

  it('finalizar durante um envio em andamento também é enviado', async () => {
    registrar.mockRejectedValue(erroHttp(0));
    const servico = criar();
    servico.iniciar(ficha, divisao);
    servico.atualizarSerie(0, 0, { concluida: true });
    await servico.finalizar();
    registrar.mockReset();
    let liberar!: () => void;
    const bloqueio = new Promise<void>((resolve) => (liberar = resolve));
    registrar.mockImplementationOnce(async () => {
      await bloqueio;
      return sessaoSalva;
    });
    registrar.mockResolvedValue(sessaoSalva);

    const envio = servico.enviarPendentes();
    servico.iniciar(ficha, divisao);
    servico.atualizarSerie(0, 0, { concluida: true });
    const resultado = servico.finalizar();
    liberar();

    expect(await resultado).toBe('enviado');
    await envio;
    expect(registrar).toHaveBeenCalledTimes(2);
    expect(servico.pendentes()).toBe(0);
  });

  it('descartarComErro remove o item', async () => {
    registrar.mockRejectedValue(erroHttp(400, { detail: 'Recusado.' }));
    const servico = criar();
    servico.iniciar(ficha, divisao);
    servico.atualizarSerie(0, 0, { concluida: true });
    await servico.finalizar();

    servico.descartarComErro(0);

    expect(servico.comErro()).toEqual([]);
    expect(JSON.parse(localStorage.getItem('sessoes-com-erro:u1')!)).toEqual([]);
  });

  it('storageCorrompidoComecaDoZero', () => {
    localStorage.setItem('sessao-em-andamento:u1', '{x');
    localStorage.setItem('sessoes-pendentes:u1', '{"nao":"lista"}');
    localStorage.setItem('sessoes-com-erro:u1', 'null');

    const servico = criar();

    expect(servico.emAndamento()).toBeNull();
    expect(servico.pendentes()).toBe(0);
    expect(servico.comErro()).toEqual([]);
    expect(localStorage.getItem('sessao-em-andamento:u1')).toBeNull();
    expect(localStorage.getItem('sessoes-pendentes:u1')).toBeNull();
  });

  it('localStorage indisponível não lança', async () => {
    const getItem = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('SecurityError');
    });
    const setItem = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('QuotaExceededError');
    });
    try {
      registrar.mockResolvedValue(sessaoSalva);
      const servico = criar();
      expect(servico.emAndamento()).toBeNull();

      servico.iniciar(ficha, divisao);
      servico.atualizarSerie(0, 0, { concluida: true });
      expect(servico.emAndamento()!.exercicios[0].series[0].concluida).toBe(true);
      expect(await servico.finalizar()).toBe('enviado');
    } finally {
      getItem.mockRestore();
      setItem.mockRestore();
    }
  });

  it('chaves separadas por usuário', () => {
    const servico = criar();
    servico.iniciar(ficha, divisao);

    usuarioAtual.set(usuario('u2'));

    expect(servico.emAndamento()).toBeNull();
    expect(recriar().emAndamento()).toBeNull();
    expect(localStorage.getItem('sessao-em-andamento:u2')).toBeNull();

    usuarioAtual.set(usuario('u1'));
    expect(recriar().emAndamento()).not.toBeNull();
  });

  it('sem usuário opera em memória sem tocar no storage', () => {
    usuarioAtual.set(null);
    const servico = criar();

    servico.iniciar(ficha, divisao);

    expect(servico.emAndamento()).not.toBeNull();
    expect(localStorage.length).toBe(0);
  });
});
