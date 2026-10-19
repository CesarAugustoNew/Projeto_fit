# Etapa 6 — Desempenho e dashboard: Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** O personal vê no dashboard quem está treinando, quem sumiu e quem está com ficha vencida, e acompanha a evolução de carga e a aderência semanal de cada aluno; o aluno vê a própria evolução.

**Architecture:** Novo módulo somente leitura `desempenho` no back (consultas agregadas sobre `sessao`/`serie_executada`/`ficha`, cálculo de semanas no fuso `America/Sao_Paulo` com o `Clock`), com rotas do personal (`/api/alunos/{id}/desempenho/*`, `/api/dashboard`) e do aluno (`/api/me/desempenho/*`). No front, gráficos com Chart.js via `ng2-charts` registrados só nos componentes lazy (para não pesar o carregamento inicial), um painel de desempenho compartilhado (detalhe do aluno e "Evolução" do aluno) e o dashboard do personal.

**Tech Stack:** Java 21, Spring Boot 4.1 (Data JPA com `@Query` e projeções `record`), PostgreSQL 17, Testcontainers · Angular 22, Angular Material, `chart.js` 4.5 + `ng2-charts` 11, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-18-app-personal-trainer-design.md` (§6 "Métricas de desempenho", §7 endpoints de desempenho e `/dashboard`, §8 Dashboard e "Minha evolução"). Pendências: `docs/superpowers/plans/2026-10-18-etapa5-pendencias.md` (seção "Incluir no plano da Etapa 6").

## Global Constraints
- Tudo das *Global Constraints* das Etapas 1–5 continua valendo (pacote `com.projetofit`, DTOs `record`, Problem Details pt-BR, escopo por usuário — aluno de outro personal → 404 `Aluno não encontrado.`, pt-BR, nunca commitar `frontend/src/environments/environment.development.ts`).
- Fuso de referência `America/Sao_Paulo` para "hoje", datas e semanas (bean `Clock` de `comum/RelogioConfig`). Semana = **segunda a domingo**.
- Rotas: personal → `GET /api/alunos/{alunoId}/desempenho/evolucao`, `/aderencia`, `/exercicios` e `GET /api/dashboard`; aluno → `GET /api/me/desempenho/evolucao`, `/aderencia`, `/exercicios` (o aluno só vê os próprios dados; a spec permite ao aluno o próprio id, e `/api/me` evita expor o id do aluno no front).
- Definições (spec §6), exatas:
  - **Evolução**: um ponto por sessão do aluno que tenha o exercício, no período (padrão: últimos 6 meses até hoje): `data` (data de `finalizada_em` no fuso), `cargaMaxima` (maior `carga_kg`; `null` se todas as séries sem carga), `volume` (Σ repetições × carga, carga nula conta 0); ordem cronológica.
  - **Aderência semanal**: últimas N semanas (padrão 8, de 1 a 26), incluindo a atual; `realizados` = sessões com `finalizada_em` na semana; `planejado` = `frequencia_semanal` da ficha ativa naquela semana (a ficha cujo período ativo — de `criado_em` até `encerrada_em` ou até agora — cruza a semana; se mais de uma, a mais recente por `criado_em`; nenhuma → 0); `percentual` = `realizados / planejado` limitado a 100, inteiro arredondado; `null` se `planejado = 0`.
  - **Dias sem treinar**: dias corridos entre hoje e a data da última `finalizada_em`; `null` se nunca treinou.
  - **Alerta "sumiu"**: dias sem treinar > 7, ou (nunca treinou e tem ficha ativa criada há mais de 7 dias).
- Chart.js **não** pode entrar no carregamento inicial: registrar com `provideCharts(withDefaultRegisterables())` nos `providers` dos componentes de gráfico (lazy), nunca em `app.config.ts`.
- Testes: back `cd backend && ./mvnw -q test -Dtest=<Classe>` e `./mvnw -q verify`; front `cd frontend && npx ng test --watch=false` e `npx ng build`.

## Review Focus
1. **Treino no domingo à noite** (ex.: 22h de domingo em São Paulo = 01h de segunda em UTC): conta no domingo, não na semana seguinte. → Task 2, `treinoDomingoNoiteContaNaSemanaCerta`.
2. **Sem ficha na semana ou ficha trocada no meio da semana**: `planejado` 0 com `percentual` nulo (sem divisão por zero); troca no meio usa a ficha mais recente. → Task 2, `semanaSemFichaTemPercentualNulo` e `fichaTrocadaNaSemanaUsaAMaisRecente`.
3. **Aluno que nunca treinou**: `diasSemTreinar` nulo, "Nunca treinou" na tela, e só "sumiu" se a ficha ativa tem mais de 7 dias. → Task 3, `nuncaTreinouComFichaRecenteNaoSumiu`; Task 7, `mostraNuncaTreinou`.
4. **Exercício sem carga** (peso corporal, ex.: prancha): `cargaMaxima` nula e `volume` 0, sem quebrar o gráfico. → Task 1, `exercicioSemCargaTemCargaMaximaNula`; Task 5, `pontoSemCargaViraLacuna`.
5. **Gráficos não podem pesar a primeira carga do app no celular**: o chunk inicial não pode crescer com o Chart.js. → Task 4, verificação de build.

---

### Task 1: Evolução por exercício e exercícios executados

**Files:**
- Create: `backend/src/main/java/com/projetofit/desempenho/DesempenhoConsultas.java`, `PontoEvolucaoBruto.java`, `PontoEvolucaoDTO.java`, `ExercicioExecutadoDTO.java`, `DesempenhoService.java`, `DesempenhoController.java`
- Test: `backend/src/test/java/com/projetofit/desempenho/EvolucaoIT.java`

**Interfaces:**
- Consumes: `Sessao`, `SerieExecutada` (Etapa 5), `AlunoRepository.findByIdAndPersonalId`, `AlunoLogado.obter()` (pacote `sessao`), `UsuarioAtual.id()`, `Clock`, `DadosInvalidosException`.
- Produces:
  - `interface DesempenhoConsultas extends org.springframework.data.repository.Repository<SerieExecutada, UUID>` com:
    - `@Query("select new com.projetofit.desempenho.PontoEvolucaoBruto(s.id, s.finalizadaEm, max(se.cargaKg), sum(se.repeticoes * coalesce(se.cargaKg, 0))) from SerieExecutada se join se.sessao s where s.aluno.id = :alunoId and se.exercicio.id = :exercicioId and s.finalizadaEm >= :de and s.finalizadaEm < :ate group by s.id, s.finalizadaEm order by s.finalizadaEm") List<PontoEvolucaoBruto> evolucao(UUID alunoId, UUID exercicioId, Instant de, Instant ate)` (`record PontoEvolucaoBruto(UUID sessaoId, Instant finalizadaEm, BigDecimal cargaMaxima, BigDecimal volume)` no mesmo pacote);
    - `@Query("select distinct new com.projetofit.desempenho.ExercicioExecutadoDTO(e.id, e.nome, e.grupoMuscular) from SerieExecutada se join se.exercicio e where se.sessao.aluno.id = :alunoId order by e.nome") List<ExercicioExecutadoDTO> exerciciosExecutados(UUID alunoId)`.
  - `record PontoEvolucaoDTO(LocalDate data, UUID sessaoId, BigDecimal cargaMaxima, BigDecimal volume)`; `record ExercicioExecutadoDTO(UUID id, String nome, GrupoMuscular grupoMuscular)`.
  - `DesempenhoService` (`@Transactional(readOnly = true)` nos métodos): `List<PontoEvolucaoDTO> evolucao(UUID alunoId, UUID exercicioId, LocalDate de, LocalDate ate)` — `ate` nulo → hoje; `de` nulo → `ate.minusMonths(6)`; `de` depois de `ate` → `DadosInvalidosException("de", "A data inicial deve ser anterior ou igual à final.")`; intervalo convertido para instantes no fuso (`de` 00:00 até `ate + 1 dia` 00:00, exclusivo); `List<ExercicioExecutadoDTO> exercicios(UUID alunoId)`; método auxiliar `UUID alunoDoPersonal(UUID alunoId)` (404 `Aluno não encontrado.`) e `UUID alunoLogado()` reutilizados nas Tasks 2 e 3.
  - `DesempenhoController`: `GET /api/alunos/{alunoId}/desempenho/evolucao?exercicioId=&de=&ate=` e `GET /api/alunos/{alunoId}/desempenho/exercicios` com `@PreAuthorize("hasRole('PERSONAL')")`; `GET /api/me/desempenho/evolucao?exercicioId=&de=&ate=` e `GET /api/me/desempenho/exercicios` com `@PreAuthorize("hasRole('ALUNO')")`. `exercicioId` obrigatório; datas `yyyy-MM-dd`.

- [ ] **Step 1: Testes que falham** (`EvolucaoIT`; personal, aluno com sessões em datas diferentes gravadas pelos repositórios, com séries de dois exercícios; outro aluno de outro personal):
```java
@Test void evolucaoPorSessaoEmOrdemCronologica()   // 3 sessões com Supino: cargas 40/42.5 e 45 → 3 pontos; cargaMaxima 42.5 na 2ª; volume = Σ reps×carga; datas no fuso
@Test void exercicioSemCargaTemCargaMaximaNula()   // prancha sem carga → cargaMaxima null, volume 0
@Test void periodoPadraoSaoSeisMeses()             // sessão de 7 meses atrás fica de fora; de 5 meses entra
@Test void periodoInvalidoRetorna400()             // de=2026-05-01&ate=2026-04-01 → errors.de = "A data inicial deve ser anterior ou igual à final."
@Test void exerciciosExecutadosSemRepeticaoPorNome() // dois exercícios → lista por nome, sem duplicar
@Test void alunoVeAPropriaEvolucaoPeloMe()          // token do aluno → GET /api/me/desempenho/evolucao → 200
@Test void personalNaoVeAlunoDeOutroPersonal()      // → 404 "Aluno não encontrado."
@Test void alunoNaoUsaRotaDoPersonal()              // token do aluno → /api/alunos/{id}/desempenho/evolucao → 403
```
- [ ] **Step 2: Rodar e ver falhar** — `./mvnw -q test -Dtest=EvolucaoIT` → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar** — `./mvnw -q verify` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(backend): evolucao de carga por exercicio e exercicios executados"`

