# Etapa 5 — Sessões do aluno: Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** O aluno vê o treino de hoje (ficha ativa e divisão sugerida), executa o treino no celular marcando cada série com carga e repetições — sem perder nada se a internet cair — e consulta o histórico; o personal vê o histórico de treinos de cada aluno.

**Architecture:** Novo módulo `sessao` no back (tabelas `sessao` e `serie_executada`, registro idempotente por `client_id`, ficha ativa do aluno com a próxima divisão, listagens paginadas e detalhe). O módulo `sessao` depende de `ficha` (nunca o contrário). No front, a área do aluno ganha "Treino de hoje", "Execução" e "Histórico", com um `SessaoLocalService` isolado que guarda o treino em andamento e a fila de envios no `localStorage`; o personal ganha o histórico no detalhe do aluno e uma tela de detalhe de sessão compartilhada.

**Tech Stack:** Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Validation, Flyway, Spring Data web `PagedModel`), PostgreSQL 17, Testcontainers · Angular 22, Angular Material, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-18-app-personal-trainer-design.md` (§5 "sessao" e "serie_executada", §6 "Sessões (registro do aluno)", §7, §8 telas do aluno e "Sessão com tolerância a queda de conexão"). Pendências: `docs/superpowers/plans/2026-10-18-etapa4-pendencias.md` (seção "Incluir no plano da Etapa 5").

**Fora desta etapa:** "Minha evolução", gráficos, aderência e dashboard (Etapa 6).

## Global Constraints
- Tudo das *Global Constraints* das Etapas 1–4 continua valendo (pacote `com.projetofit`, Flyway com RLS sem políticas, DTOs `record`, Problem Details pt-BR, escopo por usuário autenticado — recurso de outro → 404, pt-BR na interface, nunca commitar `frontend/src/environments/environment.development.ts`).
- Endpoints do aluno (`/api/me/ficha-ativa`, `POST /api/sessoes`, `/api/me/sessoes`) exigem papel `ALUNO`; `/api/alunos/{id}/sessoes` exige `PERSONAL`; `GET /api/sessoes/{id}` aceita os dois (aluno só a própria; personal só de aluno seu).
- O aluno é identificado pelo `usuario_id` do token: `alunoRepository.findByUsuarioId(usuarioAtual.id())`.
- **Sessões não são editadas nem apagadas** (spec §6). Não existe `PUT`/`DELETE` de sessão.
- `client_id` gerado no celular (`crypto.randomUUID()`), único no banco; reenvio com o mesmo `clientId` do mesmo aluno devolve **200** com a sessão existente (idempotente).
- Paginação no formato do Spring Data com `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`: `{ "content": [...], "page": { "size", "number", "totalElements", "totalPages" } }`; parâmetros `page` (0-based) e `size` (padrão 20, máximo 50).
- Horários de sessão trafegam como instante ISO-8601 (`Instant`); exibição no front no fuso do aparelho.
- Chaves do `localStorage` incluem o id do usuário: `sessao-em-andamento:<usuarioId>`, `sessoes-pendentes:<usuarioId>`, `sessoes-com-erro:<usuarioId>`.
- Testes: back `cd backend && ./mvnw -q test -Dtest=<Classe>` e `./mvnw -q verify`; front `cd frontend && npx ng test --watch=false` e `npx ng build`.

## Review Focus
1. **Resposta perdida no envio** (a sessão chegou ao servidor mas a internet caiu antes da resposta): o reenvio não pode duplicar. → Task 3, `mesmoClientIdDevolveAMesmaSessao`; Task 6, `reenvioDepoisDeFalhaDeRedeNaoDuplica`.
2. **App fechado ou recarregado no meio do treino**: o treino em andamento volta como estava. → Task 6, `restauraSessaoEmAndamento`; Task 8, `abrirExecucaoComTreinoSalvoMostraSeriesConcluidas`.
3. **Personal edita a ficha enquanto o aluno treina sem internet** (remove a divisão ou o exercício): o envio não pode ser recusado para sempre. → Task 3, `divisaoRemovidaDuranteOTreinoAindaRegistra` e `itemRemovidoDuranteOTreinoAindaRegistra`.
4. **`localStorage` corrompido ou indisponível** (modo privado, JSON inválido): a tela não quebra. → Task 6, `storageCorrompidoComecaDoZero`.
5. **Envio recusado pelo servidor** (ex.: relógio do celular adiantado → 400): o treino não some — vai para "não enviados" com a mensagem e pode ser descartado manualmente. → Task 6, `recusaDoServidorMoveParaComErro`; Task 7, `mostraTreinosNaoEnviados`.

---

### Task 1: Tabelas de sessão, entidades e repositório

**Files:**
- Create: `backend/src/main/resources/db/migration/V5__sessao.sql`
- Create: `backend/src/main/java/com/projetofit/sessao/Sessao.java`, `SerieExecutada.java`, `SessaoRepository.java`
- Modify (teste): `backend/src/test/java/com/projetofit/MigracoesIT.java` e as limpezas dos ITs (apagar `sessao` antes de `ficha`/`aluno`/`exercicio`/`usuario`)

**Interfaces:**
- Produces:
  - Tabelas (spec §5, com `ordem` acrescentado para preservar a ordem das séries):
    - `sessao(id uuid primary key default gen_random_uuid(), client_id uuid not null unique, aluno_id uuid not null references aluno(id), divisao_id uuid references divisao(id) on delete set null, divisao_nome text not null, iniciada_em timestamptz not null, finalizada_em timestamptz not null, observacao text, criado_em timestamptz not null default now())`; `create index sessao_aluno_finalizada_idx on sessao (aluno_id, finalizada_em desc);`
    - `serie_executada(id uuid primary key default gen_random_uuid(), sessao_id uuid not null references sessao(id) on delete cascade, exercicio_id uuid not null references exercicio(id), item_divisao_id uuid references item_divisao(id) on delete set null, ordem int not null, numero_serie int not null check (numero_serie >= 1), repeticoes int not null check (repeticoes >= 0), carga_kg numeric(6,2) check (carga_kg >= 0))`; `create index serie_executada_exercicio_idx on serie_executada (exercicio_id);`
    - RLS nas duas.
  - `@Entity Sessao`: `id` (`GenerationType.UUID`), `UUID clientId`, `@ManyToOne(LAZY, optional = false) Aluno aluno`, `@ManyToOne(LAZY) Divisao divisao` (nulável), `String divisaoNome`, `Instant iniciadaEm`, `Instant finalizadaEm`, `String observacao`, `Instant criadoEm`; `@OneToMany(mappedBy = "sessao", cascade = ALL, orphanRemoval = true) @OrderBy("ordem") List<SerieExecutada> series`. Construtor `Sessao(UUID clientId, Aluno aluno, Divisao divisao, String divisaoNome, Instant iniciadaEm, Instant finalizadaEm, String observacao)`; `SerieExecutada adicionarSerie(Exercicio exercicio, ItemDivisao item, int ordem, int numeroSerie, int repeticoes, BigDecimal cargaKg)`; getters.
  - `@Entity SerieExecutada` (`serie_executada`): `id`, `@ManyToOne(LAZY, optional = false) Sessao sessao`, `@ManyToOne(LAZY, optional = false) Exercicio exercicio`, `@ManyToOne(LAZY) ItemDivisao itemDivisao`, `int ordem`, `int numeroSerie`, `int repeticoes`, `@Column(precision = 6, scale = 2) BigDecimal cargaKg`; getters.
  - `SessaoRepository extends JpaRepository<Sessao, UUID>`:
    - `Optional<Sessao> findByClientId(UUID clientId)`
    - `Page<Sessao> findByAlunoIdOrderByFinalizadaEmDesc(UUID alunoId, Pageable pageable)`
    - `Optional<Sessao> findByIdAndAlunoId(UUID id, UUID alunoId)`
    - `Optional<Sessao> findByIdAndAlunoPersonalId(UUID id, UUID personalId)`
    - `Optional<Sessao> findFirstByAlunoIdAndDivisaoFichaIdOrderByFinalizadaEmDesc(UUID alunoId, UUID fichaId)` (última sessão do aluno cuja divisão ainda existe e pertence à ficha)

- [ ] **Step 1: Testes que falham** em `MigracoesIT`:
```java
// teste geral de RLS passa a exigir também .contains("sessao", "serie_executada")
@Test void clientIdEhUnico()                                   // 2º insert com o mesmo client_id → DataIntegrityViolationException
@Test void apagarDivisaoDeixaSessaoComDivisaoNula()             // sessão ligada à divisão D; delete from divisao where id = D → sessao.divisao_id is null, divisao_nome mantido
@Test void apagarItemDeixaSerieComItemNulo()                    // série ligada ao item I; delete from item_divisao where id = I → serie_executada.item_divisao_id is null
@Test void repeticoesNegativasSaoRecusadas()                    // repeticoes = -1 → DataIntegrityViolationException
```
- [ ] **Step 2: Rodar e ver falhar** — `./mvnw -q test -Dtest=MigracoesIT` → FAIL.
- [ ] **Step 3: Implementar** migration, entidades, repositório e limpezas.
- [ ] **Step 4: Rodar e ver passar** — `./mvnw -q verify` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(backend): tabelas de sessao e serie executada com RLS, entidades e repositorio"`

