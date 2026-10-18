# Etapa 4 — Fichas: Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** O personal monta, edita, visualiza e encerra fichas de treino A/B/C dos alunos (ficha → divisões → exercícios prescritos), com no máximo uma ficha ativa por aluno.

**Architecture:** Novo módulo `ficha` no back (tabelas `ficha`, `divisao`, `item_divisao`; entidades com cascata e `orphanRemoval`; serviço que grava a ficha inteira num único request e sincroniza divisões/itens por `id` na edição; controller com rotas aninhadas em `/api/alunos/{id}/fichas` e rotas próprias em `/api/fichas`). No front, seção "Fichas" no detalhe do aluno, editor de ficha (cabeçalho + divisões, cada divisão num componente próprio com seus itens) com um seletor de exercício em diálogo, e uma tela de visualização.

**Tech Stack:** Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Validation, Flyway), PostgreSQL 17, Testcontainers, JUnit 5 · Angular 22, Angular Material (Reactive Forms com `FormArray`, `MatDialog`), Vitest.

**Spec:** `docs/superpowers/specs/2026-10-18-app-personal-trainer-design.md` (seção 5 "ficha", "divisao", "item_divisao"; seção 6 "Fichas"; seção 7; seção 8 "Editor de ficha"). Pendências: `docs/superpowers/plans/2026-10-18-etapa3-pendencias.md` (seção "Incluir no plano da Etapa 4").

**Fora desta etapa:** `GET /api/me/ficha-ativa` e a "próxima divisão" do aluno (Etapa 5, junto com as sessões); o alerta de ficha vencida no dashboard (Etapa 6).

## Global Constraints
- Tudo das *Global Constraints* das Etapas 1–3 continua valendo (pacote `com.projetofit`, Flyway com RLS sem políticas, DTOs `record`, Problem Details pt-BR, toda consulta filtrada pelo personal autenticado — ficha de aluno de outro personal → 404, pt-BR na interface, nunca commitar `frontend/src/environments/environment.development.ts`).
- Endpoints de fichas são só do papel `PERSONAL`.
- Fichas **nunca são apagadas**, só encerradas (spec §5). Não existe `DELETE`.
- No máximo **uma ficha `ATIVA` por aluno** (índice único parcial). Criar uma ficha a torna `ATIVA` e encerra a anterior **na mesma transação**.
- Só fichas `ATIVA` podem ser editadas ou encerradas.
- A ordem de divisões e itens vem da **posição na lista** do request (1, 2, 3…); o cliente não envia `ordem`.
- Limites: até 10 divisões por ficha, até 20 itens por divisão; `frequencia_semanal` 1..7; `series` 1..20; `repeticoes` no formato `10` ou `8-12` (até 3 dígitos cada); `carga_kg` ≥ 0 com até 2 casas (`numeric(6,2)`); `descanso_seg` 0..3600.
- "Hoje" e "vencida" usam o fuso `America/Sao_Paulo` (bean `Clock`).
- Testes: back `cd backend && ./mvnw -q test -Dtest=<Classe>` e `./mvnw -q verify`; front `cd frontend && npx ng test --watch=false` e `npx ng build`.

## Review Focus
1. **Criar ficha nova quando já existe uma ativa**: o Hibernate executa INSERTs antes de UPDATEs no flush, então a nova `ATIVA` pode bater no índice único parcial antes de a antiga virar `ENCERRADA` → 500. Deve funcionar e deixar só a nova ativa. → Task 2, `criarNovaFichaEncerraAAnterior`.
2. **Exercício desativado depois de entrar numa ficha**: editar a ficha mantendo esse item precisa continuar funcionando; só exercícios **novos no item** precisam estar ativos. → Task 3, `editarMantendoExercicioQueFicouInativoFunciona`.
3. **`id` de divisão/item de outra ficha no PUT**: não pode "puxar" a divisão de outra ficha (nem de outro aluno). → Task 3, `idDeDivisaoDeOutraFichaRetorna400`.
4. **Carga com casa decimal** (`22,5` no teclado do celular): o front envia `22.5` e o back guarda `22.50`. → Task 7, `cargaDecimalEhEnviadaComoNumero`; Task 2, `criarFichaCompleta` (assert `22.5`).
5. **Editar uma ficha que já foi encerrada** (outra aba): 409 com mensagem clara, sem perder o formulário. → Task 3, `editarFichaEncerradaRetorna409`; Task 8, `erroAoSalvarMantemNoFormulario`.

---

### Task 1: Tabelas, entidades e repositório de fichas

**Files:**
- Create: `backend/src/main/resources/db/migration/V4__ficha.sql`
- Create: `backend/src/main/java/com/projetofit/ficha/StatusFicha.java`, `Ficha.java`, `Divisao.java`, `ItemDivisao.java`, `FichaRepository.java`
- Modify (teste): `backend/src/test/java/com/projetofit/MigracoesIT.java` e as limpezas dos ITs que apagam `aluno`/`exercicio`/`usuario`