---

### Task 2: Aderência semanal

**Files:**
- Create: `backend/src/main/java/com/projetofit/desempenho/SemanaAderenciaDTO.java`, `CalculoAderencia.java`
- Modify: `DesempenhoService.java`, `DesempenhoController.java`, `backend/src/main/java/com/projetofit/sessao/SessaoRepository.java`
- Test: `backend/src/test/java/com/projetofit/desempenho/CalculoAderenciaTest.java`, `AderenciaIT.java`

**Interfaces:**
- Consumes: Task 1 (`alunoDoPersonal`, `alunoLogado`); `FichaRepository.findByAlunoIdOrderByDataInicioDescCriadoEmDesc`; `Ficha.getCriadoEm()`, `getEncerradaEm()`, `getFrequenciaSemanal()`.
- Produces:
  - `SessaoRepository.findFinalizadasEmEntre(UUID alunoId, Instant de, Instant ate)`: `@Query("select s.finalizadaEm from Sessao s where s.aluno.id = :alunoId and s.finalizadaEm >= :de and s.finalizadaEm < :ate") List<Instant>`.
  - `record SemanaAderenciaDTO(LocalDate inicio, LocalDate fim, int realizados, int planejado, Integer percentual)`.
  - `CalculoAderencia` (classe pura, sem Spring): `static List<SemanaAderenciaDTO> calcular(LocalDate hoje, int semanas, ZoneId zona, List<Instant> finalizacoes, List<PeriodoFicha> fichas)` com `record PeriodoFicha(Instant ativaDesde, Instant ativaAte /* null = ainda ativa */, int frequenciaSemanal)`; semanas em ordem cronológica (a última é a atual); regras exatas das *Global Constraints*.
  - `DesempenhoService.aderencia(UUID alunoId, int semanas): List<SemanaAderenciaDTO>`.
  - Controller: `GET /api/alunos/{alunoId}/desempenho/aderencia?semanas=8` (PERSONAL) e `GET /api/me/desempenho/aderencia?semanas=8` (ALUNO); `@RequestParam(defaultValue = "8") @Min(1) @Max(26) int semanas`.