---

### Task 2: Ficha ativa do aluno com a próxima divisão

**Files:**
- Modify: `backend/src/main/java/com/projetofit/ficha/FichaDTO.java` (tornar `de(...)` público)
- Create: `backend/src/main/java/com/projetofit/sessao/FichaAtivaDTO.java`, `TreinoDoAlunoService.java`, `TreinoDoAlunoController.java`, `AlunoLogado.java`
- Test: `backend/src/test/java/com/projetofit/sessao/TreinoDoAlunoControllerIT.java`

**Interfaces:**
- Consumes: Task 1 (`SessaoRepository.findFirstByAlunoIdAndDivisaoFichaIdOrderByFinalizadaEmDesc`); `FichaRepository.findByAlunoIdAndStatus`, `FichaDTO.de(Ficha, LocalDate hoje, ZoneId zona)` (Etapa 4); `Clock`; `UsuarioAtual`; `AlunoRepository.findByUsuarioId`.
- Produces:
  - `@Component AlunoLogado`: `Aluno obter()` — `alunoRepository.findByUsuarioId(usuarioAtual.id())`; ausente → `RecursoNaoEncontradoException("Aluno não encontrado.")` (reutilizado nas Tasks 3 e 4).
  - `record FichaAtivaDTO(FichaDTO ficha, UUID proximaDivisaoId)`.
  - `TreinoDoAlunoService`: `@Transactional(readOnly = true) Optional<FichaAtivaDTO> fichaAtiva()` — ficha `ATIVA` do aluno logado; `proximaDivisaoId` pelo algoritmo:
    ```
    divisões = ficha.divisões ordenadas por ordem
    última = sessaoRepository.findFirstByAlunoIdAndDivisaoFichaIdOrderByFinalizadaEmDesc(aluno.id, ficha.id)
    se não há última: primeira divisão
    senão: a divisão seguinte à da última (por ordem); se a última era a de maior ordem: a primeira
    ```
  - `TreinoDoAlunoController` `@PreAuthorize("hasRole('ALUNO')")`: `GET /api/me/ficha-ativa` → **200** com o DTO, ou **204** sem corpo se não houver ficha ativa (o front trata como "sem ficha" sem mostrar erro).