**Interfaces:**
- Produces:
  - Tabelas exatamente como na spec §5 (com `default gen_random_uuid()` nas PKs, timestamps `criado_em`/`atualizado_em timestamptz not null default now()` em `ficha`):
    - `ficha(id, aluno_id uuid not null references aluno(id), nome text not null, objetivo text, frequencia_semanal int not null check (frequencia_semanal between 1 and 7), data_inicio date not null, data_fim date, status text not null check (status in ('ATIVA','ENCERRADA')), criado_em, atualizado_em)`; `create unique index ficha_ativa_por_aluno_uk on ficha (aluno_id) where status = 'ATIVA';` `create index ficha_aluno_id_idx on ficha (aluno_id);`
    - `divisao(id, ficha_id uuid not null references ficha(id) on delete cascade, nome text not null, ordem int not null)`
    - `item_divisao(id, divisao_id uuid not null references divisao(id) on delete cascade, exercicio_id uuid not null references exercicio(id), ordem int not null, series int not null check (series between 1 and 20), repeticoes text not null, carga_kg numeric(6,2) check (carga_kg >= 0), descanso_seg int check (descanso_seg >= 0), observacao text)`
    - RLS nas três.
  - `enum StatusFicha { ATIVA, ENCERRADA }`.
  - `@Entity Ficha`: `id` (`GenerationType.UUID`), `@ManyToOne(fetch = LAZY, optional = false) Aluno aluno`, `nome`, `objetivo`, `int frequenciaSemanal`, `LocalDate dataInicio`, `LocalDate dataFim`, `@Enumerated(STRING) StatusFicha status`, timestamps; `@OneToMany(mappedBy = "ficha", cascade = ALL, orphanRemoval = true) @OrderBy("ordem") List<Divisao> divisoes`. Construtor `Ficha(Aluno aluno, String nome, String objetivo, int frequenciaSemanal, LocalDate dataInicio, LocalDate dataFim)` com status `ATIVA`. Métodos: getters, `atualizarCabecalho(String nome, String objetivo, int frequenciaSemanal, LocalDate dataInicio, LocalDate dataFim)`, `encerrar()`, `boolean vencida(LocalDate hoje)` (`ATIVA` e `dataFim != null` e `dataFim.isBefore(hoje)`), `Divisao adicionarDivisao(String nome, int ordem)`, `void removerDivisoesExceto(Set<UUID> idsMantidos)`.
  - `@Entity Divisao`: `id`, `@ManyToOne(fetch = LAZY, optional = false) Ficha ficha`, `nome`, `int ordem`, `@OneToMany(mappedBy = "divisao", cascade = ALL, orphanRemoval = true) @OrderBy("ordem") List<ItemDivisao> itens`. Métodos: getters, `atualizar(String nome, int ordem)`, `ItemDivisao adicionarItem(Exercicio exercicio, int ordem, int series, String repeticoes, BigDecimal cargaKg, Integer descansoSeg, String observacao)`, `void removerItensExceto(Set<UUID> idsMantidos)`.
  - `@Entity ItemDivisao` (tabela `item_divisao`): `id`, `@ManyToOne(fetch = LAZY, optional = false) Divisao divisao`, `@ManyToOne(fetch = LAZY, optional = false) Exercicio exercicio`, `int ordem`, `int series`, `String repeticoes`, `@Column(precision = 6, scale = 2) BigDecimal cargaKg`, `Integer descansoSeg`, `String observacao`. Método `atualizar(Exercicio exercicio, int ordem, int series, String repeticoes, BigDecimal cargaKg, Integer descansoSeg, String observacao)` e getters.
  - `FichaRepository extends JpaRepository<Ficha, UUID>`:
    - `Optional<Ficha> findByIdAndAlunoPersonalId(UUID id, UUID personalId)`
    - `List<Ficha> findByAlunoIdOrderByDataInicioDescCriadoEmDesc(UUID alunoId)`
    - `Optional<Ficha> findByAlunoIdAndStatus(UUID alunoId, StatusFicha status)`
  - Limpezas: em todo IT que apaga `aluno`/`exercicio`/`usuario`, apagar `ficha` antes (o `on delete cascade` leva `divisao` e `item_divisao` junto).

