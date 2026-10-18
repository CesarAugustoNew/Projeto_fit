import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';
import { authInterceptor, ehChamadaApi } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let ctrl: HttpTestingController;
  const auth = { token: vi.fn() };

  beforeEach(() => {
    auth.token.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpClient);
    ctrl = TestBed.inject(HttpTestingController);
  });

  it('adiciona Bearer nas chamadas à API', async () => {
    auth.token.mockResolvedValue('abc');
    http.get(`${environment.apiUrl}/api/me`).subscribe();
    const req = await vi.waitFor(() => ctrl.expectOne(`${environment.apiUrl}/api/me`));
    expect(req.request.headers.get('Authorization')).toBe('Bearer abc');
  });

  it('não envia token para outras origens', () => {
    auth.token.mockResolvedValue('abc');
    http.get('https://exemplo.com/x').subscribe();
    const req = ctrl.expectOne('https://exemplo.com/x');
    expect(req.request.headers.has('Authorization')).toBe(false);
  });

  it('não adiciona header sem sessão', async () => {
    auth.token.mockResolvedValue(null);
    http.get(`${environment.apiUrl}/api/me`).subscribe();
    const req = await vi.waitFor(() => ctrl.expectOne(`${environment.apiUrl}/api/me`));
    expect(req.request.headers.has('Authorization')).toBe(false);
  });

  it('apiUrl vazia só casa URLs relativas', () => {
    expect(ehChamadaApi('/api/me', '')).toBe(true);
    expect(ehChamadaApi('https://exemplo.com/x', '')).toBe(false);
    expect(ehChamadaApi('http://exemplo.com/x', '')).toBe(false);
    expect(ehChamadaApi('//exemplo.com/x', '')).toBe(false);
    expect(ehChamadaApi('http://localhost:8080/api/me', 'http://localhost:8080')).toBe(true);
  });

  it('não casa origens parecidas com a apiUrl', () => {
    const api = 'http://localhost:8080';
    expect(ehChamadaApi('http://localhost:80801/x', api)).toBe(false);
    expect(ehChamadaApi('http://localhost:8080.evil.com/x', api)).toBe(false);
    expect(ehChamadaApi('http://localhost:8080', api)).toBe(true);
    expect(ehChamadaApi('http://localhost:8080?a=1', api)).toBe(true);
  });
});