- [ ] **Step 1: Testes que falham** (`TreinoDoAlunoControllerIT`; grava personal, aluno ATIVO com `Usuario` ALUNO, exercícios e uma ficha com divisões A, B, C pelos repositórios; sessões gravadas direto pelo `SessaoRepository`):
```java
@Test void semFichaAtivaRetorna204()
@Test void semSessaoSugereAPrimeiraDivisao()             // proximaDivisaoId == A; $.ficha.divisoes tem 3
@Test void depoisDeAsugereB()                             // última sessão em A → B
@Test void depoisDaUltimaVoltaParaAPrimeira()             // última em C → A
@Test void sessaoDeFichaAnteriorNaoConta()                // última sessão foi numa divisão de ficha encerrada → sugere A da ficha ativa
@Test void sessaoComDivisaoRemovidaEhIgnorada()           // mais recente com divisao null, anterior em A → sugere B
@Test void personalNaoAcessa()                            // token de personal → 403
```
- [ ] **Step 2: Rodar e ver falhar** — `./mvnw -q test -Dtest=TreinoDoAlunoControllerIT` → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar** — `./mvnw -q verify` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(backend): ficha ativa do aluno com sugestao da proxima divisao"`

---

### Task 3: Registrar sessão (idempotente)

**Files:**
- Create: `backend/src/main/java/com/projetofit/sessao/SessaoRequest.java`, `SerieRequest.java`, `SessaoDTO.java`, `SerieExecutadaDTO.java`, `SessaoService.java`, `SessaoController.java`
- Test: `backend/src/test/java/com/projetofit/sessao/RegistrarSessaoIT.java`

**Interfaces:**
- Consumes: Task 1, Task 2 (`AlunoLogado`), `Clock`, `DadosInvalidosException`, `ExercicioRepository`, `ItemDivisao`, `Divisao` (com `getFicha().getAluno()`).
- Produces:
  - `record SessaoRequest(@NotNull UUID clientId, UUID divisaoId, @NotBlank @Size(max = 60) String divisaoNome, @NotNull Instant iniciadaEm, @NotNull Instant finalizadaEm, @Size(max = 1000) String observacao, @NotEmpty @Size(max = 200) List<@NotNull @Valid SerieRequest> series)`.
  - `record SerieRequest(UUID itemDivisaoId, @NotNull UUID exercicioId, @NotNull @Min(1) @Max(50) Integer numeroSerie, @NotNull @Min(0) @Max(999) Integer repeticoes, @DecimalMin("0") @Digits(integer = 4, fraction = 2) BigDecimal cargaKg)`.
  - `record SerieExecutadaDTO(UUID exercicioId, String exercicioNome, UUID itemDivisaoId, int ordem, int numeroSerie, int repeticoes, BigDecimal cargaKg)`.
  - `record SessaoDTO(UUID id, UUID clientId, UUID alunoId, UUID divisaoId, String divisaoNome, Instant iniciadaEm, Instant finalizadaEm, String observacao, List<SerieExecutadaDTO> series)` com `static SessaoDTO de(Sessao s)` (séries por `ordem`).
  - `SessaoService.registrar(SessaoRequest req): Resultado` onde `record Resultado(SessaoDTO sessao, boolean criada)`; `@Transactional`, regras na ordem:
    1. `findByClientId(clientId)`: existe e é do aluno logado → `Resultado(dto, false)`; existe e é de outro → `ConflitoException("Sessão já registrada.")`;
    2. `finalizadaEm` antes de `iniciadaEm` → `DadosInvalidosException("finalizadaEm", "O fim do treino deve ser depois do início.")`; `finalizadaEm` mais de 5 minutos depois de `Instant.now(clock)` → `DadosInvalidosException("finalizadaEm", "O horário de fim está no futuro. Confira o relógio do celular.")`;
    3. divisão: `divisaoId` informado e encontrado → precisa pertencer a uma ficha do aluno logado (ativa ou encerrada), senão `DadosInvalidosException("divisaoId", "Divisão inválida.")`; o nome gravado é o da divisão no banco. `divisaoId` nulo ou não encontrado (divisão removida enquanto o aluno treinava sem internet) → grava `divisao = null` com `divisaoNome` do request (`trim`);
    4. cada série: exercício precisa ser do personal do aluno (ativo ou não), senão `DadosInvalidosException("series[i].exercicioId", "Exercício inválido.")`; `itemDivisaoId` informado e encontrado **na divisão da sessão** → liga; senão grava `null`;
    5. `ordem` = posição + 1; `observacao` com `trim`, vazia → `null`; `saveAndFlush`; `DataIntegrityViolationException` (dois envios simultâneos do mesmo `clientId`) → relê por `clientId` fora da transação falhada e devolve `Resultado(dto, false)` (fazer o registro em `TransactionTemplate` dentro do serviço, como em `AlunoService.criar`, para a releitura funcionar).
  - `SessaoController` `@RestController`: `POST /api/sessoes` com `@PreAuthorize("hasRole('ALUNO')")` → **201** se `criada`, **200** se já existia.