- [ ] **Step 1: Testes que falham** em `MigracoesIT`:
```java
// o teste geral de RLS passa a exigir também .contains("ficha", "divisao", "item_divisao")
@Test void soUmaFichaAtivaPorAluno()         // 2ª ficha ATIVA para o mesmo aluno → DataIntegrityViolationException; uma ATIVA + uma ENCERRADA → ok
@Test void apagarFichaApagaDivisoesEItens()  // delete from ficha → divisao e item_divisao da ficha somem (on delete cascade)
@Test void seriesForaDoLimiteSaoRecusadas()  // series = 0 e 21 → DataIntegrityViolationException
```
- [ ] **Step 2: Rodar e ver falhar** — `./mvnw -q test -Dtest=MigracoesIT` → FAIL.
- [ ] **Step 3: Implementar** migration, enum, entidades, repositório e limpezas.
- [ ] **Step 4: Rodar e ver passar** — `./mvnw -q verify` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(backend): tabelas de ficha, divisao e item com RLS, entidades e repositorio"`

---

### Task 2: Criar, listar e buscar fichas

**Files:**
- Create: `backend/src/main/java/com/projetofit/comum/RelogioConfig.java`, `backend/src/main/java/com/projetofit/comum/erro/DadosInvalidosException.java`
- Modify: `backend/src/main/java/com/projetofit/comum/erro/TratadorGlobalDeErros.java`
- Create: `backend/src/main/java/com/projetofit/ficha/FichaRequest.java`, `DivisaoRequest.java`, `ItemRequest.java`, `FichaDTO.java`, `DivisaoDTO.java`, `ItemDivisaoDTO.java`, `FichaResumoDTO.java`, `FichaService.java`, `FichaController.java`
- Test: `backend/src/test/java/com/projetofit/ficha/FichaControllerIT.java`, `backend/src/test/java/com/projetofit/comum/erro/TratadorGlobalDeErrosTest.java`

**Interfaces:**
- Consumes: Task 1; `Aluno`/`AlunoRepository` (`findByIdAndPersonalId`), `Exercicio`/`ExercicioRepository` (`findByIdAndPersonalId`), `UsuarioAtual.id()`.
- Produces:
  - `RelogioConfig`: `@Bean Clock relogio()` = `Clock.system(ZoneId.of("America/Sao_Paulo"))`.
  - `DadosInvalidosException(String campo, String mensagem)` → 400 com `title "Dados inválidos"`, `detail "Um ou mais campos são inválidos."` e `errors: {campo: mensagem}` (handler novo no `TratadorGlobalDeErros`; teste standalone `dadosInvalidosRetorna400ComCampo`).
  - Requests (o mesmo `FichaRequest` serve para criar e editar):
    - `FichaRequest(@NotBlank @Size(max = 120) String nome, @Size(max = 500) String objetivo, @NotNull @Min(1) @Max(7) Integer frequenciaSemanal, @NotNull LocalDate dataInicio, LocalDate dataFim, @NotEmpty @Size(max = 10) List<@Valid DivisaoRequest> divisoes)`
    - `DivisaoRequest(UUID id, @NotBlank @Size(max = 60) String nome, @NotEmpty @Size(max = 20) List<@Valid ItemRequest> itens)`
    - `ItemRequest(UUID id, @NotNull UUID exercicioId, @NotNull @Min(1) @Max(20) Integer series, @NotBlank @Pattern(regexp = "^\\d{1,3}(-\\d{1,3})?$", message = "Use um número (ex.: 10) ou uma faixa (ex.: 8-12).") String repeticoes, @DecimalMin("0") @Digits(integer = 4, fraction = 2) BigDecimal cargaKg, @Min(0) @Max(3600) Integer descansoSeg, @Size(max = 500) String observacao)`
  - DTOs:
    - `FichaDTO(UUID id, UUID alunoId, String nome, String objetivo, int frequenciaSemanal, LocalDate dataInicio, LocalDate dataFim, StatusFicha status, boolean vencida, List<DivisaoDTO> divisoes)`
    - `DivisaoDTO(UUID id, String nome, int ordem, List<ItemDivisaoDTO> itens)`
    - `ItemDivisaoDTO(UUID id, UUID exercicioId, String exercicioNome, GrupoMuscular grupoMuscular, int ordem, int series, String repeticoes, BigDecimal cargaKg, Integer descansoSeg, String observacao)`
    - `FichaResumoDTO(UUID id, String nome, StatusFicha status, LocalDate dataInicio, LocalDate dataFim, int frequenciaSemanal, int quantidadeDivisoes, boolean vencida)`
  - `FichaService` (construtor com `FichaRepository`, `AlunoRepository`, `ExercicioRepository`, `UsuarioAtual`, `Clock`):
    - `@Transactional FichaDTO criar(UUID alunoId, FichaRequest req)`:
      1. aluno do personal (`findByIdAndPersonalId`), senão `RecursoNaoEncontradoException("Aluno não encontrado.")`; aluno `INATIVO` → `ConflitoException("Não é possível criar ficha para aluno inativo.")`;
      2. `dataFim` antes de `dataInicio` → `DadosInvalidosException("dataFim", "A data de fim deve ser igual ou posterior à data de início.")`;
      3. cada `exercicioId` precisa ser do personal e ativo; senão `DadosInvalidosException("divisoes[i].itens[j].exercicioId", "Exercício inexistente ou inativo.")` (índices reais);
      4. se houver ficha `ATIVA` do aluno: `encerrar()` e **`fichaRepository.flush()` antes de gravar a nova** (o Hibernate faz INSERT antes de UPDATE no flush e o índice único parcial estouraria);
      5. monta a ficha nova com divisões/itens (ordem = posição + 1; textos com `trim()`; `objetivo`/`observacao` vazios → `null`) e `saveAndFlush`.
    - `@Transactional(readOnly = true) List<FichaResumoDTO> listarDoAluno(UUID alunoId)` — aluno do personal (senão 404 "Aluno não encontrado."); ordem: a `ATIVA` primeiro, depois as encerradas por `dataInicio` desc.
    - `@Transactional(readOnly = true) FichaDTO buscar(UUID id)` — `findByIdAndAlunoPersonalId`; senão `RecursoNaoEncontradoException("Ficha não encontrada.")`.
    - `vencida` calculada com `LocalDate.now(clock)`.
    - método auxiliar package-private `Ficha doPersonal(UUID id)` (usado na Task 3) e a validação de exercícios num método reutilizável.
  - `FichaController` `@RestController @PreAuthorize("hasRole('PERSONAL')")`: `GET /api/alunos/{alunoId}/fichas`, `POST /api/alunos/{alunoId}/fichas` → **201**, `GET /api/fichas/{id}`.

