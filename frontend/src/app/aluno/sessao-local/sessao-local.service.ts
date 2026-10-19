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
const ERRO_SALVAR = 'Não foi possível salvar o treino no aparelho. Libere espaço e tente de novo.';
const RECUSA_PADRAO = 'O servidor recusou este treino.';
/** Limite de `numeroSerie` aceito pelo back. */
const MAX_SERIES = 50;
/** Limite de séries por sessão aceito pelo back. */
const MAX_SERIES_SESSAO = 400;

/** Estado local de um usuário; `usuarioId` null = sem usuário, só em memória. */
interface EstadoLocal {
  usuarioId: string | null;
  emAndamento: SessaoEmAndamento | null;
  pendentes: SessaoRequest[];
  comErro: EnvioComErro[];
}

type Campo = 'emAndamento' | 'pendentes' | 'comErro';

const PREFIXO: Record<Campo, string> = {
  emAndamento: 'sessao-em-andamento',
  pendentes: 'sessoes-pendentes',
  comErro: 'sessoes-com-erro',
};

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

  readonly emAndamento: Signal<SessaoEmAndamento | null> = computed(
    () => this.estado().emAndamento,
  );
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
      clientId: gerarUuid(),
      fichaId: ficha.id,
      divisaoId: divisao.id,
      divisaoNome: divisao.nome,
      iniciadaEm: new Date().toISOString(),
      observacao: '',
      exercicios: divisao.itens.map((item): ExercicioEmAndamento => ({
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
      })),
    };
    this.gravar('emAndamento', sessao);
    return sessao;
  }

  atualizarSerie(
    exercicioIndex: number,
    serieIndex: number,
    mudanca: Partial<SerieEmAndamento>,
  ): void {
    this.alterarExercicio(exercicioIndex, (exercicio) => ({
      ...exercicio,
      series: exercicio.series.map((serie, i) =>
        i === serieIndex ? { ...serie, ...mudanca } : serie,
      ),
    }));
  }

  /**
   * Nova série copiando carga e repetições da última (até {@link MAX_SERIES} no exercício e
   * {@link MAX_SERIES_SESSAO} na sessão).
   */
  adicionarSerie(exercicioIndex: number): void {
    const exercicios = this.estado().emAndamento?.exercicios ?? [];
    if ((exercicios[exercicioIndex]?.series.length ?? 0) >= MAX_SERIES) return;
    if (exercicios.reduce((total, e) => total + e.series.length, 0) >= MAX_SERIES_SESSAO) return;
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
    if (atual) this.gravar('emAndamento', { ...atual, observacao: texto });
  }

  descartar(): void {
    this.gravar('emAndamento', null);
  }

  async finalizar(): Promise<'enviado' | 'pendente' | 'recusado'> {
    const { usuarioId, emAndamento: atual } = this.estado();
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
    // Só limpa o em andamento depois que a fila foi gravada: o treino nunca fica fora do storage.
    if (!this.gravarSeguro('pendentes', [...this.estado().pendentes, req]))
      throw new Error(ERRO_SALVAR);
    this.gravar('emAndamento', null);

    await this.enviarPendentes();
    // 401/403 limpam o usuário durante o envio: o treino segue na fila dele, sem ter sido enviado.
    if (this.estado().usuarioId !== usuarioId) return 'pendente';
    const { pendentes, comErro } = this.estado();
    if (pendentes.some((s) => s.clientId === req.clientId)) return 'pendente';
    if (comErro.some((e) => e.sessao.clientId === req.clientId)) return 'recusado';
    return 'enviado';
  }

  /** Envia a fila em ordem, um por vez; chamadas durante um envio recebem a mesma Promise. */
  enviarPendentes(): Promise<void> {
    this.envio ??= this.executarEnvio();
    return this.envio;
  }

  /** Devolve um treino recusado para o fim da fila e tenta enviar de novo (mesmo `clientId`). */
  async tentarDeNovoComErro(index: number): Promise<void> {
    const item = this.estado().comErro[index];
    if (!item) return;
    // Primeiro grava na fila: se falhar, o treino continua na lista de recusados (nada se perde).
    if (!this.gravarSeguro('pendentes', [...this.estado().pendentes, item.sessao])) return;
    this.gravar(
      'comErro',
      this.estado().comErro.filter((_, i) => i !== index),
    );
    await this.enviarPendentes();
  }

  descartarComErro(index: number): void {
    this.gravar(
      'comErro',
      this.estado().comErro.filter((_, i) => i !== index),
    );
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
          if (this.estado().usuarioId !== usuarioId || deveTentarDeNovo(erro)) return;
          const recusa: EnvioComErro = { sessao: proxima, mensagem: mensagemDeRecusa(erro) };
          // Sem conseguir gravar a recusa, o treino fica na fila (nada se perde) e o envio para.
          if (!this.gravarSeguro('comErro', [...this.estado().comErro, recusa])) return;
        }
        if (this.estado().usuarioId !== usuarioId) return;
        this.gravar(
          'pendentes',
          this.estado().pendentes.filter((s) => s.clientId !== proxima.clientId),
        );
      }
    } finally {
      // Síncrono após a última leitura da fila: quem enfileirar depois inicia um novo envio.
      this.envio = null;
    }
  }

  private alterarExercicio(
    index: number,
    alterar: (e: ExercicioEmAndamento) => ExercicioEmAndamento,
  ): void {
    const atual = this.estado().emAndamento;
    if (!atual || !atual.exercicios[index]) return;
    this.gravar('emAndamento', {
      ...atual,
      exercicios: atual.exercicios.map((e, i) => (i === index ? alterar(e) : e)),
    });
  }

  /** Atualiza o estado e tenta gravar; se o storage falhar, o estado segue em memória. */
  private gravar<C extends Campo>(campo: C, valor: EstadoLocal[C]): void {
    this.estado.update((e) => ({ ...e, [campo]: valor }));
    this.persistir(campo, valor);
  }

  /** Grava primeiro no storage e só então no estado; se o storage falhar, nada muda. */
  private gravarSeguro<C extends Campo>(campo: C, valor: EstadoLocal[C]): boolean {
    if (!this.persistir(campo, valor)) return false;
    this.estado.update((e) => ({ ...e, [campo]: valor }));
    return true;
  }

  /** `true` se gravou (ou se não há usuário, caso em que o estado é só em memória). */
  private persistir(campo: Campo, valor: unknown): boolean {
    const usuarioId = this.estado().usuarioId;
    if (!usuarioId) return true;
    const chave = `${PREFIXO[campo]}:${usuarioId}`;
    try {
      if (valor === null) localStorage.removeItem(chave);
      else localStorage.setItem(chave, JSON.stringify(valor));
      return true;
    } catch {
      return false;
    }
  }
}

