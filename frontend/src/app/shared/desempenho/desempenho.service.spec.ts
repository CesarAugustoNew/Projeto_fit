import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { DashboardAluno, SemanaAderencia, aderenciaMedia } from './desempenho.model';
import { DesempenhoService } from './desempenho.service';

const api = environment.apiUrl;

function semana(percentual: number | null): SemanaAderencia {
  return { inicio: '2026-10-12', fim: '2026-10-18', realizados: 0, planejado: 0, percentual };
}

describe('DesempenhoService', () => {
  let service: DesempenhoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(DesempenhoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('exercicios do aluno logado usa /api/me', async () => {
    const p = service.exercicios({ tipo: 'aluno-logado' });
    const req = http.expectOne(`${api}/api/me/desempenho/exercicios`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 'e1', nome: 'Supino', grupoMuscular: 'PEITO' }]);
    expect(await p).toEqual([{ id: 'e1', nome: 'Supino', grupoMuscular: 'PEITO' }]);
  });

  it('exercicios do personal usa /api/alunos/{id}', async () => {
    const p = service.exercicios({ tipo: 'aluno', alunoId: 'a1' });
    http.expectOne(`${api}/api/alunos/a1/desempenho/exercicios`).flush([]);
    expect(await p).toEqual([]);
  });

  it('evolucao envia exercicioId nos dois alvos', async () => {
    const p1 = service.evolucao({ tipo: 'aluno-logado' }, 'e1');
    const r1 = http.expectOne((r) => r.url === `${api}/api/me/desempenho/evolucao`);
    expect(r1.request.params.get('exercicioId')).toBe('e1');
    r1.flush([{ data: '2026-10-01', sessaoId: 's1', cargaMaxima: 80, volume: 800 }]);
    expect((await p1)[0].volume).toBe(800);

    const p2 = service.evolucao({ tipo: 'aluno', alunoId: 'a1' }, 'e2');
    const r2 = http.expectOne((r) => r.url === `${api}/api/alunos/a1/desempenho/evolucao`);
    expect(r2.request.params.get('exercicioId')).toBe('e2');
    r2.flush([]);
    expect(await p2).toEqual([]);
  });

  it('aderencia envia semanas=8 por padrao e aceita outro valor', async () => {
    const p1 = service.aderencia({ tipo: 'aluno-logado' });
    const r1 = http.expectOne((r) => r.url === `${api}/api/me/desempenho/aderencia`);
    expect(r1.request.params.get('semanas')).toBe('8');
    r1.flush([]);
    await p1;

    const p2 = service.aderencia({ tipo: 'aluno', alunoId: 'a1' }, 12);
    const r2 = http.expectOne((r) => r.url === `${api}/api/alunos/a1/desempenho/aderencia`);
    expect(r2.request.params.get('semanas')).toBe('12');
    r2.flush([]);
    await p2;
  });

  it('dashboard busca /api/dashboard', async () => {
    const item: DashboardAluno = {
      alunoId: 'a1',
      nome: 'Ana',
      realizadosSemana: 1,
      planejadoSemana: 3,
      diasSemTreinar: null,
      fichaAtivaId: null,
      fichaVencida: false,
      sumiu: false,
    };
    const p = service.dashboard();
    http.expectOne(`${api}/api/dashboard`).flush([item]);
    expect(await p).toEqual([item]);
  });
});

describe('aderenciaMedia', () => {
  it('ignora percentuais nulos e arredonda', () => {
    expect(aderenciaMedia([semana(100), semana(null), semana(50)])).toBe(75);
    expect(aderenciaMedia([semana(100), semana(100), semana(50)])).toBe(83);
  });

  it('devolve null quando todos sao nulos ou vazio', () => {
    expect(aderenciaMedia([semana(null), semana(null)])).toBeNull();
    expect(aderenciaMedia([])).toBeNull();
  });
});