- [ ] **Step 1: Testes que falham** (`FichaControllerIT extends TesteIntegracao`; `@BeforeEach` limpa tudo e grava personal, outro personal, um aluno ATIVO do personal e exercícios ativos/inativos pelo repositório):
```java
@Test void criarFichaCompleta() // POST com 2 divisões ("Treino A" com 2 itens, "Treino B" com 1), cargaKg 22.5, repeticoes "8-12"
    // → 201, status ATIVA, vencida false, divisoes[0].ordem 1, divisoes[0].itens[1].ordem 2, itens[0].exercicioNome, cargaKg 22.5
@Test void criarNovaFichaEncerraAAnterior()      // 2º POST → 201; GET /api/alunos/{id}/fichas → 2 fichas, a nova ATIVA primeiro, a antiga ENCERRADA
@Test void fichaSemDivisaoRetorna400()           // divisoes [] → 400, $.errors.divisoes exists
@Test void divisaoSemItemRetorna400()            // itens [] → 400, $.errors['divisoes[0].itens'] exists
@Test void repeticoesInvalidasRetorna400()       // "dez" → 400, errors['divisoes[0].itens[0].repeticoes'] = "Use um número (ex.: 10) ou uma faixa (ex.: 8-12)."
@Test void exercicioInativoOuDeOutroPersonalRetorna400() // → 400, errors['divisoes[0].itens[0].exercicioId'] = "Exercício inexistente ou inativo."
@Test void dataFimAntesDoInicioRetorna400()      // errors.dataFim = "A data de fim deve ser igual ou posterior à data de início."
@Test void alunoInativoRetorna409()              // detail "Não é possível criar ficha para aluno inativo."
@Test void fichaVencidaQuandoDataFimPassou()     // dataInicio 2020-01-01, dataFim 2020-02-01 → vencida true no POST e no GET
@Test void fichaDeAlunoDeOutroPersonalRetorna404() // GET /api/fichas/{id} → 404 "Ficha não encontrada."; GET/POST /api/alunos/{alunoDeOutro}/fichas → 404 "Aluno não encontrado."
@Test void alunoNaoAcessaFichasPelaApiDoPersonal() // token de aluno ATIVO → GET /api/alunos/{id}/fichas → 403
```
- [ ] **Step 2: Rodar e ver falhar** — `./mvnw -q test -Dtest='FichaControllerIT,TratadorGlobalDeErrosTest'` → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar** — `./mvnw -q verify` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(backend): criar, listar e buscar fichas com divisoes e itens"`

---

### Task 3: Editar (sincronização) e encerrar fichas

**Files:**
- Modify: `backend/src/main/java/com/projetofit/ficha/FichaService.java`, `FichaController.java`
- Test: `backend/src/test/java/com/projetofit/ficha/FichaControllerIT.java`

