import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TemaService } from './core/tema/tema.service';

@Component({
  imports: [RouterOutlet],
  selector: 'app-root',
  templateUrl: './app.html',
})
export class App {
  // Cria o serviço no boot: aplica o tema salvo e passa a seguir o modo do sistema.
  private readonly tema = inject(TemaService);
}
