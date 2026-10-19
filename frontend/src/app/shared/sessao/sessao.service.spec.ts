import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TimeoutError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SILENCIOSO } from '../../core/http/silencioso';
import { Ficha } from '../../personal/fichas/ficha.model';
import {
  FichaAtiva,
  Pagina,
  Sessao,
  SessaoRequest,
  SessaoResumo,
  duracaoMinutos,
  formatarDataHora,
} from './sessao.model';
import { SessaoService } from './sessao.service';

const api = environment.apiUrl;

const ficha: Ficha = {
  id: 'f1',
  alunoId: 'a1',
  nome: 'Treino A/B',
  objetivo: null,
  frequenciaSemanal: 4,
  dataInicio: '2026-10-05',
  dataFim: null,
  status: 'ATIVA',
  vencida: false,
  encerradaEm: null,
  divisoes: [],
};

const req: SessaoRequest = {
  clientId: 'c1',
  divisaoId: 'd1',
  divisaoNome: 'A',
  iniciadaEm: '2026-10-18T10:00:00Z',
  finalizadaEm: '2026-10-18T10:47:00Z',
  observacao: null,
  series: [{ itemDivisaoId: 'i1', exercicioId: 'e1', numeroSerie: 1, repeticoes: 10, cargaKg: 20 }],
};

const sessao: Sessao = {
  id: 's1',
  clientId: 'c1',
  alunoId: 'a1',
  divisaoId: 'd1',
  divisaoNome: 'A',
  iniciadaEm: req.iniciadaEm,
  finalizadaEm: req.finalizadaEm,
  observacao: null,
  series: [
    {
      exercicioId: 'e1',
      exercicioNome: 'Supino reto',
      itemDivisaoId: 'i1',
      ordem: 1,
      numeroSerie: 1,
      repeticoes: 10,
      cargaKg: 20,
    },
  ],
};

const pagina: Pagina<SessaoResumo> = {
  content: [
    {
      id: 's1',
      divisaoNome: 'A',
      iniciadaEm: req.iniciadaEm,
      finalizadaEm: req.finalizadaEm,
      quantidadeExercicios: 1,
      quantidadeSeries: 1,
    },
  ],
  page: { size: 20, number: 1, totalElements: 21, totalPages: 2 },
};

describe('SessaoService', () => {
  let service: SessaoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(SessaoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('fichaAtiva devolve null quando a API responde 204', async () => {
    const promessa = service.fichaAtiva();
    const r = http.expectOne(`${api}/api/me/ficha-ativa`);
    expect(r.request.method).toBe('GET');
    r.flush(null, { status: 204, statusText: 'No Content' });
    expect(await promessa).toBeNull();
  });

  it('fichaAtiva devolve o objeto quando a API responde 200', async () => {
    const corpo: FichaAtiva = { ficha, proximaDivisaoId: 'd1' };
    const promessa = service.fichaAtiva();
    http.expectOne(`${api}/api/me/ficha-ativa`).flush(corpo);
    expect(await promessa).toEqual(corpo);
  });

  it('registrar faz POST /api/sessoes com o corpo', async () => {
    const promessa = service.registrar(req);
    const r = http.expectOne(`${api}/api/sessoes`);
    expect(r.request.method).toBe('POST');
    expect(r.request.body).toEqual(req);
    r.flush(sessao, { status: 201, statusText: 'Created' });
    expect(await promessa).toEqual(sessao);
  });

  it('registrar é silencioso (envio em segundo plano)', async () => {
    const promessa = service.registrar(req);
    const r = http.expectOne(`${api}/api/sessoes`);
    expect(r.request.context.get(SILENCIOSO)).toBe(true);
    r.flush(sessao);
    await promessa;
  });

  it('registrar desiste com TimeoutError depois de 15 s sem resposta', async () => {
    vi.useFakeTimers();
    try {
      const erro = service.registrar(req).catch((e: unknown) => e);
      const r = http.expectOne(`${api}/api/sessoes`);
      await vi.advanceTimersByTimeAsync(14_999);
      expect(r.cancelled).toBe(false);
      await vi.advanceTimersByTimeAsync(1);
      expect(await erro).toBeInstanceOf(TimeoutError);
      expect(r.cancelled).toBe(true);
    } finally {
      vi.useRealTimers();
    }
  });

  it('minhas envia page e size=20', async () => {
    const promessa = service.minhas(1);
    const r = http.expectOne((x) => x.url === `${api}/api/me/sessoes`);
    expect(r.request.method).toBe('GET');
    expect(r.request.params.get('page')).toBe('1');
    expect(r.request.params.get('size')).toBe('20');
    r.flush(pagina);
    expect(await promessa).toEqual(pagina);
  });

  it('doAluno faz GET /api/alunos/{id}/sessoes paginado', async () => {
    const promessa = service.doAluno('a1', 0);
    const r = http.expectOne((x) => x.url === `${api}/api/alunos/a1/sessoes`);
    expect(r.request.params.get('page')).toBe('0');
    expect(r.request.params.get('size')).toBe('20');
    r.flush(pagina);
    expect(await promessa).toEqual(pagina);
  });

  it('buscar faz GET /api/sessoes/{id}', async () => {
    const promessa = service.buscar('s1');
    const r = http.expectOne(`${api}/api/sessoes/s1`);
    expect(r.request.method).toBe('GET');
    r.flush(sessao);
    expect(await promessa).toEqual(sessao);
  });
});

describe('sessao.model', () => {
  it('formatarDataHora usa dd/MM/yyyy HH:mm no fuso do aparelho', () => {
    const iso = new Date(2026, 9, 8, 7, 5).toISOString();
    expect(formatarDataHora(iso)).toBe('08/10/2026 07:05');
  });

  it('duracaoMinutos arredonda e tem mínimo de 1', () => {
    expect(duracaoMinutos('2026-10-18T10:00:00Z', '2026-10-18T10:47:00Z')).toBe(47);
    expect(duracaoMinutos('2026-10-18T10:00:00Z', '2026-10-18T10:00:20Z')).toBe(1);
    expect(duracaoMinutos('2026-10-18T10:00:00Z', '2026-10-18T10:10:40Z')).toBe(11);
  });
});