**Interfaces:**
- Consumes: Task 2 (`doPersonal`, validação de exercícios, DTOs, `DadosInvalidosException`).
- Produces:
  - `@Transactional FichaDTO atualizar(UUID id, FichaRequest req)`:
    - ficha do personal (404 "Ficha não encontrada."); `ENCERRADA` → `ConflitoException("Só é possível editar a ficha ativa.")`; mesma validação de datas;
    - **sincronização** (algoritmo):
      ```
      para cada divisaoReq na posição i:
        se divisaoReq.id != null: d = divisão da própria ficha com esse id
             (não encontrada → DadosInvalidosException("divisoes[i].id", "Divisão não pertence a esta ficha."));
             d.atualizar(nome, i+1)
        senão: d = ficha.adicionarDivisao(nome, i+1)
        para cada itemReq na posição j:
          se itemReq.id != null: it = item da própria divisão d com esse id
               (não encontrado → DadosInvalidosException("divisoes[i].itens[j].id", "Item não pertence a esta divisão."));
               it.atualizar(exercicio, j+1, ...)
          senão: d.adicionarItem(exercicio, j+1, ...)
        d.removerItensExceto(ids de itens mantidos em d)
      ficha.removerDivisoesExceto(ids de divisões mantidas)
      ```
    - exercício: precisa ser do personal; precisa estar **ativo só se for um item novo ou se o exercício do item mudou** (item mantido com o mesmo exercício que ficou inativo continua válido); senão `DadosInvalidosException("divisoes[i].itens[j].exercicioId", "Exercício inexistente ou inativo.")`;
    - termina com `fichaRepository.flush()` dentro do método.
  - `@Transactional FichaDTO encerrar(UUID id)` — `ENCERRADA` → `ConflitoException("A ficha já está encerrada.")`.
  - Controller: `PUT /api/fichas/{id}` → 200; `POST /api/fichas/{id}/encerrar` → 200 com o DTO.

- [ ] **Step 1: Testes que falham** (em `FichaControllerIT`):
```java
@Test void editarAtualizaCriaERemoveDivisoesEItens()
    // ficha com A(itens a1,a2) e B(b1) → PUT com [A(id) com [a2(id) alterado, novo item], C nova] (B ausente)
    // → 200: divisões [A, C] com ordens 1,2; A.itens [a2 (ordem 1, valores novos), novo (ordem 2)]; B e a1 não existem mais (conferir pelo repositório)
@Test void editarMantendoExercicioQueFicouInativoFunciona() // desativa o exercício de a1 no banco → PUT mantendo a1 igual → 200; PUT trocando a1 para outro exercício inativo → 400
@Test void idDeDivisaoDeOutraFichaRetorna400()   // id de divisão de outra ficha (outro aluno) → 400 errors['divisoes[0].id'] = "Divisão não pertence a esta ficha."; a outra ficha continua intacta
@Test void idDeItemDeOutraDivisaoRetorna400()    // errors['divisoes[0].itens[0].id'] = "Item não pertence a esta divisão."
@Test void editarFichaEncerradaRetorna409()      // detail "Só é possível editar a ficha ativa."
@Test void encerrarFicha()                       // POST /encerrar → 200 status ENCERRADA; de novo → 409 "A ficha já está encerrada."
@Test void encerrarFichaDeOutroPersonalRetorna404()
```
- [ ] **Step 2: Rodar e ver falhar** — `./mvnw -q test -Dtest=FichaControllerIT` → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar** — `./mvnw -q verify` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(backend): edicao completa e encerramento de fichas"`

---

### Task 4: Front — modelo e `FichaService`

**Files:**
- Create: `frontend/src/app/personal/fichas/ficha.model.ts`, `ficha.service.ts`
- Test: `frontend/src/app/personal/fichas/ficha.service.spec.ts`

**Interfaces:**
- Produces:
  - `ficha.model.ts`: `type StatusFicha = 'ATIVA' | 'ENCERRADA'`; `interface ItemDivisao { id: string; exercicioId: string; exercicioNome: string; grupoMuscular: GrupoMuscular; ordem: number; series: number; repeticoes: string; cargaKg: number | null; descansoSeg: number | null; observacao: string | null }`; `interface Divisao { id: string; nome: string; ordem: number; itens: ItemDivisao[] }`; `interface Ficha { id: string; alunoId: string; nome: string; objetivo: string | null; frequenciaSemanal: number; dataInicio: string; dataFim: string | null; status: StatusFicha; vencida: boolean; divisoes: Divisao[] }`; `interface FichaResumo { id: string; nome: string; status: StatusFicha; dataInicio: string; dataFim: string | null; frequenciaSemanal: number; quantidadeDivisoes: number; vencida: boolean }`; `interface ItemRequest { id: string | null; exercicioId: string; series: number; repeticoes: string; cargaKg: number | null; descansoSeg: number | null; observacao: string | null }`; `interface DivisaoRequest { id: string | null; nome: string; itens: ItemRequest[] }`; `interface FichaRequest { nome: string; objetivo: string | null; frequenciaSemanal: number; dataInicio: string; dataFim: string | null; divisoes: DivisaoRequest[] }`; `function formatarData(data: string | null): string` (`yyyy-MM-dd` → `dd/MM/yyyy`, `null` → `—`, sem `new Date`); `function hojeLocal(): string` (`yyyy-MM-dd` no fuso do aparelho, sem `toISOString`).
  - `FichaService` (`providedIn: 'root'`): `listarDoAluno(alunoId: string): Promise<FichaResumo[]>` (GET `/api/alunos/{alunoId}/fichas`), `buscar(id: string): Promise<Ficha>` (GET `/api/fichas/{id}`), `criar(alunoId: string, dados: FichaRequest): Promise<Ficha>` (POST `/api/alunos/{alunoId}/fichas`), `atualizar(id: string, dados: FichaRequest): Promise<Ficha>` (PUT `/api/fichas/{id}`), `encerrar(id: string): Promise<Ficha>` (POST `/api/fichas/{id}/encerrar`).