- [ ] **Step 1: Testes que falham** (`RegistrarSessaoIT`; ficha com divisões A e B e itens; token do aluno):
```java
@Test void registrarSessao()                          // 201; $.series na ordem enviada; $.divisaoNome "Treino A" (do banco, mesmo que o request mande outro nome); cargaKg 22.5
@Test void mesmoClientIdDevolveAMesmaSessao()        // 2º POST igual → 200 com o mesmo $.id; count de sessões == 1
@Test void clientIdDeOutroAlunoRetorna409()          // detail "Sessão já registrada."
@Test void fimAntesDoInicioRetorna400()              // errors.finalizadaEm = "O fim do treino deve ser depois do início."
@Test void fimNoFuturoRetorna400()                   // finalizadaEm = agora + 10 min → errors.finalizadaEm = "O horário de fim está no futuro. Confira o relógio do celular."
@Test void fimAteCincoMinutosNoFuturoEhAceito()      // agora + 4 min → 201
@Test void semSeriesRetorna400()                     // series [] → 400, $.errors.series exists
@Test void divisaoDeOutroAlunoRetorna400()           // errors.divisaoId = "Divisão inválida."
@Test void divisaoRemovidaDuranteOTreinoAindaRegistra() // divisaoId inexistente + divisaoNome "Treino A" → 201, $.divisaoId null, $.divisaoNome "Treino A"
@Test void itemRemovidoDuranteOTreinoAindaRegistra()   // itemDivisaoId inexistente → 201, série com itemDivisaoId null
@Test void divisaoDeFichaEncerradaEhAceita()           // ficha encerrada depois de começar o treino → 201 ligado à divisão
@Test void exercicioDeOutroPersonalRetorna400()        // errors['series[0].exercicioId'] = "Exercício inválido."
@Test void personalNaoRegistraSessao()                 // token de personal → 403
```
- [ ] **Step 2: Rodar e ver falhar** — `./mvnw -q test -Dtest=RegistrarSessaoIT` → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar** — `./mvnw -q verify` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(backend): registro idempotente de sessao de treino"`

---

### Task 4: Histórico de sessões (listas paginadas e detalhe)

**Files:**
- Create: `backend/src/main/java/com/projetofit/comum/PaginacaoConfig.java`, `backend/src/main/java/com/projetofit/sessao/SessaoResumoDTO.java`
- Modify: `SessaoService.java`, `SessaoController.java`
- Test: `backend/src/test/java/com/projetofit/sessao/HistoricoSessoesIT.java`

**Interfaces:**
- Consumes: Tasks 1–3; `AlunoRepository.findByIdAndPersonalId`; `UsuarioAtual.papel()`.
- Produces:
  - `PaginacaoConfig`: `@Configuration @EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)`; tamanho máximo 50 (`spring.data.web.pageable.max-page-size: 50` e `default-page-size: 20` no `application.yml`).
  - `record SessaoResumoDTO(UUID id, String divisaoNome, Instant iniciadaEm, Instant finalizadaEm, int quantidadeExercicios, int quantidadeSeries)` (`quantidadeExercicios` = exercícios distintos).
  - `SessaoService`:
    - `@Transactional(readOnly = true) Page<SessaoResumoDTO> minhas(Pageable p)` — do aluno logado, por `finalizadaEm` desc;
    - `@Transactional(readOnly = true) Page<SessaoResumoDTO> doAluno(UUID alunoId, Pageable p)` — aluno do personal (senão 404 "Aluno não encontrado.");
    - `@Transactional(readOnly = true) SessaoDTO buscar(UUID id)` — papel `ALUNO`: `findByIdAndAlunoId(id, alunoLogado.id)`; papel `PERSONAL`: `findByIdAndAlunoPersonalId(id, usuarioAtual.id())`; ausente → `RecursoNaoEncontradoException("Sessão não encontrada.")`.
  - Controller: `GET /api/me/sessoes` (`hasRole('ALUNO')`) e `GET /api/alunos/{alunoId}/sessoes` (`hasRole('PERSONAL')`) devolvem `PagedModel<SessaoResumoDTO>` (ou `Page`, serializado via DTO); `GET /api/sessoes/{id}` (`hasAnyRole('PERSONAL','ALUNO')`).

