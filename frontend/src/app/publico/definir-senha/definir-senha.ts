import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { rotaInicial } from '../../core/usuario/usuario.model';
import { UsuarioService } from '../../core/usuario/usuario.service';
import { TelaAcesso } from '../tela-acesso/tela-acesso';

@Component({
  selector: 'app-definir-senha',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    TelaAcesso,
  ],
  templateUrl: './definir-senha.html',
})
export class DefinirSenha {
  private readonly auth = inject(AuthService);
  private readonly usuarios = inject(UsuarioService);
  private readonly router = inject(Router);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    senha: ['', [Validators.required, Validators.minLength(8)]],
    confirmar: ['', Validators.required],
  });
  protected readonly erro = signal<string | null>(null);
  protected readonly enviando = signal(false);

  protected async salvar(): Promise<void> {
    if (this.form.invalid || this.enviando()) {
      return;
    }
    const { senha, confirmar } = this.form.getRawValue();
    if (senha !== confirmar) {
      this.erro.set('As senhas não conferem.');
      return;
    }
    this.enviando.set(true);
    this.erro.set(null);
    try {
      await this.auth.definirSenha(senha);
      const usuario = await this.usuarios.carregar();
      await this.router.navigateByUrl(rotaInicial(usuario.papel));
    } catch {
      this.erro.set('Não foi possível salvar a senha. O link pode ter expirado.');
    } finally {
      this.enviando.set(false);
    }
  }
}