- [ ] **Step 1: Testes que falham** (`HttpTestingController` + `http.verify()`): um teste por método (verbo, URL, corpo); `formatarData('2026-10-05')` → `'05/10/2026'` e `null` → `'—'`; `hojeLocal()` casa com `/^\d{4}-\d{2}-\d{2}$/`.
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar** — `npx ng test --watch=false` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): modelo e servico de fichas"`

---

### Task 5: Front — seção "Fichas" no detalhe do aluno

**Files:**
- Create: `frontend/src/app/personal/fichas/fichas-do-aluno/fichas-do-aluno.ts|html|scss`, `fichas-do-aluno.spec.ts`
- Modify: `frontend/src/app/personal/alunos/detalhe-aluno/detalhe-aluno.html|ts` (inclui `<app-fichas-do-aluno [alunoId]="a.id" [alunoAtivo]="a.status !== 'INATIVO'" />` depois das ações)

**Interfaces:**
- Consumes: Task 4.
- Produces: `FichasDoAluno` com `alunoId = input.required<string>()` e `alunoAtivo = input(true)`. Carrega `listarDoAluno`. Mostra:
  - título `Fichas`; botão `Nova ficha` (só se `alunoAtivo()`) → `/personal/alunos/<alunoId>/fichas/nova`;
  - **ficha ativa** num card: nome, `Início <dd/MM/yyyy>`, `Fim <dd/MM/yyyy>` (se houver), `<n>x por semana`, `<k> divisões` (singular `1 divisão`), etiqueta `Vencida` quando `vencida`; ações `Ver` → `/personal/fichas/<id>` e `Editar` → `/personal/fichas/<id>/editar`;
  - sem ativa: `Nenhuma ficha ativa.`;
  - `Fichas anteriores` (encerradas), cada uma com nome e período, levando a `/personal/fichas/<id>`; some se não houver.
  - Estados `Carregando…` e `Não foi possível carregar as fichas.` como nas listas.

- [ ] **Step 1: Testes que falham**
```ts
it('mostra a ficha ativa com frequência, divisões e ações', ...)  // '3x por semana', '2 divisões', links Ver/Editar
it('mostra Vencida quando a ficha venceu', ...)
it('sem ficha ativa mostra a mensagem', ...)                      // 'Nenhuma ficha ativa.'
it('lista fichas anteriores com link para visualizar', ...)
it('aluno inativo não mostra Nova ficha', ...)
```
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar** e incluir no `DetalheAluno`.
- [ ] **Step 4: Rodar e ver passar** — `npx ng test --watch=false && npx ng build` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): fichas no detalhe do aluno"`

---

### Task 6: Front — seletor de exercício (diálogo)

**Files:**
- Create: `frontend/src/app/personal/fichas/seletor-exercicio/seletor-exercicio.ts|html|scss`, `seletor-exercicio.spec.ts`

**Interfaces:**
- Consumes: `ExercicioService.listar({ ativo: true })`, `Exercicio`, `GRUPOS`, `ROTULO_GRUPO` (Etapa 3), `normalizarBusca` (`shared/texto/normalizar-busca.ts`).
- Produces: `SeletorExercicio` (componente de `MatDialog`) e a função `escolherExercicio(dialog: MatDialog): Promise<Exercicio | null>` no mesmo arquivo. O diálogo carrega **só exercícios ativos** uma vez; título `Escolher exercício`; campo `Buscar exercício` (com `normalizarBusca`, ignora acentos e maiúsculas); chips ou `mat-select` `Grupo muscular` com `Todos`; lista com nome e rótulo do grupo; clicar num exercício fecha devolvendo-o; botão `Cancelar` devolve `null`. Vazio: `Nenhum exercício encontrado.` No celular o diálogo ocupa a tela inteira (`maxWidth: '100vw'`, altura cheia).

