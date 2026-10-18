import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AtualizarExercicio, Exercicio, GrupoMuscular, NovoExercicio } from './exercicio.model';

@Injectable({ providedIn: 'root' })
export class ExercicioService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/exercicios`;

  listar(filtro: { ativo?: boolean; grupo?: GrupoMuscular } = {}): Promise<Exercicio[]> {
    let params = new HttpParams();
    if (filtro.ativo !== undefined) params = params.set('ativo', String(filtro.ativo));
    if (filtro.grupo) params = params.set('grupo', filtro.grupo);
    return firstValueFrom(this.http.get<Exercicio[]>(this.base, { params }));
  }

  buscar(id: string): Promise<Exercicio> {
    return firstValueFrom(this.http.get<Exercicio>(`${this.base}/${id}`));
  }

  criar(dados: NovoExercicio): Promise<Exercicio> {
    return firstValueFrom(this.http.post<Exercicio>(this.base, dados));
  }

  atualizar(id: string, dados: AtualizarExercicio): Promise<Exercicio> {
    return firstValueFrom(this.http.put<Exercicio>(`${this.base}/${id}`, dados));
  }
}