- [ ] **Step 1: Testes que falham**

`CalculoAderenciaTest` (unitário, `hoje` = quarta 2026-10-14, zona São Paulo):
```java
@Test void oitoSemanasTerminandoNaAtual()              // 8 semanas; última inicio 2026-10-12 (segunda) fim 2026-10-18
@Test void contaRealizadosEPercentualLimitadoA100()    // planejado 3, 4 treinos → percentual 100; 2 treinos → 67
@Test void treinoDomingoNoiteContaNaSemanaCerta()      // 2026-10-11T22:00-03:00 (= 2026-10-12T01:00Z) conta na semana de 05 a 11
@Test void semanaSemFichaTemPercentualNulo()           // planejado 0, percentual null
@Test void fichaTrocadaNaSemanaUsaAMaisRecente()       // ficha A (freq 3) encerrada na terça, ficha B (freq 5) criada na terça → planejado 5
@Test void fichaEncerradaAntesDaSemanaNaoConta()
```
`AderenciaIT`: `semanasPadraoOito` (8 itens), `semanasForaDoLimiteRetorna400` (`semanas=30` → 400), `alunoVeAPropriaAderencia` (`/api/me/desempenho/aderencia`), `personalNaoVeAlunoDeOutroPersonal` (404).
- [ ] **Step 2: Rodar e ver falhar** — `./mvnw -q test -Dtest='CalculoAderenciaTest,AderenciaIT'` → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar** — `./mvnw -q verify` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(backend): aderencia semanal por aluno"`