- [ ] **Step 1: Testes que falham**
```ts
it('carrega só ativos e lista nome e grupo', ...)                 // listar({ ativo: true })
it('busca ignora acentos', ...)                                    // 'triceps' mostra 'Tríceps testa'
it('filtra por grupo', ...)
it('clicar no exercício fecha devolvendo-o', ...)                  // dialogRef.close(exercicio)
it('escolherExercicio devolve null ao cancelar', ...)
```
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar** — `npx ng test --watch=false` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): seletor de exercicio para fichas"`

---

### Task 7: Front — editor de uma divisão

**Files:**
- Create: `frontend/src/app/personal/fichas/editor-divisao/editor-divisao.ts|html|scss`, `editor-divisao.spec.ts`, `frontend/src/app/personal/fichas/form-ficha.model.ts`

**Interfaces:**
- Consumes: Tasks 4 e 6.
- Produces:
  - `form-ficha.model.ts` (formulários tipados, usados também na Task 8): `type ItemForm = FormGroup<{ id: FormControl<string | null>; exercicioId: FormControl<string>; exercicioNome: FormControl<string>; grupoMuscular: FormControl<GrupoMuscular>; series: FormControl<number | null>; repeticoes: FormControl<string>; cargaKg: FormControl<number | null>; descansoSeg: FormControl<number | null>; observacao: FormControl<string> }>`; `type DivisaoForm = FormGroup<{ id: FormControl<string | null>; nome: FormControl<string>; itens: FormArray<ItemForm> }>`; `function criarItemForm(fb: NonNullableFormBuilder, valor: Partial<ItemDivisao> & { exercicioId: string; exercicioNome: string; grupoMuscular: GrupoMuscular }): ItemForm` com padrões `series 3`, `repeticoes '10'`, `descansoSeg 60`, validadores (séries obrigatório 1..20; repetições obrigatório com `^\d{1,3}(-\d{1,3})?$`; carga ≥ 0; descanso 0..3600; observação máx. 500); `function criarDivisaoForm(fb, valor?: Partial<Divisao>): DivisaoForm` (nome obrigatório, máx. 60; ao menos 1 item — validador no `FormArray`); `function paraDivisaoRequest(form: DivisaoForm): DivisaoRequest` (textos com `trim`, vazios → `null`, números como `number`).
  - `EditorDivisao` com `form = input.required<DivisaoForm>()`, `posicao = input.required<number>()` (para o rótulo), saídas `remover = output<void>()`, `subir = output<void>()`, `descer = output<void>()`. Mostra: campo `Nome da divisão`; botões de ícone `Subir divisão`/`Descer divisão`/`Remover divisão` (`aria-label`s com esses textos); um card por item com o nome e o grupo do exercício, campos `Séries` (`type="number"`), `Repetições` (texto, dica `Ex.: 10 ou 8-12`), `Carga (kg)` (`type="number"`, `inputmode="decimal"`, `step="0.5"`), `Descanso (s)` (`type="number"`), `Observação`; botões `Trocar exercício`, `Subir`, `Descer`, `Remover exercício`; botão `Adicionar exercício` (abre `escolherExercicio`; se escolher, acrescenta um item com os padrões). Erros: `Informe o número de séries (1 a 20).`, `Use um número (ex.: 10) ou uma faixa (ex.: 8-12).`, `Adicione pelo menos um exercício.`

- [ ] **Step 1: Testes que falham**
```ts
it('adicionar exercício acrescenta item com padrões 3 x 10 e 60 s', ...)  // escolherExercicio mockado devolve um exercício
it('cancelar o seletor não acrescenta nada', ...)
it('subir e descer reordenam os itens', ...)
it('remover exercício tira o item', ...)
it('repetições inválidas mostram erro', ...)                             // 'dez'
it('cargaDecimalEhEnviadaComoNumero', ...)                               // digitar 22.5 → paraDivisaoRequest(...).itens[0].cargaKg === 22.5
it('divisão sem exercícios mostra Adicione pelo menos um exercício.', ...)
```
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar** — `npx ng test --watch=false` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): editor de divisao com itens da ficha"`

---

### Task 8: Front — editor de ficha (nova e editar)

**Files:**
- Create: `frontend/src/app/personal/fichas/form-ficha/form-ficha.ts|html|scss`, `form-ficha.spec.ts`
- Modify: `frontend/src/app/personal/personal.routes.ts` (rotas `alunos/:alunoId/fichas/nova` e `fichas/:id/editar`, com `loadComponent`)

