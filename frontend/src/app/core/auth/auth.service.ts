import { Injectable, inject } from '@angular/core';
import { SUPABASE_CLIENT } from './supabase';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly supabase = inject(SUPABASE_CLIENT);

  async login(email: string, senha: string): Promise<void> {
    const { error } = await this.supabase.auth.signInWithPassword({ email, password: senha });
    if (error) {
      throw new Error(error.message);
    }
  }

  async logout(): Promise<void> {
    await this.supabase.auth.signOut();
  }

  async token(): Promise<string | null> {
    const { data } = await this.supabase.auth.getSession();
    return data.session?.access_token ?? null;
  }

  async estaLogado(): Promise<boolean> {
    return (await this.token()) !== null;
  }

  async definirSenha(senha: string): Promise<void> {
    const { error } = await this.supabase.auth.updateUser({ password: senha });
    if (error) {
      throw new Error(error.message);
    }
  }

  async enviarRedefinicao(email: string): Promise<void> {
    const { error } = await this.supabase.auth.resetPasswordForEmail(email, {
      redirectTo: window.location.origin + '/definir-senha',
    });
    if (error) {
      throw new Error(error.message);
    }
  }
}