- [ ] **Step 1: Testes que falham** (`HistoricoSessoesIT`; 3 sessões do aluno em datas diferentes, 1 de outro aluno do mesmo personal):
```java
@Test void minhasSessoesPaginadasDaMaisRecente()    // GET /api/me/sessoes?size=2 → $.content length 2, a mais recente primeiro; $.page.totalElements 3; $.page.totalPages 2
@Test void resumoContaExerciciosESeries()            // sessão com 2 exercícios × 3 séries → quantidadeExercicios 2, quantidadeSeries 6
@Test void personalVeSessoesDoAluno()                // GET /api/alunos/{id}/sessoes → 3
@Test void personalNaoVeAlunoDeOutroPersonal()       // → 404 "Aluno não encontrado."
@Test void alunoVeDetalheDaPropriaSessao()           // GET /api/sessoes/{id} → 200 com séries
@Test void alunoNaoVeSessaoDeOutroAluno()            // → 404 "Sessão não encontrada."
@Test void personalVeDetalheDeSessaoDoSeuAluno()     // → 200
@Test void tamanhoDePaginaLimitadoA50()              // ?size=500 → $.page.size 50
```
- [ ] **Step 2: Rodar e ver falhar** — `./mvnw -q test -Dtest=HistoricoSessoesIT` → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar** — `./mvnw -q verify` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(backend): historico paginado e detalhe de sessoes"`

---

### Task 5: Front — modelo e `SessaoService`

**Files:**
- Create: `frontend/src/app/shared/sessao/sessao.model.ts`, `sessao.service.ts`, `sessao.service.spec.ts`

**Interfaces:**
- Produces:
  - `sessao.model.ts`: `interface Pagina<T> { content: T[]; page: { size: number; number: number; totalElements: number; totalPages: number } }`; `interface FichaAtiva { ficha: Ficha; proximaDivisaoId: string | null }` (`Ficha` de `personal/fichas/ficha.model.ts`); `interface SerieRequest { itemDivisaoId: string | null; exercicioId: string; numeroSerie: number; repeticoes: number; cargaKg: number | null }`; `interface SessaoRequest { clientId: string; divisaoId: string | null; divisaoNome: string; iniciadaEm: string; finalizadaEm: string; observacao: string | null; series: SerieRequest[] }`; `interface SerieExecutada { exercicioId: string; exercicioNome: string; itemDivisaoId: string | null; ordem: number; numeroSerie: number; repeticoes: number; cargaKg: number | null }`; `interface Sessao { id: string; clientId: string; alunoId: string; divisaoId: string | null; divisaoNome: string; iniciadaEm: string; finalizadaEm: string; observacao: string | null; series: SerieExecutada[] }`; `interface SessaoResumo { id: string; divisaoNome: string; iniciadaEm: string; finalizadaEm: string; quantidadeExercicios: number; quantidadeSeries: number }`; `function formatarDataHora(iso: string): string` (`dd/MM/yyyy HH:mm` no fuso do aparelho); `function duracaoMinutos(inicio: string, fim: string): number` (arredondado, mínimo 1).
  - `SessaoService` (`providedIn: 'root'`): `fichaAtiva(): Promise<FichaAtiva | null>` (GET `/api/me/ficha-ativa`, `observe: 'response'`; 204 → `null`), `registrar(req: SessaoRequest): Promise<Sessao>` (POST `/api/sessoes`), `minhas(pagina: number): Promise<Pagina<SessaoResumo>>` (GET `/api/me/sessoes?page=&size=20`), `doAluno(alunoId: string, pagina: number): Promise<Pagina<SessaoResumo>>`, `buscar(id: string): Promise<Sessao>`.

- [ ] **Step 1: Testes que falham**: `fichaAtiva` com 204 → `null` e com 200 → objeto; `registrar` faz POST com o corpo; `minhas(1)` envia `page=1&size=20`; `doAluno` e `buscar` com as URLs certas; `formatarDataHora` e `duracaoMinutos` (`'2026-10-18T10:00:00Z'` → `'2026-10-18T10:47:00Z'` = 47; menos de 1 min → 1).
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar** — `npx ng test --watch=false` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): modelo e servico de sessoes"`

---

### Task 6: Front — `SessaoLocalService` (treino em andamento e fila de envio)

**Files:**
- Create: `frontend/src/app/aluno/sessao-local/sessao-local.service.ts`, `sessao-local.model.ts`, `sessao-local.service.spec.ts`

