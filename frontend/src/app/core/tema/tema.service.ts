import { DOCUMENT } from '@angular/common';
import { Injectable, computed, inject, signal } from '@angular/core';

/** `sistema` segue o modo do aparelho; `claro`/`escuro` são escolhas do usuário. */
export type PreferenciaTema = 'claro' | 'escuro' | 'sistema';

/** Mesma chave lida pelo script do index.html antes do boot. */
export const CHAVE_TEMA = 'tema';

const ESQUEMA: Record<PreferenciaTema, string> = {
  claro: 'light',
  escuro: 'dark',
  sistema: 'light dark',
};

@Injectable({ providedIn: 'root' })
export class TemaService {
  private readonly documento = inject(DOCUMENT);
  private readonly janela = this.documento.defaultView;
  private readonly midiaEscura = this.janela?.matchMedia?.('(prefers-color-scheme: dark)') ?? null;
  private readonly sistemaEscuro = signal(this.midiaEscura?.matches ?? false);

  readonly preferencia = signal<PreferenciaTema>(this.ler());
  readonly escuro = computed(() => {
    const p = this.preferencia();
    return p === 'sistema' ? this.sistemaEscuro() : p === 'escuro';
  });

  constructor() {
    this.midiaEscura?.addEventListener?.('change', (e) => this.sistemaEscuro.set(e.matches));
    this.aplicar(this.preferencia());
  }

  /** Troca para o modo oposto ao que está na tela e guarda a escolha. */
  alternar(): void {
    this.definir(this.escuro() ? 'claro' : 'escuro');
  }

  definir(preferencia: PreferenciaTema): void {
    this.preferencia.set(preferencia);
    this.aplicar(preferencia);
    try {
      if (preferencia === 'sistema') {
        this.janela?.localStorage.removeItem(CHAVE_TEMA);
      } else {
        this.janela?.localStorage.setItem(CHAVE_TEMA, preferencia);
      }
    } catch {
      // sem localStorage (modo privado, bloqueado): vale só nesta visita
    }
  }

  private aplicar(preferencia: PreferenciaTema): void {
    this.documento.documentElement.style.colorScheme = ESQUEMA[preferencia];
  }

  private ler(): PreferenciaTema {
    try {
      const salvo = this.janela?.localStorage.getItem(CHAVE_TEMA);
      return salvo === 'claro' || salvo === 'escuro' ? salvo : 'sistema';
    } catch {
      return 'sistema';
    }
  }
}
