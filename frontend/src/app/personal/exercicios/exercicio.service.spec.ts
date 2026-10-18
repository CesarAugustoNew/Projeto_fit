import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { Exercicio, GRUPOS, NovoExercicio, ROTULO_GRUPO, linkSeguro } from './exercicio.model';
import { ExercicioService } from './exercicio.service';

const base = `${environment.apiUrl}/api/exercicios`;

const exercicio: Exercicio = {
  id: '1',
  nome: 'Supino reto',
  grupoMuscular: 'PEITO',
  videoUrl: null,
  observacao: null,
  ativo: true,
};

describe('ExercicioService', () => {
  let service: ExercicioService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ExercicioService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('listar sem filtro não envia query', async () => {
    const promessa = service.listar();
    const req = http.expectOne(base);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.keys()).toEqual([]);
    req.flush([exercicio]);
    expect(await promessa).toEqual([exercicio]);
  });

  it('listar envia ativo=false e grupo=COSTAS', async () => {
    const promessa = service.listar({ ativo: false, grupo: 'COSTAS' });
    const req = http.expectOne((r) => r.url === base);
    expect(req.request.params.get('ativo')).toBe('false');
    expect(req.request.params.get('grupo')).toBe('COSTAS');
    req.flush([]);
    expect(await promessa).toEqual([]);
  });

  it('criar faz POST', async () => {
    const dados: NovoExercicio = { nome: 'Supino reto', grupoMuscular: 'PEITO', videoUrl: null, observacao: null };
    const promessa = service.criar(dados);
    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(dados);
    req.flush(exercicio);
    expect(await promessa).toEqual(exercicio);
  });

  it('atualizar faz PUT em /{id}', async () => {
    const dados = { nome: 'Supino', grupoMuscular: 'PEITO' as const, videoUrl: null, observacao: null, ativo: false };
    const promessa = service.atualizar('1', dados);
    const req = http.expectOne(`${base}/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(dados);
    req.flush({ ...exercicio, ativo: false });
    expect((await promessa).ativo).toBe(false);
  });

  it('buscar faz GET /{id}', async () => {
    const promessa = service.buscar('1');
    const req = http.expectOne(`${base}/1`);
    expect(req.request.method).toBe('GET');
    req.flush(exercicio);
    expect(await promessa).toEqual(exercicio);
  });
});

describe('exercicio.model', () => {
  it('linkSeguro aceita http(s) e recusa javascript:', () => {
    expect(linkSeguro('HTTPS://x.com')).toBe('HTTPS://x.com');
    expect(linkSeguro('http://x.com')).toBe('http://x.com');
    expect(linkSeguro('javascript:alert(1)')).toBeNull();
    expect(linkSeguro(null)).toBeNull();
  });

  it('ROTULO_GRUPO cobre os 13 grupos', () => {
    expect(Object.keys(ROTULO_GRUPO).length).toBe(13);
    expect(GRUPOS.length).toBe(13);
    for (const g of GRUPOS) expect(ROTULO_GRUPO[g]).toBeTruthy();
  });
});
