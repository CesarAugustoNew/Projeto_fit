import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Exercicio } from '../exercicio.model';
import { ExercicioService } from '../exercicio.service';
import { ListaExercicios } from './lista-exercicios';

const exercicio = (id: string, nome: string, videoUrl: string | null = null): Exercicio => ({
  id,
  nome,
  grupoMuscular: 'PEITO',
  videoUrl,
  observacao: null,
  ativo: true,
});

describe('ListaExercicios', () => {
  let listar: ReturnType<typeof vi.fn>;

  async function criar(retorno: Exercicio[]) {
    listar = vi.fn().mockResolvedValue(retorno);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: ExercicioService, useValue: { listar } }],
    });
    const fixture = TestBed.createComponent(ListaExercicios);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  const texto = (f: { nativeElement: HTMLElement }) => f.nativeElement.textContent ?? '';

  it('carrega ativos ao abrir e mostra nome e grupo', async () => {
    const f = await criar([exercicio('1', 'Supino reto')]);
    expect(listar).toHaveBeenCalledWith({ ativo: true });
    expect(texto(f)).toContain('Supino reto');
    expect(texto(f)).toContain('Peito');
  });

  it('inativosChamaListarComAtivoFalse', async () => {
    const f = await criar([]);
    const botoes = f.nativeElement.querySelectorAll('mat-button-toggle button');
    (botoes[1] as HTMLButtonElement).click();
    await f.whenStable();
    f.detectChanges();
    expect(listar).toHaveBeenLastCalledWith({ ativo: false });
    expect(texto(f)).toContain('Nenhum exercício inativo.');
  });

  it('trocar o grupo recarrega com grupo', async () => {
    const f = await criar([]);
    (f.nativeElement.querySelector('.mat-mdc-select-trigger') as HTMLElement).click();
    f.detectChanges();
    const opcoes = Array.from(document.querySelectorAll('mat-option')) as HTMLElement[];
    expect(opcoes.length).toBe(14);
    expect(opcoes[0].textContent).toContain('Todos');
    opcoes.find((o) => o.textContent?.includes('Costas'))!.click();
    await f.whenStable();
    f.detectChanges();
    expect(listar).toHaveBeenLastCalledWith({ ativo: true, grupo: 'COSTAS' });
  });

  it('busca filtra por nome sem diferenciar maiúsculas', async () => {
    const f = await criar([exercicio('1', 'Supino reto'), exercicio('2', 'Remada')]);
    const input = f.nativeElement.querySelector('input[type=search]') as HTMLInputElement;
    input.value = 'SUPINO';
    input.dispatchEvent(new Event('input'));
    f.detectChanges();
    expect(texto(f)).toContain('Supino reto');
    expect(texto(f)).not.toContain('Remada');
    input.value = 'zzz';
    input.dispatchEvent(new Event('input'));
    f.detectChanges();
    expect(texto(f)).toContain('Nenhum exercício encontrado.');
  });

  it('linkDoVideoAbreEmNovaAbaComNoopener', async () => {
    const f = await criar([
      exercicio('1', 'Supino', 'https://youtu.be/x'),
      exercicio('2', 'Malicioso', 'javascript:alert(1)'),
    ]);
    const links = f.nativeElement.querySelectorAll('a[aria-label="Ver vídeo"]');
    expect(links.length).toBe(1);
    const a = links[0] as HTMLAnchorElement;
    expect(a.getAttribute('href')).toBe('https://youtu.be/x');
    expect(a.getAttribute('target')).toBe('_blank');
    expect(a.getAttribute('rel')).toContain('noopener');
    // o link do vídeo não é filho do link que navega para a edição
    expect(a.closest('a.item')).toBeNull();
  });

  it('item leva à edição', async () => {
    const f = await criar([exercicio('abc', 'Supino')]);
    const link = f.nativeElement.querySelector('a.item') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe('/personal/exercicios/abc/editar');
  });
});