**Interfaces:**
- Consumes: Tasks 4, 7 e `confirmar` (`shared/confirmacao/confirmacao-dialog.ts`).
- Produces: `FormFicha` com `alunoId = input<string>()` (modo nova) e `id = input<string>()` (modo editar). Cabeçalho: `Nome da ficha` (obrigatório, máx. 120; erro `Informe o nome da ficha.`), `Objetivo` (máx. 500), `Frequência semanal` (`mat-select` com `1x por semana` … `7x por semana`; padrão 3), `Data de início` (`type="date"`, padrão `hojeLocal()`), `Data de fim` (opcional; erro `A data de fim deve ser igual ou posterior à data de início.`). Divisões: um `EditorDivisao` por divisão; no modo nova começa com uma divisão `Treino A`; botão `Adicionar divisão` cria `Treino B`, `Treino C`… (próxima letra pela quantidade atual; até 10, depois o botão some). Títulos `Nova ficha` / `Editar ficha`; botões `Salvar` e `Cancelar` (volta para o detalhe do aluno — no editar, o `alunoId` vem da ficha carregada).
  - Salvar no modo **nova**: se o aluno já tem ficha `ATIVA` (`listarDoAluno` ao abrir), pede confirmação — título `Substituir ficha ativa`, mensagem `A ficha "<nome da ativa>" será encerrada e a nova passará a ser a ficha ativa.`, botão `Criar e encerrar a atual`; cancelado → não envia.
  - Formulário inválido: marca tudo como tocado e não envia; mostra `Revise os campos destacados.` acima dos botões.
  - Sucesso: snackbar `Ficha criada.` / `Ficha atualizada.` e navega para `/personal/fichas/<id>`. Erro: permanece com o formulário preenchido e reabilita `Salvar`. Sem envio duplo. No editar, campos desabilitados enquanto carrega; falha na carga volta para `/personal/alunos`.

- [ ] **Step 1: Testes que falham**
```ts
it('nova: começa com Treino A e Adicionar divisão cria Treino B', ...)
it('nova sem ficha ativa envia criar com divisões e itens na ordem', ...)
it('nova com ficha ativa pede confirmação e só envia se confirmado', ...)
it('formulário inválido não envia e mostra Revise os campos destacados.', ...)
it('editar carrega a ficha e envia atualizar com os ids existentes', ...)
it('erroAoSalvarMantemNoFormulario', ...)                                 // atualizar rejeita (409) → continua na tela, Salvar habilitado
it('não envia duas vezes enquanto salva', ...)
```
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar** e registrar as rotas.
- [ ] **Step 4: Rodar e ver passar** — `npx ng test --watch=false && npx ng build` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): editor de ficha (nova e editar)"`

---

### Task 9: Front — visualizar e encerrar ficha

**Files:**
- Create: `frontend/src/app/personal/fichas/ver-ficha/ver-ficha.ts|html|scss`, `ver-ficha.spec.ts`
- Modify: `frontend/src/app/personal/personal.routes.ts` (rota `fichas/:id`; a ordem em relação a `fichas/:id/editar` não importa, porque têm quantidades diferentes de segmentos)

**Interfaces:**
- Consumes: Task 4, `confirmar`.
- Produces: `VerFicha` com `id = input.required<string>()`: cabeçalho com nome, etiqueta de status (`Ativa` / `Encerrada`) e `Vencida` quando for o caso, objetivo, período (`Início`/`Fim`), `<n>x por semana`; cada divisão com o nome e seus itens em ordem: exercício, `<séries> x <repetições>`, carga (`22,5 kg`, vírgula decimal; sem carga não mostra), descanso (`60 s`), observação. Ações (só para `ATIVA`): `Editar` → `/personal/fichas/<id>/editar`; `Encerrar` → confirmação (título `Encerrar ficha`, mensagem `A ficha "<nome>" deixará de ser a ficha ativa do aluno.`, botão `Encerrar`) → `encerrar`, snackbar `Ficha encerrada.` e a tela mostra a ficha devolvida. Link `Voltar ao aluno` → `/personal/alunos/<alunoId>`.

- [ ] **Step 1: Testes que falham**
```ts
it('mostra divisões e itens com 3 x 10, 22,5 kg e 60 s', ...)
it('ficha encerrada não mostra Editar nem Encerrar', ...)
it('Encerrar pede confirmação e atualiza a tela', ...)  // confirmar false → encerrar não chamado
it('Voltar ao aluno aponta para o aluno da ficha', ...)
```
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar** e registrar a rota.
- [ ] **Step 4: Rodar e ver passar e verificar no navegador**

Run: `npx ng test --watch=false && npx ng build` → PASS.
Verificação manual (usuário): no detalhe de um aluno, `Nova ficha` → montar Treino A e B com exercícios do catálogo (buscar sem acento), séries/repetições/carga decimal → salvar → tela da ficha; editar removendo um exercício e acrescentando outro; criar outra ficha para o mesmo aluno → confirmação de substituição → a antiga aparece em "Fichas anteriores"; encerrar a ativa.

- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): visualizar e encerrar ficha"`