**Interfaces:**
- Consumes: Task 5 (`SessaoService.registrar`, tipos), `UsuarioService.usuario()` (id para as chaves), `Ficha`/`Divisao`/`ItemDivisao`.
- Produces:
  - `sessao-local.model.ts`: `interface SerieEmAndamento { numero: number; cargaKg: number | null; repeticoes: number | null; concluida: boolean }`; `interface ExercicioEmAndamento { itemDivisaoId: string; exercicioId: string; exercicioNome: string; prescricao: { series: number; repeticoes: string; cargaKg: number | null; descansoSeg: number | null; observacao: string | null }; series: SerieEmAndamento[] }`; `interface SessaoEmAndamento { clientId: string; fichaId: string; divisaoId: string; divisaoNome: string; iniciadaEm: string; observacao: string; exercicios: ExercicioEmAndamento[] }`; `interface EnvioComErro { sessao: SessaoRequest; mensagem: string }`; `function repeticoesSugeridas(prescricao: string): number` (`'10'` → 10; `'8-12'` → 8).
  - `SessaoLocalService` (`providedIn: 'root'`), estado em signals: `readonly emAndamento: Signal<SessaoEmAndamento | null>`, `readonly pendentes: Signal<number>`, `readonly comErro: Signal<EnvioComErro[]>`. Métodos:
    - `iniciar(ficha: Ficha, divisao: Divisao): SessaoEmAndamento` — `clientId = crypto.randomUUID()`, `iniciadaEm = new Date().toISOString()`, um `ExercicioEmAndamento` por item (na ordem) com `prescricao.series` séries pré-preenchidas (`cargaKg` = carga prescrita, `repeticoes` = `repeticoesSugeridas`, `concluida: false`); grava em `sessao-em-andamento:<usuarioId>`.
    - `atualizarSerie(exercicioIndex: number, serieIndex: number, mudanca: Partial<SerieEmAndamento>): void`, `adicionarSerie(exercicioIndex: number): void` (copia carga/repetições da última), `atualizarObservacao(texto: string): void` — todos gravam imediatamente.
    - `descartar(): void` — remove o treino em andamento.
    - `finalizar(): Promise<'enviado' | 'pendente'>` — monta o `SessaoRequest` só com as séries concluídas (na ordem dos exercícios e das séries; `finalizadaEm = new Date().toISOString()`); se nenhuma série concluída, lança `Error('Conclua pelo menos uma série para finalizar o treino.')` e não altera nada; senão põe na fila `sessoes-pendentes:<usuarioId>`, limpa o em andamento e chama `enviarPendentes()`; devolve `'enviado'` se a fila ficou vazia, senão `'pendente'`.
    - `enviarPendentes(): Promise<void>` — envia a fila em ordem, um por vez, sem execuções concorrentes (se já está enviando, devolve a mesma Promise): sucesso → remove; erro de rede (status 0), 5xx ou 401 → para e mantém; outro 4xx → move para `sessoes-com-erro:<usuarioId>` com `mensagem` = `detail` do Problem Details (ou `'O servidor recusou este treino.'`) e segue para o próximo.
    - `descartarComErro(index: number): void`.
    - Escuta o evento `online` da janela e chama `enviarPendentes()`.
    - Leitura do `localStorage` tolerante: JSON inválido ou `localStorage` indisponível → estado vazio, sem lançar (e apaga a chave inválida quando possível).

- [ ] **Step 1: Testes que falham** (com `SessaoService` falso e `localStorage` real do jsdom; usuário `u1`):
```ts
it('iniciar pré-preenche séries com a prescrição e grava no storage', ...)   // 3 x '8-12' com 40 kg → 3 séries {cargaKg: 40, repeticoes: 8, concluida: false}; chave 'sessao-em-andamento:u1'
it('restauraSessaoEmAndamento', ...)                                         // novo serviço lendo o mesmo storage → emAndamento() igual
it('finalizar envia só as séries concluídas, na ordem', ...)
it('finalizar sem série concluída lança erro e mantém o treino', ...)
it('falha de rede mantém na fila e devolve pendente', ...)                   // registrar rejeita com HttpErrorResponse status 0
it('reenvioDepoisDeFalhaDeRedeNaoDuplica', ...)                              // 1ª chamada falha (0), 2ª ok → registrar chamado 2x com o MESMO clientId; fila vazia
it('evento online dispara o envio', ...)
it('recusaDoServidorMoveParaComErro', ...)                                   // 400 { detail: 'O horário de fim está no futuro. Confira o relógio do celular.' } → comErro()[0].mensagem igual; pendentes() 0
it('401 mantém na fila', ...)
it('envios concorrentes não duplicam', ...)                                  // duas chamadas a enviarPendentes simultâneas → registrar 1x por sessão
it('storageCorrompidoComecaDoZero', ...)                                     // 'sessao-em-andamento:u1' = '{x' → emAndamento() null sem lançar
it('chaves separadas por usuário', ...)                                      // usuário u2 não vê o treino de u1
```
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar** — `npx ng test --watch=false` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): treino em andamento local e fila de envio de sessoes"`

---

### Task 7: Front — "Treino de hoje" e navegação do aluno

**Files:**
- Modify: `frontend/src/app/aluno/treino/treino.ts` (vira `treino.ts|html|scss`), `frontend/src/app/aluno/aluno.routes.ts`
- Test: `frontend/src/app/aluno/treino/treino.spec.ts`

