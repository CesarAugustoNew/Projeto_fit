import { normalizarBusca } from './normalizar-busca';

describe('normalizarBusca', () => {
  it('remove acentos, converte para minúsculas e apara espaços', () => {
    expect(normalizarBusca('  Tríceps Testa ')).toBe('triceps testa');
    expect(normalizarBusca('Elevação')).toBe('elevacao');
    expect(normalizarBusca('JOÃO')).toBe('joao');
  });
});
