import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Aluno } from '../aluno.model';
import { AlunoService } from '../aluno.service';
import { ListaAlunos } from './lista-alunos';

const aluno = (id: string, nome: string, email: string, status: Aluno['status'] = 'ATIVO'): Aluno => ({
  id,
  nome,
  email,
  telefone: null,
  dataNascimento: null,
  objetivo: null,
  observacoes: null,
  status,
  criadoEm: '2026-10-01T10:00:00Z',
});

describe('ListaAlunos', () => {
  let listar: ReturnType<typeof vi.fn>;

  async function criar(retorno: Aluno[]) {
    listar = vi.fn().mockResolvedValue(retorno);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AlunoService, useValue: { listar } }],
    });
    const fixture = TestBed.createComponent(ListaAlunos);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  const texto = (f: { nativeElement: HTMLElement }) => f.nativeElement.textContent ?? '';

  it('carrega ativos ao abrir e mostra nome, e-mail e status', async () => {
    const f = await criar([aluno('1', 'Ana', 'ana@x.com', 'CONVIDADO')]);
    expect(listar).toHaveBeenCalledWith();
    expect(texto(f)).toContain('Ana');
    expect(texto(f)).toContain('ana@x.com');
    expect(texto(f)).toContain('Convite pendente');
  });

  it('Inativos chama listar("INATIVO")', async () => {
    const f = await criar([]);
    const botoes = f.nativeElement.querySelectorAll('mat-button-toggle button');
    (botoes[1] as HTMLButtonElement).click();
    await f.whenStable();
    f.detectChanges();
    expect(listar).toHaveBeenLastCalledWith('INATIVO');
    expect(texto(f)).toContain('Nenhum aluno inativo.');
  });

  it('busca filtra por nome ou e-mail sem diferenciar maiúsculas', async () => {
    const f = await criar([aluno('1', 'Ana', 'ana@x.com'), aluno('2', 'Bruno', 'bruno@x.com')]);
    const input = f.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = 'ANA';
    input.dispatchEvent(new Event('input'));
    f.detectChanges();
    expect(texto(f)).toContain('Ana');
    expect(texto(f)).not.toContain('Bruno');
    input.value = 'BRUNO@X';
    input.dispatchEvent(new Event('input'));
    f.detectChanges();
    expect(texto(f)).toContain('Bruno');
    expect(texto(f)).not.toContain('ana@x.com');
  });

  it('mostra mensagens de lista vazia', async () => {
    const f = await criar([]);
    expect(texto(f)).toContain('Nenhum aluno cadastrado ainda.');
  });

  it('mostra mensagem quando a busca não encontra', async () => {
    const g = await criar([aluno('1', 'Ana', 'ana@x.com')]);
    const input = g.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = 'zzz';
    input.dispatchEvent(new Event('input'));
    g.detectChanges();
    expect(texto(g)).toContain('Nenhum aluno encontrado.');
  });

  it('item leva ao detalhe', async () => {
    const f = await criar([aluno('abc', 'Ana', 'ana@x.com')]);
    const link = f.nativeElement.querySelector('a.item') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe('/personal/alunos/abc');
  });
});