**Interfaces:**
- Consumes: Tasks 5 e 6; `confirmar` (`shared/confirmacao/confirmacao-dialog.ts`); `formatarData` (`personal/fichas/ficha.model.ts`).
- Produces:
  - Itens de navegação do aluno: `[{ rota: '/aluno/treino', rotulo: 'Treino', icone: 'fitness_center' }, { rota: '/aluno/historico', rotulo: 'Histórico', icone: 'history' }]`.
  - `Treino` (rota `treino`): ao abrir chama `sessaoLocal.enviarPendentes()` e `sessaoService.fichaAtiva()`.
    - Treino em andamento → card `Treino em andamento: <divisaoNome>` com botões `Continuar` (→ `/aluno/treino/execucao`) e `Descartar` (confirmação: título `Descartar treino`, mensagem `As séries marcadas neste treino serão perdidas.`, botão `Descartar`).
    - Pendentes > 0 → aviso `<n> treino(s) aguardando envio` (`1 treino aguardando envio` / `2 treinos aguardando envio`) com botão `Enviar agora`.
    - `comErro()` → para cada um: `Um treino não pôde ser enviado: <mensagem>` com botão `Descartar` (`descartarComErro`).
    - Sem ficha ativa → `Você ainda não tem uma ficha ativa. Fale com seu personal.`
    - Com ficha: título com o nome da ficha; lista das divisões (nome e quantidade de exercícios) para escolher, com a sugerida pré-selecionada e marcada `Sugerido`; botão `Começar treino` (desabilitado se já houver treino em andamento) → `sessaoLocal.iniciar(ficha, divisaoEscolhida)` e navega para `/aluno/treino/execucao`.
    - Estados `Carregando…` e `Não foi possível carregar seu treino.`

- [ ] **Step 1: Testes que falham**
```ts
it('sem ficha ativa mostra a mensagem', ...)
it('pré-seleciona a divisão sugerida e marca Sugerido', ...)
it('Começar treino inicia a sessão local e vai para a execução', ...)
it('treino em andamento mostra Continuar e Descartar', ...)
it('mostraTreinosNaoEnviados', ...)                    // comErro com mensagem → texto 'Um treino não pôde ser enviado: ...' e Descartar
it('pendentes mostra aviso e Enviar agora', ...)        // '2 treinos aguardando envio'; clique → enviarPendentes
it('ao abrir tenta enviar pendentes', ...)
```
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar** a tela e os itens/rotas (`treino/execucao` é registrada na Task 8; `historico` e `historico/:id` na Task 9).
- [ ] **Step 4: Rodar e ver passar** — `npx ng test --watch=false && npx ng build` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): treino de hoje do aluno"`

---

### Task 8: Front — execução do treino

**Files:**
- Create: `frontend/src/app/aluno/execucao/execucao.ts|html|scss`, `execucao.spec.ts`
- Modify: `frontend/src/app/aluno/aluno.routes.ts` (rota `treino/execucao`, `loadComponent`)

**Interfaces:**
- Consumes: Task 6 (`SessaoLocalService`), `confirmar`.
- Produces: `Execucao`: sem treino em andamento → navega para `/aluno/treino`. Cabeçalho com `divisaoNome` e tempo decorrido (`<m> min`, atualizado a cada minuto). Um card por exercício: nome; prescrição `<séries> x <repetições>` + `· <carga> kg` (vírgula decimal, se houver) + `· <descanso> s` (se houver); observação do personal (se houver). Para cada série uma linha: `Série <n>`, campo `Carga (kg)` (`type="number"`, `inputmode="decimal"`, `step="0.5"`), campo `Repetições` (`type="number"`, `inputmode="numeric"`), botão `Concluir` que vira `Concluída` (com ícone de check) e pode ser tocado de novo para desfazer; cada mudança chama `sessaoLocal.atualizarSerie` na hora. Botão `Adicionar série` por exercício. Campo `Como foi o treino?` (opcional, máx. 1000) → `atualizarObservacao`. Botão `Finalizar treino`: se houver séries não concluídas, confirmação (título `Finalizar treino`, mensagem `Algumas séries não foram concluídas e não serão registradas.`, botão `Finalizar`); depois `finalizar()`: `'enviado'` → snackbar `Treino registrado!`; `'pendente'` → snackbar `Treino salvo. Ele será enviado quando houver conexão.`; em ambos navega para `/aluno/treino` (`replaceUrl: true`); erro "Conclua pelo menos uma série…" → mostra a mensagem e fica. Alvos de toque de pelo menos 48px.

- [ ] **Step 1: Testes que falham**
```ts
it('sem treino em andamento volta para o treino de hoje', ...)
it('mostra a prescrição 3 x 8-12 · 40 kg · 60 s', ...)
it('Concluir marca a série e grava', ...)              // atualizarSerie(0, 0, { concluida: true })
it('editar carga grava na hora', ...)                  // 42.5 → atualizarSerie(0, 0, { cargaKg: 42.5 })
it('abrirExecucaoComTreinoSalvoMostraSeriesConcluidas', ...)
it('finalizar com séries pendentes pede confirmação', ...)
it('finalizar enviado mostra Treino registrado! e volta', ...)
it('finalizar pendente avisa que será enviado depois', ...)
it('sem série concluída mostra o erro e fica', ...)
```
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar** e registrar a rota.
- [ ] **Step 4: Rodar e ver passar** — `npx ng test --watch=false && npx ng build` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): execucao do treino pelo aluno"`

