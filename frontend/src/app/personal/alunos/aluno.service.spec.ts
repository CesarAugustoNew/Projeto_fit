import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { Aluno, NovoAluno } from './aluno.model';
import { AlunoService } from './aluno.service';

const base = `${environment.apiUrl}/api/alunos`;

const aluno: Aluno = {
  id: '1',
  nome: 'Ana',
  email: 'ana@x.com',
  telefone: null,
  dataNascimento: null,
  objetivo: null,
  observacoes: null,
  status: 'CONVIDADO',
  criadoEm: '2026-10-18T10:00:00Z',
};

describe('AlunoService', () => {
  let service: AlunoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AlunoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('listar sem status não envia query', async () => {
    const p = service.listar();
    const req = http.expectOne(base);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.keys()).toEqual([]);
    req.flush([aluno]);
    expect(await p).toEqual([aluno]);
  });

  it('listar com status envia ?status=INATIVO', async () => {
    const p = service.listar('INATIVO');
    const req = http.expectOne(`${base}?status=INATIVO`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('status')).toBe('INATIVO');
    req.flush([]);
    expect(await p).toEqual([]);
  });

  it('buscar faz GET /{id}', async () => {
    const p = service.buscar('1');
    const req = http.expectOne(`${base}/1`);
    expect(req.request.method).toBe('GET');
    req.flush(aluno);
    expect(await p).toEqual(aluno);
  });

  it('criar faz POST com os dados', async () => {
    const dados: NovoAluno = { nome: 'Ana', email: 'ana@x.com', telefone: '11999999999' };
    const p = service.criar(dados);
    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(dados);
    req.flush(aluno);
    expect(await p).toEqual(aluno);
  });

  it('atualizar faz PUT em /{id}', async () => {
    const dados = { nome: 'Ana Maria', objetivo: 'Hipertrofia' };
    const p = service.atualizar('1', dados);
    const req = http.expectOne(`${base}/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(dados);
    req.flush(aluno);
    expect(await p).toEqual(aluno);
  });

  it('alterarStatus faz PATCH /{id}/status com { status }', async () => {
    const p = service.alterarStatus('1', 'INATIVO');
    const req = http.expectOne(`${base}/1/status`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ status: 'INATIVO' });
    req.flush({ ...aluno, status: 'INATIVO' });
    expect((await p).status).toBe('INATIVO');
  });

  it('reenviarConvite faz POST /{id}/reenviar-convite', async () => {
    const p = service.reenviarConvite('1');
    const req = http.expectOne(`${base}/1/reenviar-convite`);
    expect(req.request.method).toBe('POST');
    req.flush(null, { status: 204, statusText: 'No Content' });
    await expect(p).resolves.toBeUndefined();
  });
});
