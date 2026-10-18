import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FichaAtiva, Pagina, Sessao, SessaoRequest, SessaoResumo } from './sessao.model';

const TAMANHO_PAGINA = 20;

@Injectable({ providedIn: 'root' })
export class SessaoService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  async fichaAtiva(): Promise<FichaAtiva | null> {
    const res = await firstValueFrom(
      this.http.get<FichaAtiva>(`${this.api}/api/me/ficha-ativa`, { observe: 'response' }),
    );
    return res.status === 204 || res.body == null ? null : res.body;
  }

  registrar(req: SessaoRequest): Promise<Sessao> {
    return firstValueFrom(this.http.post<Sessao>(`${this.api}/api/sessoes`, req));
  }

  minhas(pagina: number): Promise<Pagina<SessaoResumo>> {
    return firstValueFrom(
      this.http.get<Pagina<SessaoResumo>>(`${this.api}/api/me/sessoes`, { params: this.params(pagina) }),
    );
  }

  doAluno(alunoId: string, pagina: number): Promise<Pagina<SessaoResumo>> {
    return firstValueFrom(
      this.http.get<Pagina<SessaoResumo>>(`${this.api}/api/alunos/${alunoId}/sessoes`, {
        params: this.params(pagina),
      }),
    );
  }

  buscar(id: string): Promise<Sessao> {
    return firstValueFrom(this.http.get<Sessao>(`${this.api}/api/sessoes/${id}`));
  }

  private params(pagina: number): HttpParams {
    return new HttpParams().set('page', pagina).set('size', TAMANHO_PAGINA);
  }
}
