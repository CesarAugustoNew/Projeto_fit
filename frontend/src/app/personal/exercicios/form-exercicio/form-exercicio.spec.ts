import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, provideRouter } from '@angular/router';
import { Exercicio } from '../exercicio.model';
import { ExercicioService } from '../exercicio.service';
import { FormExercicio } from './form-exercicio';

const exercicio = (extra: Partial<Exercicio> = {}): Exercicio => ({
  id: 'e1',
  nome: 'Supino reto',
  grupoMuscular: 'PEITO',
  videoUrl: null,
  observacao: null,
  ativo: true,
  ...extra,
});

describe('FormExercicio', () => {
  let criar: ReturnType<typeof vi.fn>;
  let atualizar: ReturnType<typeof vi.fn>;
  let buscar: ReturnType<typeof vi.fn>;
  let open: ReturnType<typeof vi.fn>;
  let navigate: ReturnType<typeof vi.fn>;

  async function montar(id?: string, falhaCarga = false) {
    criar = vi.fn().mockResolvedValue(exercicio());
    atualizar = vi.fn().mockResolvedValue(exercicio());
    buscar = vi.fn().mockResolvedValue(exercicio({ videoUrl: 'https://youtu.be/x' }));
    if (falhaCarga) {
      buscar.mockRejectedValue(new Error('404'));
    }
    open = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: ExercicioService, useValue: { criar, atualizar, buscar } },
        { provide: MatSnackBar, useValue: { open } },
      ],
    });
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true) as never;
    const fixture = TestBed.createComponent(FormExercicio);
    if (id) {
      fixture.componentRef.setInput('id', id);
    }
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  type Fx = { nativeElement: HTMLElement; componentInstance: unknown; detectChanges(): void };
  const el = (f: Fx) => f.nativeElement;
  const campo = (f: Fx, nome: string) =>
    el(f).querySelector(`[formControlName="${nome}"]`) as HTMLInputElement;
  const digitar = (f: Fx, nome: string, valor: string) => {
    const c = campo(f, nome);
    c.value = valor;
    c.dispatchEvent(new Event('input'));
  };
  const escolherGrupo = (f: Fx, grupo: string) =>
    (f.componentInstance as { form: { controls: { grupoMuscular: { setValue(v: string): void } } } }).form.controls.grupoMuscular.setValue(grupo);
  const enviar = (f: Fx) =>
    (el(f).querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
  const salvarBtn = (f: Fx) => el(f).querySelector('button[type="submit"]') as HTMLButtonElement;

  it('novo: envia criar com opcionais vazios como null e volta à lista', async () => {
    const f = await montar();
    expect(el(f).textContent).toContain('Novo exercício');
    expect(el(f).querySelector('mat-slide-toggle')).toBeNull();
    digitar(f, 'nome', '  Remada  ');
    escolherGrupo(f, 'COSTAS');
    digitar(f, 'videoUrl', '   ');
    enviar(f);
    await f.whenStable();
    expect(criar).toHaveBeenCalledWith({
      nome: 'Remada',
      grupoMuscular: 'COSTAS',
      videoUrl: null,
      observacao: null,
    });
    expect(open).toHaveBeenCalledWith('Exercício criado.', undefined, expect.anything());
    expect(navigate).toHaveBeenCalledWith(['/personal/exercicios']);
  });

  it('nome só com espaços mostra erro e não envia', async () => {
    const f = await montar();
    digitar(f, 'nome', '   ');
    escolherGrupo(f, 'COSTAS');
    campo(f, 'nome').dispatchEvent(new Event('blur'));
    enviar(f);
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(criar).not.toHaveBeenCalled();
    expect(el(f).textContent).toContain('Informe o nome.');
  });

  it('grupo não escolhido mostra erro e não envia', async () => {
    const f = await montar();
    digitar(f, 'nome', 'Remada');
    enviar(f);
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(criar).not.toHaveBeenCalled();
    expect(el(f).textContent).toContain('Escolha o grupo muscular.');
  });

  it('link sem http mostra erro e não envia', async () => {
    const f = await montar();
    digitar(f, 'nome', 'Remada');
    escolherGrupo(f, 'COSTAS');
    digitar(f, 'videoUrl', 'javascript:alert(1)');
    campo(f, 'videoUrl').dispatchEvent(new Event('blur'));
    enviar(f);
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(criar).not.toHaveBeenCalled();
    expect(el(f).textContent).toContain('Informe um link começando com http:// ou https://.');
  });

  it('não envia duas vezes enquanto salva', async () => {
    const f = await montar();
    criar.mockReturnValue(new Promise(() => {}));
    digitar(f, 'nome', 'Remada');
    escolherGrupo(f, 'COSTAS');
    enviar(f);
    enviar(f);
    expect(criar).toHaveBeenCalledTimes(1);
  });

  it('editar: carrega, mostra o toggle e envia atualizar com ativo', async () => {
    const f = await montar('e1');
    expect(buscar).toHaveBeenCalledWith('e1');
    expect(el(f).textContent).toContain('Editar exercício');
    expect(campo(f, 'nome').value).toBe('Supino reto');
    expect(campo(f, 'videoUrl').value).toBe('https://youtu.be/x');
    expect(el(f).querySelector('mat-slide-toggle')).not.toBeNull();
    (el(f).querySelector('mat-slide-toggle button') as HTMLButtonElement).click();
    f.detectChanges();
    expect(el(f).textContent).toContain('Exercícios inativos não aparecem para novas fichas.');
    enviar(f);
    await f.whenStable();
    expect(atualizar).toHaveBeenCalledWith('e1', {
      nome: 'Supino reto',
      grupoMuscular: 'PEITO',
      videoUrl: 'https://youtu.be/x',
      observacao: null,
      ativo: false,
    });
    expect(open).toHaveBeenCalledWith('Exercício atualizado.', undefined, expect.anything());
    expect(navigate).toHaveBeenCalledWith(['/personal/exercicios']);
  });

  it('editar: se a carga falhar, volta para a lista', async () => {
    await montar('e1', true);
    expect(navigate).toHaveBeenCalledWith(['/personal/exercicios']);
  });

  it('erro ao salvar mantém no formulário e reabilita o botão', async () => {
    const f = await montar();
    criar.mockRejectedValue(new Error('409'));
    digitar(f, 'nome', 'Remada');
    escolherGrupo(f, 'COSTAS');
    enviar(f);
    await f.whenStable();
    f.detectChanges();
    expect(navigate).not.toHaveBeenCalled();
    expect(salvarBtn(f).disabled).toBe(false);
  });
});
