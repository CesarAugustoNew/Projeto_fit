import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { rotaInicial } from '../../core/usuario/usuario.model';
import { UsuarioService } from '../../core/usuario/usuario.service';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly usuarios = inject(UsuarioService);
  private readonly router = inject(Router);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', Validators.required],
  });
  protected readonly erro = signal<string | null>(null);
  protected readonly enviando = signal(false);

  async ngOnInit(): Promise<void> {
    try {
      if (await this.auth.estaLogado()) {
        await this.irParaInicio();
      }
    } catch {
      // sessao invalida ou /me indisponivel: permanece na tela de login
    }
  }

  protected async entrar(): Promise<void> {
    if (this.form.invalid || this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.erro.set(null);
    const { email, senha } = this.form.getRawValue();
    try {
      try {
        await this.auth.login(email, senha);
      } catch {
        this.erro.set('E-mail ou senha inválidos.');
        return;
      }
      try {
        await this.irParaInicio();
      } catch (e) {
        // autenticou no Supabase, mas /me falhou: nao deixa a sessao aberta
        await this.auth.logout();
        this.erro.set(
          e instanceof HttpErrorResponse && e.status === 403
            ? 'Sua conta não tem acesso ao app.'
            : 'Não foi possível entrar. Tente novamente.',
        );
      }
    } finally {
      this.enviando.set(false);
    }
  }

  private async irParaInicio(): Promise<void> {
    const usuario = await this.usuarios.carregar();
    await this.router.navigateByUrl(rotaInicial(usuario.papel));
  }
}
