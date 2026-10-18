import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { rotaInicial } from './usuario.model';
import { UsuarioService } from './usuario.service';

describe('UsuarioService', () => {
  let service: UsuarioService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(UsuarioService);
    http = TestBed.inject(HttpTestingController);
  });

  it('carregar faz GET /api/me e guarda no signal', async () => {
    const p = service.carregar();
    http.expectOne(`${environment.apiUrl}/api/me`).flush({ id: '1', nome: 'Ana', email: 'a@b.com', papel: 'ALUNO' });
    expect((await p).papel).toBe('ALUNO');
    expect(service.usuario()?.nome).toBe('Ana');
  });

  it('limpar zera o usuário', async () => {
    const p = service.carregar();
    http.expectOne(`${environment.apiUrl}/api/me`).flush({ id: '1', nome: 'Ana', email: 'a@b.com', papel: 'ALUNO' });
    await p;
    service.limpar();
    expect(service.usuario()).toBeNull();
  });

  it('rotaInicial mapeia os papéis', () => {
    expect(rotaInicial('PERSONAL')).toBe('/personal/dashboard');
    expect(rotaInicial('ALUNO')).toBe('/aluno/treino');
  });
});
