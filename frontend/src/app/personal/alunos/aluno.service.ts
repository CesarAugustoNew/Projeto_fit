import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Aluno, DadosAluno, NovoAluno, StatusAluno } from './aluno.model';

@Injectable({ providedIn: 'root' })
export class AlunoService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/alunos`;

  listar(status?: StatusAluno): Promise<Aluno[]> {
    const params = status ? new HttpParams().set('status', status) : new HttpParams();
    return firstValueFrom(this.http.get<Aluno[]>(this.base, { params }));
  }

  buscar(id: string): Promise<Aluno> {
    return firstValueFrom(this.http.get<Aluno>(`${this.base}/${id}`));
  }

  criar(dados: NovoAluno): Promise<Aluno> {
    return firstValueFrom(this.http.post<Aluno>(this.base, dados));
  }

  atualizar(id: string, dados: DadosAluno): Promise<Aluno> {
    return firstValueFrom(this.http.put<Aluno>(`${this.base}/${id}`, dados));
  }

  alterarStatus(id: string, status: 'ATIVO' | 'INATIVO'): Promise<Aluno> {
    return firstValueFrom(this.http.patch<Aluno>(`${this.base}/${id}/status`, { status }));
  }

  async reenviarConvite(id: string): Promise<void> {
    await firstValueFrom(this.http.post<void>(`${this.base}/${id}/reenviar-convite`, null));
  }
}
