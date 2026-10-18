import { FormBuilder, FormControl } from '@angular/forms';
import { criarDivisaoForm, criarItemForm, naoVazio } from './form-ficha.model';

describe('form-ficha.model', () => {
  const fb = new FormBuilder().nonNullable;
  const item = () => criarItemForm(fb, { exercicioId: 'e1', exercicioNome: 'Supino', grupoMuscular: 'PEITO' });

  it('séries só aceita inteiros de 1 a 20', () => {
    const c = item().controls.series;
    c.setValue(2.5);
    expect(c.valid).toBe(false);
    c.setValue(20);
    expect(c.valid).toBe(true);
    c.setValue(21);
    expect(c.valid).toBe(false);
  });

  it('descanso só aceita inteiros de 0 a 3600 (ou vazio)', () => {
    const c = item().controls.descansoSeg;
    c.setValue(30.5);
    expect(c.valid).toBe(false);
    c.setValue(3600);
    expect(c.valid).toBe(true);
    c.setValue(null);
    expect(c.valid).toBe(true);
  });

  it('carga aceita de 0 a 9999,99 com até 2 casas decimais (ou vazio)', () => {
    const c = item().controls.cargaKg;
    c.setValue(9999.99);
    expect(c.valid).toBe(true);
    c.setValue(22.5);
    expect(c.valid).toBe(true);
    c.setValue(10000);
    expect(c.valid).toBe(false);
    c.setValue(1.234);
    expect(c.valid).toBe(false);
    c.setValue(-1);
    expect(c.valid).toBe(false);
    c.setValue(null);
    expect(c.valid).toBe(true);
  });

  it('nome da divisão só com espaços é inválido', () => {
    const d = criarDivisaoForm(fb, { nome: '   ' });
    expect(d.controls.nome.hasError('required')).toBe(true);
    d.controls.nome.setValue(' Treino A ');
    expect(d.controls.nome.valid).toBe(true);
  });

  it('naoVazio ignora espaços nas pontas', () => {
    expect(naoVazio(new FormControl('  ', { nonNullable: true }))).toEqual({ required: true });
    expect(naoVazio(new FormControl(' a ', { nonNullable: true }))).toBeNull();
  });
});
