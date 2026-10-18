import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Ficha, FichaRequest, FichaResumo } from './ficha.model';

@Injectable({ providedIn: 'root' })
export class FichaService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  listarDoAluno(alunoId: string): Promise<FichaResumo[]> {
    return firstValueFrom(this.http.get<FichaResumo[]>(`${this.api}/api/alunos/${alunoId}/fichas`));
  }

  buscar(id: string): Promise<Ficha> {
    return firstValueFrom(this.http.get<Ficha>(`${this.api}/api/fichas/${id}`));
  }

  criar(alunoId: string, dados: FichaRequest): Promise<Ficha> {
    return firstValueFrom(this.http.post<Ficha>(`${this.api}/api/alunos/${alunoId}/fichas`, dados));
  }

  atualizar(id: string, dados: FichaRequest): Promise<Ficha> {
    return firstValueFrom(this.http.put<Ficha>(`${this.api}/api/fichas/${id}`, dados));
  }

  encerrar(id: string): Promise<Ficha> {
    return firstValueFrom(this.http.post<Ficha>(`${this.api}/api/fichas/${id}/encerrar`, {}));
  }
}
