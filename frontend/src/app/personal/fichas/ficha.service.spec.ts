import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { Ficha, FichaRequest, FichaResumo, formatarData, hojeLocal } from './ficha.model';
import { FichaService } from './ficha.service';

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
  divisoes: [
    {
      id: 'd1',
      nome: 'A',
      ordem: 1,
      itens: [
        {
          id: 'i1',
          exercicioId: 'e1',
          exercicioNome: 'Supino reto',
          grupoMuscular: 'PEITO',
          ordem: 1,
          series: 3,
          repeticoes: '8-12',
          cargaKg: 22.5,
          descansoSeg: 60,
          observacao: null,
        },
      ],
    },
  ],
};

const dados: FichaRequest = {
  nome: 'Treino A/B',
  objetivo: null,
  frequenciaSemanal: 4,
  dataInicio: '2026-10-05',
  dataFim: null,
  divisoes: [
    {
      id: null,
      nome: 'A',
      itens: [
        { id: null, exercicioId: 'e1', series: 3, repeticoes: '8-12', cargaKg: 22.5, descansoSeg: null, observacao: null },
      ],
    },
  ],
};

describe('FichaService', () => {
  let service: FichaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(FichaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('listarDoAluno faz GET /api/alunos/{id}/fichas', async () => {
    const resumo: FichaResumo = {
      id: 'f1',
      nome: 'Treino A/B',
      status: 'ATIVA',
      dataInicio: '2026-10-05',
      dataFim: null,
      frequenciaSemanal: 4,
      quantidadeDivisoes: 2,
      vencida: false,
    };
    const promessa = service.listarDoAluno('a1');
    const req = http.expectOne(`${api}/api/alunos/a1/fichas`);
    expect(req.request.method).toBe('GET');
    req.flush([resumo]);
    expect(await promessa).toEqual([resumo]);
  });

  it('buscar faz GET /api/fichas/{id}', async () => {
    const promessa = service.buscar('f1');
    const req = http.expectOne(`${api}/api/fichas/f1`);
    expect(req.request.method).toBe('GET');
    req.flush(ficha);
    expect(await promessa).toEqual(ficha);
  });

  it('criar faz POST /api/alunos/{id}/fichas com o corpo', async () => {
    const promessa = service.criar('a1', dados);
    const req = http.expectOne(`${api}/api/alunos/a1/fichas`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(dados);
    req.flush(ficha);
    expect(await promessa).toEqual(ficha);
  });

  it('atualizar faz PUT /api/fichas/{id} com o corpo', async () => {
    const promessa = service.atualizar('f1', dados);
    const req = http.expectOne(`${api}/api/fichas/f1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(dados);
    req.flush(ficha);
    expect(await promessa).toEqual(ficha);
  });

  it('encerrar faz POST /api/fichas/{id}/encerrar', async () => {
    const promessa = service.encerrar('f1');
    const req = http.expectOne(`${api}/api/fichas/f1/encerrar`);
    expect(req.request.method).toBe('POST');
    req.flush({ ...ficha, status: 'ENCERRADA' });
    expect((await promessa).status).toBe('ENCERRADA');
  });
});

describe('ficha.model', () => {
  it('formatarData converte yyyy-MM-dd em dd/MM/yyyy e null em traço', () => {
    expect(formatarData('2026-10-05')).toBe('05/10/2026');
    expect(formatarData(null)).toBe('—');
  });

  it('hojeLocal devolve yyyy-MM-dd', () => {
    expect(hojeLocal()).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });
});