function carregar(usuarioId: string | null): EstadoLocal {
  if (!usuarioId) return { usuarioId, emAndamento: null, pendentes: [], comErro: [] };
  return {
    usuarioId,
    emAndamento: ler(`${PREFIXO.emAndamento}:${usuarioId}`, null, (v) =>
      ehSessaoEmAndamento(v) ? v : undefined,
    ),
    pendentes: ler(`${PREFIXO.pendentes}:${usuarioId}`, [], (v) =>
      Array.isArray(v) ? v.filter(ehSessaoRequest) : undefined,
    ),
    comErro: ler(`${PREFIXO.comErro}:${usuarioId}`, [], (v) =>
      Array.isArray(v) ? v.filter(ehEnvioComErro) : undefined,
    ),
  };
}

/**
 * Lê uma chave e converte o valor; `undefined` na conversão = formato inválido.
 * JSON inválido ou formato inesperado → padrão (e apaga a chave); storage indisponível → padrão.
 */
function ler<T>(chave: string, padrao: T, converter: (valor: unknown) => T | undefined): T {
  let texto: string | null;
  try {
    texto = localStorage.getItem(chave);
  } catch {
    return padrao;
  }
  if (texto === null) return padrao;
  try {
    const convertido = converter(JSON.parse(texto));
    if (convertido !== undefined) return convertido;
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

function ehObjeto(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === 'object' && valor !== null && !Array.isArray(valor);
}

function ehSessaoEmAndamento(valor: unknown): valor is SessaoEmAndamento {
  return (
    ehObjeto(valor) &&
    typeof valor['clientId'] === 'string' &&
    Array.isArray(valor['exercicios']) &&
    valor['exercicios'].every((e) => ehObjeto(e) && Array.isArray(e['series']))
  );
}

function ehSessaoRequest(valor: unknown): valor is SessaoRequest {
  return ehObjeto(valor) && typeof valor['clientId'] === 'string';
}

function ehEnvioComErro(valor: unknown): valor is EnvioComErro {
  return (
    ehObjeto(valor) && ehSessaoRequest(valor['sessao']) && typeof valor['mensagem'] === 'string'
  );
}

/** `crypto.randomUUID` só existe em contexto seguro (HTTPS/localhost); em HTTP na rede local, gera um UUID v4. */
function gerarUuid(): string {
  if (typeof crypto.randomUUID === 'function') return crypto.randomUUID();
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6] & 0x0f) | 0x40; // versão 4
  bytes[8] = (bytes[8] & 0x3f) | 0x80; // variante RFC 4122
  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

function seriesConcluidas(sessao: SessaoEmAndamento): SerieRequest[] {
  return sessao.exercicios.flatMap((exercicio) =>
    exercicio.series
      .filter((serie) => serie.concluida)
      .map((serie): SerieRequest => ({
        itemDivisaoId: exercicio.itemDivisaoId,
        exercicioId: exercicio.exercicioId,
        numeroSerie: serie.numero,
        repeticoes: serie.repeticoes ?? 0,
        cargaKg: serie.cargaKg,
      })),
  );
}

/** Rede (0), 401, 408, 429, 5xx ou erro desconhecido → manter na fila e tentar depois. */
function deveTentarDeNovo(erro: unknown): boolean {
  if (!(erro instanceof HttpErrorResponse)) return true;
  const { status } = erro;
  return status < 400 || status >= 500 || status === 401 || status === 408 || status === 429;
}

function mensagemDeRecusa(erro: unknown): string {
  const detail =
    erro instanceof HttpErrorResponse ? (erro.error as { detail?: unknown } | null)?.detail : null;
  return typeof detail === 'string' && detail.trim() ? detail : RECUSA_PADRAO;
}
