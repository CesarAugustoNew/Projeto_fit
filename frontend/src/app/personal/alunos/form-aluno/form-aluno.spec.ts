import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, provideRouter } from '@angular/router';
import { Aluno } from '../aluno.model';
import { AlunoService } from '../aluno.service';
import { FormAluno } from './form-aluno';

const aluno = (extra: Partial<Aluno> = {}): Aluno => ({
  id: 'a1',
  nome: 'Ana',
  email: 'ana@x.com',
  telefone: null,
  dataNascimento: null,
  objetivo: null,
  observacoes: null,
  status: 'CONVIDADO',
  criadoEm: '2026-10-01T10:00:00Z',
  ...extra,
});

describe('FormAluno', () => {
  let criar: ReturnType<typeof vi.fn>;
  let atualizar: ReturnType<typeof vi.fn>;
  let buscar: ReturnType<typeof vi.fn>;
  let open: ReturnType<typeof vi.fn>;
  let navigate: ReturnType<typeof vi.fn>;

  async function montar(id?: string) {
    criar = vi.fn().mockResolvedValue(aluno());
    atualizar = vi.fn().mockResolvedValue(aluno());
    buscar = vi.fn().mockResolvedValue(
      aluno({ telefone: '119', objetivo: 'Emagrecer', dataNascimento: '1990-05-01' }),
    );
    open = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AlunoService, useValue: { criar, atualizar, buscar } },
        { provide: MatSnackBar, useValue: { open } },
      ],
    });
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true) as never;
    const fixture = TestBed.createComponent(FormAluno);
    if (id) {
      fixture.componentRef.setInput('id', id);
    }
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  const el = (f: { nativeElement: HTMLElement }) => f.nativeElement;
  const campo = (f: { nativeElement: HTMLElement }, nome: string) =>
    el(f).querySelector(`[formControlName="${nome}"]`) as HTMLInputElement;
  const digitar = (f: { nativeElement: HTMLElement }, nome: string, valor: string) => {
    const c = campo(f, nome);
    c.value = valor;
    c.dispatchEvent(new Event('input'));
  };
  const enviar = (f: { nativeElement: HTMLElement }) =>
    (el(f).querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));

  it('novo: envia criar com campos opcionais vazios como null e navega ao detalhe', async () => {
    const f = await montar();
    expect(el(f).textContent).toContain('Novo aluno');
    digitar(f, 'nome', 'Ana');
    digitar(f, 'email', 'ana@x.com');
    enviar(f);
    await f.whenStable();
    expect(criar).toHaveBeenCalledWith({
      nome: 'Ana',
      email: 'ana@x.com',
      telefone: null,
      dataNascimento: null,
      objetivo: null,
      observacoes: null,
    });
    expect(open).toHaveBeenCalledWith('Convite enviado para ana@x.com.', undefined, expect.anything());
    expect(navigate).toHaveBeenCalledWith(['/personal/alunos', 'a1']);
  });

  it('novo: e-mail inválido mostra erro e não envia', async () => {
    const f = await montar();
    digitar(f, 'nome', 'Ana');
    digitar(f, 'email', 'invalido');
    campo(f, 'email').dispatchEvent(new Event('blur'));
    enviar(f);
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(criar).not.toHaveBeenCalled();
    expect(el(f).textContent).toContain('Informe um e-mail válido.');
  });

  it('naoEnviaDuasVezesEnquantoSalva', async () => {
    const f = await montar();
    criar.mockReturnValue(new Promise(() => {}));
    digitar(f, 'nome', 'Ana');
    digitar(f, 'email', 'ana@x.com');
    enviar(f);
    enviar(f);
    expect(criar).toHaveBeenCalledTimes(1);
  });

  it('editar: carrega o aluno, e-mail desabilitado, envia atualizar sem e-mail', async () => {
    const f = await montar('a1');
    expect(buscar).toHaveBeenCalledWith('a1');
    expect(el(f).textContent).toContain('Editar aluno');
    expect(campo(f, 'email').disabled).toBe(true);
    expect(campo(f, 'email').value).toBe('ana@x.com');
    expect(campo(f, 'telefone').value).toBe('119');
    digitar(f, 'nome', 'Ana Maria');
    enviar(f);
    await f.whenStable();
    expect(atualizar).toHaveBeenCalledWith('a1', {
      nome: 'Ana Maria',
      telefone: '119',
      dataNascimento: '1990-05-01',
      objetivo: 'Emagrecer',
      observacoes: null,
    });
    expect(open).toHaveBeenCalledWith('Dados do aluno atualizados.', undefined, expect.anything());
    expect(navigate).toHaveBeenCalledWith(['/personal/alunos', 'a1']);
  });

  it('erro ao salvar mantém no formulário e reabilita o botão', async () => {
    const f = await montar();
    criar.mockRejectedValue(new Error('409'));
    digitar(f, 'nome', 'Ana');
    digitar(f, 'email', 'ana@x.com');
    enviar(f);
    await f.whenStable();
    f.detectChanges();
    expect(navigate).not.toHaveBeenCalled();
    const salvar = el(f).querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(salvar.disabled).toBe(false);
  });
});
