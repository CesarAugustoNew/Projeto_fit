import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AlvoDesempenho,
  DashboardAluno,
  ExercicioExecutado,
  PontoEvolucao,
  SemanaAderencia,
} from './desempenho.model';

@Injectable({ providedIn: 'root' })
export class DesempenhoService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  exercicios(alvo: AlvoDesempenho): Promise<ExercicioExecutado[]> {
    return firstValueFrom(this.http.get<ExercicioExecutado[]>(`${this.base(alvo)}/exercicios`));
  }

  evolucao(alvo: AlvoDesempenho, exercicioId: string): Promise<PontoEvolucao[]> {
    return firstValueFrom(
      this.http.get<PontoEvolucao[]>(`${this.base(alvo)}/evolucao`, { params: { exercicioId } }),
    );
  }

  aderencia(alvo: AlvoDesempenho, semanas = 8): Promise<SemanaAderencia[]> {
    return firstValueFrom(
      this.http.get<SemanaAderencia[]>(`${this.base(alvo)}/aderencia`, {
        params: { semanas: String(semanas) },
      }),
    );
  }

  dashboard(): Promise<DashboardAluno[]> {
    return firstValueFrom(this.http.get<DashboardAluno[]>(`${this.api}/api/dashboard`));
  }

  private base(alvo: AlvoDesempenho): string {
    return alvo.tipo === 'aluno-logado'
      ? `${this.api}/api/me/desempenho`
      : `${this.api}/api/alunos/${alvo.alunoId}/desempenho`;
  }
}