---

### Task 9: Front — detalhe de sessão e histórico do aluno

**Files:**
- Create: `frontend/src/app/shared/sessao/detalhe-sessao/detalhe-sessao.ts|html|scss`, `detalhe-sessao.spec.ts`
- Create: `frontend/src/app/aluno/historico/historico.ts|html|scss`, `historico.spec.ts`
- Modify: `frontend/src/app/aluno/aluno.routes.ts` (rotas `historico` e `historico/:id`)

**Interfaces:**
- Consumes: Task 5.
- Produces:
  - `DetalheSessao` (rota com `id = input.required<string>()` e `voltarPara = input<string>()` via `data` da rota): cabeçalho com `divisaoNome`, `formatarDataHora(iniciadaEm)` e `<duração> min`; séries agrupadas por exercício (na ordem), cada linha `Série <n>: <repetições> reps` + ` · <carga> kg` (vírgula decimal, se houver); observação do aluno (se houver, rótulo `Observação`); link `Voltar` para `voltarPara` (padrão `/aluno/historico`). Estados `Carregando…` e `Não foi possível carregar o treino.`
  - `Historico` (aluno): título `Histórico`; lista de `minhas(0)`: data/hora, divisão, `<n> min`, `<k> exercícios`; cada item → `/aluno/historico/<id>`; botão `Carregar mais` enquanto houver páginas; vazio: `Você ainda não registrou nenhum treino.`
  - Rotas: `historico` → `Historico`; `historico/:id` → `DetalheSessao` com `data: { voltarPara: '/aluno/historico' }`.

- [ ] **Step 1: Testes que falham**
```ts
// detalhe-sessao.spec.ts
it('agrupa séries por exercício com reps e carga', ...)  // 'Série 1: 10 reps · 22,5 kg'
it('mostra duração e observação', ...)
// historico.spec.ts
it('lista sessões com data, divisão e duração', ...)
it('Carregar mais busca a próxima página', ...)          // minhas(1); itens acumulados
it('sem sessões mostra a mensagem', ...)
```
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar** e registrar as rotas.
- [ ] **Step 4: Rodar e ver passar** — `npx ng test --watch=false && npx ng build` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): historico e detalhe de sessoes do aluno"`

---

### Task 10: Front — histórico de treinos no detalhe do aluno (personal)

**Files:**
- Create: `frontend/src/app/personal/sessoes/sessoes-do-aluno/sessoes-do-aluno.ts|html|scss`, `sessoes-do-aluno.spec.ts`
- Modify: `frontend/src/app/personal/alunos/detalhe-aluno/detalhe-aluno.html|ts|spec.ts` (incluir `<app-sessoes-do-aluno [alunoId]="a.id" />` depois das fichas), `frontend/src/app/personal/personal.routes.ts` (rota `sessoes/:id` → `DetalheSessao` com `data: { voltarPara: ... }` — ver abaixo)

**Interfaces:**
- Consumes: Tasks 5 e 9 (`DetalheSessao`).
- Produces:
  - `SessoesDoAluno` com `alunoId = input.required<string>()`: título `Histórico de treinos`; lista de `doAluno(alunoId, 0)` com data/hora, divisão, `<n> min` e `<k> exercícios`, cada item → `/personal/sessoes/<id>`; `Carregar mais`; vazio: `Nenhum treino registrado ainda.`
  - Rota `sessoes/:id` → `DetalheSessao`. O `Voltar` do personal precisa levar ao aluno da sessão: o `DetalheSessao` aceita `voltarPara` com o marcador `aluno` (`data: { voltarPara: 'aluno' }`) e, nesse caso, monta `/personal/alunos/<sessao.alunoId>`.

- [ ] **Step 1: Testes que falham**
```ts
it('lista os treinos do aluno com link para o detalhe', ...)
it('Carregar mais busca a próxima página', ...)
it('sem treinos mostra a mensagem', ...)
// em detalhe-sessao.spec.ts
it('voltarPara aluno aponta para o aluno da sessão', ...)
```
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar** e registrar a rota.
- [ ] **Step 4: Rodar e ver passar e verificar no navegador**

Run: `npx ng test --watch=false && npx ng build` → PASS.
Verificação manual (usuário): entrar como aluno (conta convidada na Etapa 2) com ficha ativa → "Treino de hoje" sugere o Treino A → `Começar treino` → marcar séries, mudar carga, recarregar a página no meio (o treino continua) → `Finalizar treino` → "Treino registrado!"; o próximo "Treino de hoje" sugere o Treino B; repetir com o Wi-Fi/dados desligados no DevTools (Network → Offline) → "Treino salvo. Ele será enviado quando houver conexão." → voltar online → o aviso some e o treino aparece no Histórico; como personal, ver o treino em "Histórico de treinos" no detalhe do aluno.

- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): historico de treinos do aluno para o personal"`
