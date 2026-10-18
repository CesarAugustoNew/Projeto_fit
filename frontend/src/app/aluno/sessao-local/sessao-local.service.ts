import { HttpErrorResponse } from '@angular/common/http';
import { DestroyRef, Injectable, Signal, computed, inject, linkedSignal } from '@angular/core';
import { Divisao, Ficha } from '../../personal/fichas/ficha.model';
import { SerieRequest, SessaoRequest } from '../../shared/sessao/sessao.model';
import { SessaoService } from '../../shared/sessao/sessao.service';
import { UsuarioService } from '../../core/usuario/usuario.service';
import {
  EnvioComErro,
  ExercicioEmAndamento,
  SerieEmAndamento,
  SessaoEmAndamento,
  repeticoesSugeridas,
} from './sessao-local.model';

const ERRO_NENHUMA_SERIE = 'Conclua pelo menos uma série para finalizar o treino.';
const RECUSA_PADRAO = 'O servidor recusou este treino.';

/** Estado local de um usuário; `usuarioId` null = sem usuário, só em memória. */
interface EstadoLocal {
  usuarioId: string | null;
  emAndamento: SessaoEmAndamento | null;
  pendentes: SessaoRequest[];
  comErro: EnvioComErro[];
}

type Chave = 'sessao-em-andamento' | 'sessoes-pendentes' | 'sessoes-com-erro';

/**
 * Treino em andamento e fila de envio de sessões, persistidos no `localStorage`
 * (chaves por usuário) para não perder dados quando a internet cai.
 */
@Injectable({ providedIn: 'root' })
export class SessaoLocalService {
  private readonly sessaoService = inject(SessaoService);
  private readonly usuarioService = inject(UsuarioService);

  /** Recarregado do storage sempre que o usuário muda. */
  private readonly estado = linkedSignal<string | null, EstadoLocal>({
    source: () => this.usuarioService.usuario()?.id ?? null,
    computation: (usuarioId) => carregar(usuarioId),
  });

  readonly emAndamento: Signal<SessaoEmAndamento | null> = computed(() => this.estado().emAndamento);
  readonly pendentes: Signal<number> = computed(() => this.estado().pendentes.length);
  readonly comErro: Signal<EnvioComErro[]> = computed(() => this.estado().comErro);

  private envio: Promise<void> | null = null;

  constructor() {
    const aoFicarOnline = () => void this.enviarPendentes();
    window.addEventListener('online', aoFicarOnline);
    inject(DestroyRef).onDestroy(() => window.removeEventListener('online', aoFicarOnline));
  }

  iniciar(ficha: Ficha, divisao: Divisao): SessaoEmAndamento {
    const sessao: SessaoEmAndamento = {
      clientId: crypto.randomUUID(),
      fichaId: ficha.id,
      divisaoId: divisao.id,
      divisaoNome: divisao.nome,
      iniciadaEm: new Date().toISOString(),
      observacao: '',
      exercicios: divisao.itens.map(
        (item): ExercicioEmAndamento => ({
          itemDivisaoId: item.id,
          exercicioId: item.exercicioId,
          exercicioNome: item.exercicioNome,
          prescricao: {
            series: item.series,
            repeticoes: item.repeticoes,
            cargaKg: item.cargaKg,
            descansoSeg: item.descansoSeg,
            observacao: item.observacao,
          },
          series: Array.from({ length: item.series }, (_, i) => ({
            numero: i + 1,
            cargaKg: item.cargaKg,
            repeticoes: repeticoesSugeridas(item.repeticoes),
            concluida: false,
          })),
        }),
      ),
    };
    this.gravarEmAndamento(sessao);
    return sessao;
  }

  atualizarSerie(exercicioIndex: number, serieIndex: number, mudanca: Partial<SerieEmAndamento>): void {
    this.alterarExercicio(exercicioIndex, (exercicio) => ({
      ...exercicio,
      series: exercicio.series.map((serie, i) => (i === serieIndex ? { ...serie, ...mudanca } : serie)),
    }));
  }

  /** Nova série copiando carga e repetições da última. */
  adicionarSerie(exercicioIndex: number): void {
    this.alterarExercicio(exercicioIndex, (exercicio) => {
      const ultima = exercicio.series.at(-1);
      const nova: SerieEmAndamento = {
        numero: exercicio.series.length + 1,
        cargaKg: ultima?.cargaKg ?? exercicio.prescricao.cargaKg,
        repeticoes: ultima?.repeticoes ?? repeticoesSugeridas(exercicio.prescricao.repeticoes),
        concluida: false,
      };
      return { ...exercicio, series: [...exercicio.series, nova] };
    });
  }

  atualizarObservacao(texto: string): void {
    const atual = this.estado().emAndamento;
    if (atual) this.gravarEmAndamento({ ...atual, observacao: texto });
  }

  descartar(): void {
    this.gravarEmAndamento(null);
  }

  async finalizar(): Promise<'enviado' | 'pendente'> {
    const atual = this.estado().emAndamento;
    const series = atual ? seriesConcluidas(atual) : [];
    if (!atual || series.length === 0) throw new Error(ERRO_NENHUMA_SERIE);

    const req: SessaoRequest = {
      clientId: atual.clientId,
      divisaoId: atual.divisaoId,
      divisaoNome: atual.divisaoNome,
      iniciadaEm: atual.iniciadaEm,
      finalizadaEm: new Date().toISOString(),
      observacao: atual.observacao.trim() || null,
      series,
    };
    // Enfileira antes de limpar o em andamento: nunca há um instante sem o treino no storage.
    this.gravarPendentes([...this.estado().pendentes, req]);
    this.gravarEmAndamento(null);

    await this.enviarPendentes();
    return this.estado().pendentes.length === 0 ? 'enviado' : 'pendente';
  }