---

### Task 3: Dashboard do personal

**Files:**
- Create: `backend/src/main/java/com/projetofit/desempenho/DashboardAlunoDTO.java`, `DashboardService.java`
- Modify: `DesempenhoController.java` (ou um `DashboardController.java` no mesmo pacote), `SessaoRepository.java`, `backend/src/main/java/com/projetofit/ficha/FichaRepository.java`
- Test: `backend/src/test/java/com/projetofit/desempenho/DashboardIT.java`

**Interfaces:**
- Consumes: `AlunoRepository.findByPersonalIdAndStatusInOrderByUsuarioNomeAsc`, `Ficha.vencida(LocalDate)`, `Clock`, `UsuarioAtual`.
- Produces:
  - `SessaoRepository`: `@Query("select s.aluno.id, max(s.finalizadaEm) from Sessao s where s.aluno.personalId = :personalId group by s.aluno.id") List<Object[]> ultimaSessaoPorAluno(UUID personalId)` (ou projeção `record`) e `@Query("select s.aluno.id, count(s) from Sessao s where s.aluno.personalId = :personalId and s.finalizadaEm >= :de and s.finalizadaEm < :ate group by s.aluno.id") List<Object[]> sessoesPorAlunoEntre(UUID personalId, Instant de, Instant ate)`.
  - `FichaRepository.findByAlunoPersonalIdAndStatus(UUID personalId, StatusFicha status): List<Ficha>`.
  - `record DashboardAlunoDTO(UUID alunoId, String nome, int realizadosSemana, int planejadoSemana, Integer diasSemTreinar, UUID fichaAtivaId, boolean fichaVencida, boolean sumiu)`.
  - `DashboardService.dashboard(): List<DashboardAlunoDTO>` — só alunos `ATIVO` do personal logado; semana atual (segunda a domingo, fuso); `planejadoSemana` = frequência da ficha ativa (0 sem ficha); regras de "dias sem treinar" e "sumiu" das *Global Constraints*; ordem: `sumiu` primeiro, depois nome. Consultas em lote (sem uma consulta por aluno).
  - `GET /api/dashboard` com `@PreAuthorize("hasRole('PERSONAL')")`.

