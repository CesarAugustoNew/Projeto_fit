import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { UsuarioService } from '../../core/usuario/usuario.service';
import { DashboardAluno } from '../../shared/desempenho/desempenho.model';
import { DesempenhoService } from '../../shared/desempenho/desempenho.service';
import { Dashboard } from './dashboard';

const aluno = (extra: Partial<DashboardAluno> = {}): DashboardAluno => ({
  alunoId: 'a1',
  nome: 'Ana',
  realizadosSemana: 2,
  planejadoSemana: 3,
  diasSemTreinar: 3,
  fichaAtivaId: 'f1',
  fichaVencida: false,
  sumiu: false,
  ...extra,
});

describe('Dashboard', () => {
  async function criar(retorno: DashboardAluno[] | Error) {
    const dashboard = vi.fn();
    if (retorno instanceof Error) dashboard.mockRejectedValue(retorno);
    else dashboard.mockResolvedValue(retorno);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: DesempenhoService, useValue: { dashboard } },
        { provide: UsuarioService, useValue: { usuario: () => ({ nome: 'Carlos' }) } },
      ],
    });
    const fixture = TestBed.createComponent(Dashboard);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  const texto = (f: { nativeElement: HTMLElement }) => (f.nativeElement.textContent ?? '').replace(/\s+/g, ' ');

  it('mostra os indicadores de ativos, sumiram e fichas vencidas', async () => {
    const f = await criar([
      aluno({ alunoId: '1', sumiu: true }),
      aluno({ alunoId: '2', fichaVencida: true }),
      aluno({ alunoId: '3', sumiu: true, fichaVencida: true }),
    ]);
    const indicadores = Array.from<Element>(f.nativeElement.querySelectorAll('.indicador')).map((e) =>
      `${e.querySelector('.valor')!.textContent} ${e.querySelector('.rotulo')!.textContent}`,
    );
    expect(texto(f)).toContain('Olá, Carlos');
    expect(indicadores).toEqual(['3 Alunos ativos', '2 Sumiram', '2 Fichas vencidas']);
  });

  it('mostra treinos da semana e último treino', async () => {
    const f = await criar([aluno()]);
    expect(texto(f)).toContain('2/3 treinos nesta semana');
    expect(texto(f)).toContain('Último treino há 3 dias');
  });

  it('descreve hoje e ontem', async () => {
    const f = await criar([
      aluno({ alunoId: '1', diasSemTreinar: 0 }),
      aluno({ alunoId: '2', diasSemTreinar: 1 }),
    ]);
    expect(texto(f)).toContain('Último treino hoje');
    expect(texto(f)).toContain('Último treino ontem');
  });

  it('mostraNuncaTreinou', async () => {
    const f = await criar([aluno({ diasSemTreinar: null })]);
    expect(texto(f)).toContain('Nunca treinou');
  });

  it('sem ficha mostra Sem ficha', async () => {
    const f = await criar([
      aluno({ alunoId: '1', planejadoSemana: 0, realizadosSemana: 1, fichaAtivaId: null }),
      aluno({ alunoId: '2', planejadoSemana: 0, realizadosSemana: 2, fichaAtivaId: null }),
    ]);
    expect(texto(f)).toContain('1 treino nesta semana · Sem ficha');
    expect(texto(f)).toContain('2 treinos nesta semana · Sem ficha');
  });

  it('etiquetas Sumiu e Ficha vencida', async () => {
    const f = await criar([aluno({ sumiu: true, fichaVencida: true })]);
    const etiquetas = Array.from(f.nativeElement.querySelectorAll('.etiqueta')).map((e) => (e as HTMLElement).textContent!.trim());
    expect(etiquetas).toEqual(['Sumiu', 'Ficha vencida']);
  });

  it('sem alerta não mostra etiquetas', async () => {
    const f = await criar([aluno()]);
    expect(f.nativeElement.querySelectorAll('.etiqueta').length).toBe(0);
  });

  it('item leva ao detalhe do aluno', async () => {
    const f = await criar([aluno({ alunoId: 'abc' })]);
    const link = f.nativeElement.querySelector('a.item') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe('/personal/alunos/abc');
  });

  it('sem alunos mostra a mensagem e o link', async () => {
    const f = await criar([]);
    expect(texto(f)).toContain('Nenhum aluno ativo ainda.');
    const link = f.nativeElement.querySelector('a.ir-alunos') as HTMLAnchorElement;
    expect(link.textContent).toContain('Ir para Alunos');
    expect(link.getAttribute('href')).toBe('/personal/alunos');
  });

  it('mostra erro quando falha', async () => {
    const f = await criar(new Error('x'));
    expect(texto(f)).toContain('Não foi possível carregar o dashboard.');
  });
});
