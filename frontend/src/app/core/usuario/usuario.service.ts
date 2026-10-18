import { HttpClient } from '@angular/common/http';
import { Injectable, Signal, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Usuario } from './usuario.model';

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private readonly http = inject(HttpClient);
  private readonly _usuario = signal<Usuario | null>(null);

  readonly usuario: Signal<Usuario | null> = this._usuario.asReadonly();

  async carregar(): Promise<Usuario> {
    const usuario = await firstValueFrom(this.http.get<Usuario>(`${environment.apiUrl}/api/me`));
    this._usuario.set(usuario);
    return usuario;
  }

  limpar(): void {
    this._usuario.set(null);
  }
}
