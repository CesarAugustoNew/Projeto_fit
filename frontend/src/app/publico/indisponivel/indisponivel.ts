import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, Router } from '@angular/router';
import { TelaAcesso } from '../tela-acesso/tela-acesso';

@Component({
  selector: 'app-indisponivel',
  imports: [MatButtonModule, TelaAcesso],
  templateUrl: './indisponivel.html',
})
export class Indisponivel {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected tentarDeNovo(): void {
    const voltar = this.route.snapshot.queryParamMap.get('voltar');
    const seguro = voltar && voltar.startsWith('/') && !voltar.startsWith('//') ? voltar : '/login';
    void this.router.navigateByUrl(seguro);
  }
}
