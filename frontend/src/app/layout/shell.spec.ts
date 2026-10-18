import { BreakpointObserver } from '@angular/cdk/layout';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../core/auth/auth.service';
import { UsuarioService } from '../core/usuario/usuario.service';
import { ItemNav } from './item-nav';
import { Shell } from './shell';

describe('Shell', () => {
  const auth = { logout: vi.fn() };
  const usuarios = { limpar: vi.fn() };
  const itens: ItemNav[] = [{ rota: '/personal/dashboard', rotulo: 'Dashboard', icone: 'dashboard' }];

  function criar(celular: boolean): ComponentFixture<Shell> {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: UsuarioService, useValue: usuarios },
        { provide: BreakpointObserver, useValue: { observe: () => of({ matches: celular, breakpoints: {} }) } },
      ],
    });
    const fixture = TestBed.createComponent(Shell);
    fixture.componentRef.setInput('itens', itens);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    auth.logout.mockReset();
    auth.logout.mockResolvedValue(undefined);
    usuarios.limpar.mockReset();
  });

  it('no celular mostra a barra inferior com os itens', () => {
    const el = criar(true).nativeElement as HTMLElement;
    const links = el.querySelectorAll('nav.nav-inferior a');
    expect(links.length).toBe(1);
    expect(links[0].textContent).toContain('Dashboard');
    expect(el.querySelector('mat-sidenav')).toBeNull();
  });

  it('no desktop mostra o menu lateral', () => {
    const el = criar(false).nativeElement as HTMLElement;
    expect(el.querySelector('mat-sidenav')).not.toBeNull();
    expect(el.querySelector('nav.nav-inferior')).toBeNull();
    expect(el.querySelector('mat-sidenav a')?.textContent).toContain('Dashboard');
  });

  it('Sair desloga, limpa e vai ao login', async () => {
    const fixture = criar(true);
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.sair')!.click();
    await fixture.whenStable();
    expect(auth.logout).toHaveBeenCalled();
    expect(usuarios.limpar).toHaveBeenCalled();
    expect(navegar).toHaveBeenCalledWith('/login');
  });
});