  /** Envia a fila em ordem, um por vez; chamadas durante um envio recebem a mesma Promise. */
  enviarPendentes(): Promise<void> {
    this.envio ??= this.executarEnvio();
    return this.envio;
  }

  descartarComErro(index: number): void {
    this.gravarComErro(this.estado().comErro.filter((_, i) => i !== index));
  }

  private async executarEnvio(): Promise<void> {
    // Garante que `this.envio` já foi atribuído antes de o `finally` poder limpá-lo.
    await Promise.resolve();
    const usuarioId = this.estado().usuarioId;
    try {
      while (this.estado().usuarioId === usuarioId) {
        const proxima = this.estado().pendentes[0];
        if (!proxima) return;
        try {
          await this.sessaoService.registrar(proxima);
        } catch (erro) {
          if (this.estado().usuarioId !== usuarioId) return;
          if (deveTentarDeNovo(erro)) return;
          this.gravarComErro([...this.estado().comErro, { sessao: proxima, mensagem: mensagemDeRecusa(erro) }]);
        }
        if (this.estado().usuarioId !== usuarioId) return;
        this.gravarPendentes(this.estado().pendentes.filter((s) => s.clientId !== proxima.clientId));
      }
    } finally {
      // Síncrono após a última leitura da fila: quem enfileirar depois inicia um novo envio.
      this.envio = null;
    }
  }

  private alterarExercicio(index: number, alterar: (e: ExercicioEmAndamento) => ExercicioEmAndamento): void {
    const atual = this.estado().emAndamento;
    if (!atual || !atual.exercicios[index]) return;
    this.gravarEmAndamento({
      ...atual,
      exercicios: atual.exercicios.map((e, i) => (i === index ? alterar(e) : e)),
    });
  }

  private gravarEmAndamento(emAndamento: SessaoEmAndamento | null): void {
    this.estado.update((e) => ({ ...e, emAndamento }));
    this.persistir('sessao-em-andamento', emAndamento);
  }

  private gravarPendentes(pendentes: SessaoRequest[]): void {
    this.estado.update((e) => ({ ...e, pendentes }));
    this.persistir('sessoes-pendentes', pendentes);
  }

  private gravarComErro(comErro: EnvioComErro[]): void {
    this.estado.update((e) => ({ ...e, comErro }));
    this.persistir('sessoes-com-erro', comErro);
  }

  private persistir(chave: Chave, valor: unknown): void {
    const usuarioId = this.estado().usuarioId;
    if (!usuarioId) return;
    try {
      if (valor === null) localStorage.removeItem(`${chave}:${usuarioId}`);
      else localStorage.setItem(`${chave}:${usuarioId}`, JSON.stringify(valor));
    } catch {
      // Storage indisponível ou cheio: o estado segue em memória.
    }
  }
}

function carregar(usuarioId: string | null): EstadoLocal {
  if (!usuarioId) return { usuarioId, emAndamento: null, pendentes: [], comErro: [] };
  return {
    usuarioId,
    emAndamento: ler(`sessao-em-andamento:${usuarioId}`, ehSessaoEmAndamento, null),
    pendentes: ler(`sessoes-pendentes:${usuarioId}`, Array.isArray, []),
    comErro: ler(`sessoes-com-erro:${usuarioId}`, Array.isArray, []),
  };
}

/** Lê e valida uma chave; JSON inválido ou formato inesperado → padrão (e apaga a chave). */
function ler<T>(chave: string, valido: (v: unknown) => boolean, padrao: T): T {
  let texto: string | null;
  try {
    texto = localStorage.getItem(chave);
  } catch {
    return padrao;
  }
  if (texto === null) return padrao;
  try {
    const valor: unknown = JSON.parse(texto);
    if (valido(valor)) return valor as T;
  } catch {
    // cai na limpeza abaixo
  }
  try {
    localStorage.removeItem(chave);
  } catch {
    // nada a fazer
  }
  return padrao;
}

function ehSessaoEmAndamento(valor: unknown): boolean {
  return (
    typeof valor === 'object' &&
    valor !== null &&
    typeof (valor as SessaoEmAndamento).clientId === 'string' &&
    Array.isArray((valor as SessaoEmAndamento).exercicios)
  );
}

function seriesConcluidas(sessao: SessaoEmAndamento): SerieRequest[] {
  return sessao.exercicios.flatMap((exercicio) =>
    exercicio.series
      .filter((serie) => serie.concluida)
      .map(
        (serie): SerieRequest => ({
          itemDivisaoId: exercicio.itemDivisaoId,
          exercicioId: exercicio.exercicioId,
          numeroSerie: serie.numero,
          repeticoes: serie.repeticoes ?? 0,
          cargaKg: serie.cargaKg,
        }),
      ),
  );
}

/** Rede (0), 5xx, 401 ou erro desconhecido → manter na fila e tentar depois. */
function deveTentarDeNovo(erro: unknown): boolean {
  if (!(erro instanceof HttpErrorResponse)) return true;
  return erro.status === 0 || erro.status === 401 || erro.status >= 500 || erro.status < 400;
}

function mensagemDeRecusa(erro: unknown): string {
  const detail = erro instanceof HttpErrorResponse ? (erro.error as { detail?: unknown } | null)?.detail : null;
  return typeof detail === 'string' && detail.trim() ? detail : RECUSA_PADRAO;
}