- [ ] **Step 1: Testes que falham** (`DashboardIT`; datas relativas a `LocalDate.now(clock)`):
```java
@Test void resumoDaSemanaDeCadaAluno()               // aluno com ficha freq 3 e 2 treinos nesta semana → realizados 2, planejado 3, diasSemTreinar 0
@Test void alunoQueSumiuVemPrimeiro()                 // último treino há 10 dias → sumiu true, primeiro da lista
@Test void nuncaTreinouComFichaRecenteNaoSumiu()      // ficha criada hoje, sem treino → diasSemTreinar null, sumiu false
@Test void nuncaTreinouComFichaAntigaSumiu()          // ficha com criado_em de 10 dias atrás (ajustar via JdbcTemplate) → sumiu true
@Test void fichaVencidaSinalizada()                   // dataFim no passado → fichaVencida true
@Test void semFichaTemPlanejadoZero()                 // fichaAtivaId null, planejadoSemana 0
@Test void soAlunosAtivosDoPersonal()                 // convidado, inativo e aluno de outro personal não aparecem
@Test void alunoNaoAcessaDashboard()                  // → 403
```
- [ ] **Step 2: Rodar e ver falhar** — `./mvnw -q test -Dtest=DashboardIT` → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar** — `./mvnw -q verify` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(backend): dashboard do personal com alertas"`

---

### Task 4: Front — Chart.js, modelo e `DesempenhoService`

**Files:**
- Modify: `frontend/package.json` (`chart.js` 4.5.x e `ng2-charts` 11.x)
- Create: `frontend/src/app/shared/desempenho/desempenho.model.ts`, `desempenho.service.ts`, `desempenho.service.spec.ts`

**Interfaces:**
- Produces:
  - `desempenho.model.ts`: `interface PontoEvolucao { data: string; sessaoId: string; cargaMaxima: number | null; volume: number }`; `interface ExercicioExecutado { id: string; nome: string; grupoMuscular: GrupoMuscular }`; `interface SemanaAderencia { inicio: string; fim: string; realizados: number; planejado: number; percentual: number | null }`; `interface DashboardAluno { alunoId: string; nome: string; realizadosSemana: number; planejadoSemana: number; diasSemTreinar: number | null; fichaAtivaId: string | null; fichaVencida: boolean; sumiu: boolean }`; `type AlvoDesempenho = { tipo: 'aluno-logado' } | { tipo: 'aluno'; alunoId: string }`; `function aderenciaMedia(semanas: SemanaAderencia[]): number | null` (média dos percentuais não nulos, arredondada; `null` se nenhum).
  - `DesempenhoService` (`providedIn: 'root'`): `exercicios(alvo: AlvoDesempenho): Promise<ExercicioExecutado[]>`, `evolucao(alvo: AlvoDesempenho, exercicioId: string): Promise<PontoEvolucao[]>`, `aderencia(alvo: AlvoDesempenho, semanas = 8): Promise<SemanaAderencia[]>`, `dashboard(): Promise<DashboardAluno[]>` — `aluno-logado` usa `/api/me/desempenho/...`; `aluno` usa `/api/alunos/{alunoId}/desempenho/...`.

- [ ] **Step 1: Instalar** — `cd frontend && npm i chart.js@^4.5.1 ng2-charts@^11.0.0` (não registrar nada em `app.config.ts`).
- [ ] **Step 2: Testes que falham** (`HttpTestingController` + `http.verify()`): URLs de cada método para os dois alvos; `evolucao` envia `exercicioId`; `aderencia` envia `semanas=8`; `aderenciaMedia([{percentual: 100}, {percentual: null}, {percentual: 50}])` → 75; todos nulos → `null`.
- [ ] **Step 3: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 4: Implementar.**
- [ ] **Step 5: Rodar e ver passar e verificar o bundle** — `npx ng test --watch=false && npx ng build` → PASS; o "Initial total" do build não muda em relação a antes da instalação (as libs ainda não são importadas).
- [ ] **Step 6: Commit** — `git commit -m "feat(frontend): servico de desempenho e dependencias de graficos"`

