import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-indisponivel',
  imports: [MatButtonModule, MatCardModule],
  templateUrl: './indisponivel.html',
  styleUrl: './indisponivel.scss',
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
