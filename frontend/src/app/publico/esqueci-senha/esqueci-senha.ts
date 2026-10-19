import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { TelaAcesso } from '../tela-acesso/tela-acesso';

@Component({
  selector: 'app-esqueci-senha',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    TelaAcesso,
  ],
  templateUrl: './esqueci-senha.html',
})
export class EsqueciSenha {
  private readonly auth = inject(AuthService);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });
  protected readonly enviado = signal(false);
  protected readonly erro = signal(false);
  protected readonly enviando = signal(false);

  protected async enviar(): Promise<void> {
    if (this.form.invalid || this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.erro.set(false);
    this.enviado.set(false);
    try {
      await this.auth.enviarRedefinicao(this.form.getRawValue().email);
      this.enviado.set(true);
    } catch {
      this.erro.set(true);
    } finally {
      this.enviando.set(false);
    }
  }
}