---

### Task 5: Front — gráficos de evolução e de aderência

**Files:**
- Create: `frontend/src/app/shared/desempenho/grafico-evolucao/grafico-evolucao.ts|html|scss`, `grafico-evolucao.spec.ts`
- Create: `frontend/src/app/shared/desempenho/grafico-aderencia/grafico-aderencia.ts|html|scss`, `grafico-aderencia.spec.ts`
- Create: `frontend/src/app/shared/desempenho/dados-graficos.ts`, `dados-graficos.spec.ts`

**Interfaces:**
- Consumes: Task 4.
- Produces:
  - `dados-graficos.ts` (funções puras): `dadosEvolucao(pontos: PontoEvolucao[], medida: 'carga' | 'volume'): ChartData<'line'>` — rótulos `dd/MM`, dados `cargaMaxima` (nulo vira lacuna, `null` no array, com `spanGaps: true`) ou `volume`; rótulo da série `Carga máxima (kg)` / `Volume (kg)`; `dadosAderencia(semanas: SemanaAderencia[]): ChartData<'bar'>` — rótulos `dd/MM` do início da semana, dados `percentual` (nulo → 0) e rótulo `Aderência (%)`.
  - `GraficoEvolucao`: `pontos = input.required<PontoEvolucao[]>()`; toggle `Carga` / `Volume` (padrão Carga); `<canvas baseChart type="line">` com eixo Y começando em 0; vazio: `Ainda não há treinos com este exercício no período.`; `providers: [provideCharts(withDefaultRegisterables())]`.
  - `GraficoAderencia`: `semanas = input.required<SemanaAderencia[]>()`; barras com eixo Y de 0 a 100; texto `Aderência média: <n>%` (ou `Sem ficha no período` quando `aderenciaMedia` é nulo); `providers` idem.
  - Ambos responsivos (`maintainAspectRatio: false` num contêiner com altura fixa de 240px).

- [ ] **Step 1: Testes que falham** — nos specs dos componentes, antes de criar o componente, fazer `HTMLCanvasElement.prototype.getContext = () => null` (o jsdom não tem canvas) para evitar ruído; testar as funções puras com precisão e os componentes pelos textos/estados:
```ts
// dados-graficos.spec.ts
it('dadosEvolucao em carga usa cargaMaxima e rótulos dd/MM', ...)
it('pontoSemCargaViraLacuna', ...)                       // cargaMaxima null → null no array e spanGaps true
it('dadosEvolucao em volume usa volume', ...)
it('dadosAderencia usa percentual e nulo vira 0', ...)
// grafico-evolucao.spec.ts
it('sem pontos mostra a mensagem de vazio', ...)
it('toggle Volume troca a medida', ...)
// grafico-aderencia.spec.ts
it('mostra a aderência média', ...)                       // 'Aderência média: 75%'
it('sem ficha no período', ...)
```
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar** — `npx ng test --watch=false && npx ng build` → PASS; "Initial total" sem aumento relevante (até ~2 kB), Chart.js só em chunks lazy.
- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): graficos de evolucao e aderencia"`

---

### Task 6: Front — painel de desempenho (detalhe do aluno e "Evolução" do aluno)

**Files:**
- Create: `frontend/src/app/shared/desempenho/painel-desempenho/painel-desempenho.ts|html|scss`, `painel-desempenho.spec.ts`
- Create: `frontend/src/app/aluno/evolucao/evolucao.ts`, `evolucao.spec.ts`
- Modify: `frontend/src/app/personal/alunos/detalhe-aluno/detalhe-aluno.html|ts|spec.ts` (incluir o painel depois do histórico), `frontend/src/app/aluno/aluno.routes.ts` (item de navegação e rota `evolucao`, lazy)

**Interfaces:**
- Consumes: Tasks 4 e 5.
- Produces:
  - `PainelDesempenho` com `alvo = input.required<AlvoDesempenho>()`: título `Desempenho`; seção `Aderência semanal` com `GraficoAderencia` (8 semanas); seção `Evolução por exercício` com `mat-select` `Exercício` (exercícios executados; padrão o primeiro) e `GraficoEvolucao` do selecionado; trocar o exercício recarrega a evolução (descartando respostas atrasadas); sem exercícios executados: `Nenhum treino registrado ainda.`; estados `Carregando…` e `Não foi possível carregar o desempenho.`
  - No `DetalheAluno`: `<app-painel-desempenho [alvo]="{ tipo: 'aluno', alunoId: a.id }" />` (memorizar o objeto para não recriar a cada detecção de mudanças).
  - Aluno: item de navegação `{ rota: '/aluno/evolucao', rotulo: 'Evolução', icone: 'trending_up' }` depois de Histórico; rota `evolucao` → `Evolucao` (lazy), que mostra o título `Minha evolução` e o painel com `{ tipo: 'aluno-logado' }`.

- [ ] **Step 1: Testes que falham**
```ts
// painel-desempenho.spec.ts (DesempenhoService falso)
it('carrega aderência e exercícios e mostra a evolução do primeiro', ...)
it('trocar o exercício recarrega a evolução', ...)
it('sem exercícios executados mostra a mensagem', ...)
it('erro mostra a mensagem de falha', ...)
// evolucao.spec.ts
it('usa o alvo aluno-logado', ...)
```
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar** e registrar rota e item.
- [ ] **Step 4: Rodar e ver passar** — `npx ng test --watch=false && npx ng build` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): painel de desempenho no detalhe do aluno e evolucao do aluno"`

---

### Task 7: Front — dashboard do personal

**Files:**
- Modify: `frontend/src/app/personal/dashboard/dashboard.ts` (vira `dashboard.ts|html|scss`), `frontend/src/app/personal/personal.routes.ts` se necessário
- Test: `frontend/src/app/personal/dashboard/dashboard.spec.ts`

**Interfaces:**
- Consumes: Task 4 (`dashboard()`), `UsuarioService`.
- Produces: `Dashboard`: saudação `Olá, <nome>`; três indicadores — `Alunos ativos` (total), `Sumiram` (quantos com `sumiu`), `Fichas vencidas` (quantos com `fichaVencida`); lista dos alunos na ordem da API, cada item levando a `/personal/alunos/<alunoId>` com: nome; `<realizados>/<planejado> treinos nesta semana` (sem ficha: `<realizados> treinos nesta semana · Sem ficha`; `1 treino` no singular); `Último treino hoje` / `Último treino ontem` / `Último treino há <n> dias` / `Nunca treinou`; etiquetas `Sumiu` e `Ficha vencida` quando for o caso. Vazio: `Nenhum aluno ativo ainda.` com link `Ir para Alunos` (`/personal/alunos`). Estados `Carregando…` e `Não foi possível carregar o dashboard.`

- [ ] **Step 1: Testes que falham**
```ts
it('mostra os indicadores de ativos, sumiram e fichas vencidas', ...)
it('mostra treinos da semana e último treino', ...)       // '2/3 treinos nesta semana', 'Último treino há 3 dias'
it('mostraNuncaTreinou', ...)
it('sem ficha mostra Sem ficha', ...)
it('etiquetas Sumiu e Ficha vencida', ...)
it('item leva ao detalhe do aluno', ...)
it('sem alunos mostra a mensagem e o link', ...)
```
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar e verificar no navegador**

Run: `npx ng test --watch=false && npx ng build` → PASS.
Verificação manual (usuário): como personal, o dashboard mostra os alunos ativos com treinos da semana e último treino; um aluno sem treino há mais de 7 dias aparece primeiro com "Sumiu"; no detalhe de um aluno com treinos, a seção "Desempenho" mostra a aderência e a evolução do exercício escolhido; como aluno, o menu "Evolução" mostra os próprios gráficos.

- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): dashboard do personal"`
