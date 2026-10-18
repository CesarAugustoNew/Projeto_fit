# Etapa 3 — Catálogo de exercícios: Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** O personal tem um catálogo de exercícios próprio — já semeado com 44 exercícios básicos — que pode listar, filtrar por grupo muscular, buscar, criar, editar e desativar/reativar.

**Architecture:** Novo módulo `exercicio` no back (tabela com RLS, entidade, repositório, serviço, controller `/api/exercicios`) e a semente em `semente/exercicios.json`. O bootstrap do personal passa a ser um serviço transacional (`BootstrapPersonal`) que avisa ouvintes (`AoCriarPersonal`) na mesma transação; a semente é um desses ouvintes, e um `ApplicationRunner` semeia personais já existentes que ainda não têm exercícios. Para tirar o ciclo de pacotes `usuario` ↔ `aluno`, o resolver passa a depender de interfaces do próprio pacote `usuario`. No front, área `personal/exercicios` com lista e formulário.

**Tech Stack:** Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation, Flyway, Jackson 3), PostgreSQL 17, Testcontainers, JUnit 5, Mockito · Angular 22, Angular Material, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-18-app-personal-trainer-design.md` (seções 5 "exercicio" e "Semente", 7 e 8). Pendências: `docs/superpowers/plans/2026-10-18-etapa2-pendencias.md` (seção "Incluir no plano da Etapa 3").

## Global Constraints
- Tudo das *Global Constraints* das Etapas 1 e 2 continua valendo (pacote `com.projetofit`, Flyway com RLS sem políticas em toda tabela nova, DTOs `record`, Problem Details pt-BR, pt-BR na interface, sem segredos no repositório, toda consulta filtrada pelo personal autenticado, recurso de outro personal → 404).
- `/api/exercicios` é só do papel `PERSONAL` (aluno → 403).
- Exercícios **nunca são apagados**: só `ativo = false` (spec §5). Não existe `DELETE`.
- Nome de exercício: `trim()` antes de gravar; único por personal **sem diferenciar maiúsculas** → 409 `Já existe um exercício com este nome.`
- `grupo_muscular` só aceita: `PEITO, COSTAS, OMBROS, BICEPS, TRICEPS, ANTEBRACO, QUADRICEPS, POSTERIORES, GLUTEOS, PANTURRILHA, ABDOMEN, CARDIO, CORPO_INTEIRO`.
- Rótulos pt-BR dos grupos (front): Peito, Costas, Ombros, Bíceps, Tríceps, Antebraço, Quadríceps, Posteriores de coxa, Glúteos, Panturrilha, Abdômen, Cardio, Corpo inteiro.
- `video_url` opcional; quando informado, precisa começar com `http://` ou `https://`.
- Testes: back `cd backend && ./mvnw -q test -Dtest=<Classe>` (focado) e `./mvnw -q verify` (completo); front `cd frontend && npx ng test --watch=false` e `npx ng build`. Nunca commitar `frontend/src/environments/environment.development.ts`.

## Review Focus
1. **Personal que já existia antes desta etapa** (o do usuário foi criado na Etapa 1): o catálogo não pode ficar vazio. → Task 4, `personalExistenteSemExerciciosRecebeSementeNaInicializacao`.
2. **Nome repetido com outra caixa ou espaços** (`" supino reto com barra"` vs `"Supino reto com barra"`), inclusive ao **renomear** um exercício para o nome de outro: 409, nunca 500. → Task 5, `nomeDuplicadoSemDiferenciarMaiusculasRetorna409` e `renomearParaNomeExistenteRetorna409`.
3. **Link de vídeo perigoso** (`javascript:alert(1)`): 400 na API; o front só abre links `http(s)` em nova aba com `rel="noopener"`. → Task 5, `linkDeVideoSemHttpRetorna400`; Task 7, `linkDoVideoAbreEmNovaAbaComNoopener`.
4. **Dois primeiros acessos simultâneos do personal**: nem usuário nem semente duplicados, sem 500. → Task 3, `falhaNoOuvinteDesfazOUsuario`; Task 4, `segundoAcessoNaoDuplicaSemente`.
5. **Exercício desativado** não some: aparece em "Inativos" e pode ser reativado. → Task 5, `desativarEReativarPeloPut`; Task 7, `inativosChamaListarComAtivoFalse`.

---

### Task 1: Erros pt-BR para 405, 415 e parâmetro obrigatório ausente

**Files:**
- Modify: `backend/src/main/java/com/projetofit/comum/erro/TratadorGlobalDeErros.java`
- Modify (teste): `backend/src/test/java/com/projetofit/comum/erro/ControllerDeTeste.java`, `TratadorGlobalDeErrosTest.java`

**Interfaces:**
- Produces (copiar textos exatamente):

| Exceção (método de `ResponseEntityExceptionHandler`) | status | `title` | `detail` |
|---|---|---|---|
| `HttpRequestMethodNotSupportedException` (`handleHttpRequestMethodNotSupported`) | 405 | `Método não permitido` | `Método <MÉTODO> não é suportado nesta rota.` |
| `HttpMediaTypeNotSupportedException` (`handleHttpMediaTypeNotSupported`) | 415 | `Tipo de conteúdo não suportado` | `Envie o corpo em JSON (application/json).` |
| `MissingServletRequestParameterException` (`handleMissingServletRequestParameter`) | 400 | `Dados inválidos` | `Parâmetro '<nome>' é obrigatório.` |

Manter os headers que o Spring já coloca (ex.: `Allow` no 405) passando `headers` adiante em `handleExceptionInternal`.

- [ ] **Step 1: Testes que falham** (standalone, como os existentes; acrescentar ao `ControllerDeTeste` `GET /teste/obrigatorio` com `@RequestParam String q`):
```java
@Test void metodoNaoSuportadoRetorna405EmPtBr()   // DELETE /teste/validacao → 405, title "Método não permitido", detail "Método DELETE não é suportado nesta rota."
@Test void tipoDeConteudoNaoSuportadoRetorna415() // POST /teste/validacao com text/plain → 415, title "Tipo de conteúdo não suportado"
@Test void parametroAusenteRetorna400()           // GET /teste/obrigatorio → 400, detail "Parâmetro 'q' é obrigatório."
```
- [ ] **Step 2: Rodar e ver falhar** — `./mvnw -q test -Dtest=TratadorGlobalDeErrosTest` → FAIL.
- [ ] **Step 3: Implementar** as três sobrescritas com o helper `problema(...)`.
- [ ] **Step 4: Rodar e ver passar** — mesmo comando → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(backend): erros pt-BR para metodo nao suportado, tipo de conteudo e parametro ausente"`

---

### Task 2: Tabela `exercicio`, entidade e repositório

**Files:**
- Create: `backend/src/main/resources/db/migration/V3__exercicio.sql`
- Create: `backend/src/main/java/com/projetofit/exercicio/GrupoMuscular.java`, `Exercicio.java`, `ExercicioRepository.java`
- Modify (teste): `backend/src/test/java/com/projetofit/MigracoesIT.java`

**Interfaces:**
- Produces:
  - Tabela `exercicio` (spec §5): `id uuid primary key default gen_random_uuid()`, `personal_id uuid not null references usuario(id)`, `nome text not null`, `grupo_muscular text not null check (grupo_muscular in (<os 13 valores>))`, `video_url text`, `observacao text`, `ativo boolean not null default true`, `criado_em`/`atualizado_em timestamptz not null default now()`; `create unique index exercicio_personal_nome_uk on exercicio (personal_id, lower(nome));` RLS.
  - `enum GrupoMuscular` com os 13 valores na ordem das *Global Constraints*.
  - `@Entity Exercicio`: `@Id @GeneratedValue(strategy = GenerationType.UUID) UUID id`; `UUID personalId`; `String nome`; `@Enumerated(STRING) GrupoMuscular grupoMuscular`; `String videoUrl`; `String observacao`; `boolean ativo`; timestamps no padrão de `Aluno`. Construtor `Exercicio(UUID personalId, String nome, GrupoMuscular grupoMuscular, String videoUrl, String observacao)` com `ativo = true`. Métodos: getters, `atualizar(String nome, GrupoMuscular grupoMuscular, String videoUrl, String observacao, boolean ativo)`.
  - `ExercicioRepository extends JpaRepository<Exercicio, UUID>`:
    - `List<Exercicio> findByPersonalIdAndAtivoOrderByNomeAsc(UUID personalId, boolean ativo)`
    - `List<Exercicio> findByPersonalIdAndAtivoAndGrupoMuscularOrderByNomeAsc(UUID personalId, boolean ativo, GrupoMuscular grupoMuscular)`
    - `Optional<Exercicio> findByIdAndPersonalId(UUID id, UUID personalId)`
    - `boolean existsByPersonalIdAndNomeIgnoreCase(UUID personalId, String nome)`
    - `boolean existsByPersonalIdAndNomeIgnoreCaseAndIdNot(UUID personalId, String nome, UUID id)`
    - `long countByPersonalId(UUID personalId)`
  - Limpeza nos testes: com a FK `exercicio → usuario`, testes que apagam `usuario` precisam apagar `exercicio` antes (junto com `aluno`). Ajustar `MeControllerIT`, `MigracoesIT`, `AlunoControllerIT`, `AcessoAlunoIT` (onde houver `deleteAll`/`delete from usuario`).

- [ ] **Step 1: Testes que falham** em `MigracoesIT`:
```java
// o teste geral de RLS já existente passa a exigir também "exercicio" na lista de tabelas: .contains("usuario", "aluno", "exercicio")
@Test void nomeDeExercicioEhUnicoPorPersonalSemDiferenciarMaiusculas() // 2º insert "supino" para o mesmo personal após "Supino" → DataIntegrityViolationException; mesmo nome para outro personal → ok
@Test void grupoMuscularInvalidoEhRecusado()                          // grupo 'PESCOCO' → DataIntegrityViolationException
```
- [ ] **Step 2: Rodar e ver falhar** — `./mvnw -q test -Dtest=MigracoesIT` → FAIL.
- [ ] **Step 3: Implementar** migration, enum, entidade, repositório e as limpezas.
- [ ] **Step 4: Rodar e ver passar** — `./mvnw -q verify` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(backend): tabela exercicio com RLS, entidade e repositorio"`

---

### Task 3: Bootstrap transacional do personal e fim do ciclo `usuario` ↔ `aluno`

**Files:**
- Create: `backend/src/main/java/com/projetofit/usuario/AoCriarPersonal.java`, `AcessoDoAluno.java`, `BootstrapPersonal.java`
- Modify: `backend/src/main/java/com/projetofit/usuario/UsuarioAtualResolver.java` (e Javadoc), `backend/src/main/java/com/projetofit/aluno/AlunoAcesso.java`
- Test: `backend/src/test/java/com/projetofit/usuario/UsuarioAtualResolverTest.java`, `backend/src/test/java/com/projetofit/usuario/BootstrapPersonalIT.java`

**Interfaces:**
- Produces:
  - `interface AoCriarPersonal { void aoCriarPersonal(UUID personalId); }` — chamado dentro da transação que cria o personal.
  - `interface AcessoDoAluno { boolean liberar(UUID usuarioId); }` — `AlunoAcesso implements AcessoDoAluno` (assinatura e comportamento inalterados).
  - `@Service BootstrapPersonal` (construtor `(UsuarioRepository, List<AoCriarPersonal> ouvintes)`): `@Transactional public Usuario criar(UUID id, String nome, String email)` — `saveAndFlush(new Usuario(id, nome, email, PERSONAL))` e depois `ouvinte.aoCriarPersonal(id)` para cada ouvinte; qualquer exceção desfaz tudo.
  - `UsuarioAtualResolver` passa a receber `(UsuarioRepository, AppProperties, AcessoDoAluno, BootstrapPersonal)` e **não importa nada do pacote `aluno`**. O bootstrap chama `bootstrapPersonal.criar(...)` no lugar do `saveAndFlush`; o `catch (DataIntegrityViolationException)` + releitura continua no resolver (fora da transação, que já foi desfeita). Javadoc: a parte "Etapa 3" deixa de ser futura.

- [ ] **Step 1: Testes que falham**

`UsuarioAtualResolverTest` (ajustar construtor; trocar o mock do repositório no bootstrap pelo de `BootstrapPersonal`):
```java
@Test void primeiroAcessoDoPersonalUsaBootstrap()   // findById vazio, e-mail do personal → bootstrapPersonal.criar(sub, "personal", "personal@teste.com") chamado e devolvido
@Test void insercaoConcorrenteRelêOUsuario()        // bootstrapPersonal.criar lança DataIntegrityViolationException; 2º findById devolve o usuário → resultado presente
@Test void personalExistenteNaoPassaPeloAcessoDoAluno() // usuário PERSONAL encontrado → acessoDoAluno.liberar nunca chamado (pendência da Etapa 2)
```
`BootstrapPersonalIT extends TesteIntegracao` com um ouvinte de teste registrado via `@TestConfiguration` (bean `AoCriarPersonal` que grava o id recebido numa lista e, quando um flag estático estiver ligado, lança `IllegalStateException`):
```java
@Test void criarGravaUsuarioEAvisaOuvintes()   // criar(...) → usuário PERSONAL no banco; ouvinte recebeu o id
@Test void falhaNoOuvinteDesfazOUsuario()      // flag ligado → criar lança; usuarioRepository.findById(id) vazio
```
Teste de arquitetura simples em `UsuarioAtualResolverTest` ou numa classe nova: `pacoteUsuarioNaoDependeDeAluno` — lê o fonte `src/main/java/com/projetofit/usuario/*.java` e verifica que nenhum contém `import com.projetofit.aluno`.

- [ ] **Step 2: Rodar e ver falhar** — `./mvnw -q test -Dtest='UsuarioAtualResolverTest,BootstrapPersonalIT'` → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar** — `./mvnw -q verify` (inclui `MeControllerIT` e `AcessoAlunoIT`, que garantem o comportamento antigo) → PASS.
- [ ] **Step 5: Commit** — `git commit -m "refactor(backend): bootstrap do personal transacional com ouvintes e resolver sem depender de aluno"`

---

### Task 4: Semente de exercícios

**Files:**
- Create: `backend/src/main/resources/semente/exercicios.json`
- Create: `backend/src/main/java/com/projetofit/exercicio/SementeExercicios.java`, `SementeParaPersonaisExistentes.java`
- Modify: `backend/src/main/java/com/projetofit/usuario/UsuarioRepository.java` (`List<Usuario> findByPapel(Papel papel)`)
- Test: `backend/src/test/java/com/projetofit/exercicio/SementeExerciciosIT.java`

**Interfaces:**
- Consumes: `AoCriarPersonal` (Task 3), `Exercicio`, `ExercicioRepository.countByPersonalId` (Task 2).
- Produces:
  - `exercicios.json`: array de `{ "nome": ..., "grupoMuscular": ... }` com exatamente estes 44 itens:
    - PEITO: Supino reto com barra; Supino inclinado com halteres; Crucifixo com halteres; Crossover na polia; Flexão de braço
    - COSTAS: Puxada frontal; Remada curvada com barra; Remada baixa na polia; Remada unilateral com halter; Barra fixa
    - OMBROS: Desenvolvimento com halteres; Elevação lateral; Elevação frontal; Crucifixo inverso
    - BICEPS: Rosca direta com barra; Rosca alternada com halteres; Rosca martelo; Rosca Scott
    - TRICEPS: Tríceps na polia com corda; Tríceps testa; Tríceps francês; Mergulho no banco
    - ANTEBRACO: Rosca de punho; Rosca inversa
    - QUADRICEPS: Agachamento livre; Leg press 45°; Cadeira extensora; Afundo com halteres
    - POSTERIORES: Mesa flexora; Stiff com barra; Cadeira flexora
    - GLUTEOS: Elevação pélvica; Glúteo na polia; Cadeira abdutora
    - PANTURRILHA: Panturrilha em pé; Panturrilha sentado
    - ABDOMEN: Abdominal supra; Prancha; Abdominal infra
    - CARDIO: Esteira; Bicicleta ergométrica; Elíptico
    - CORPO_INTEIRO: Burpee; Levantamento terra
  - `@Component SementeExercicios implements AoCriarPersonal`: lê o JSON uma vez (com o `JsonMapper` do Jackson 3 injetado; arquivo via `ClassPathResource("semente/exercicios.json")`); `@Transactional public void semearSeVazio(UUID personalId)` — se `countByPersonalId(personalId) == 0`, grava todos (`saveAll`); `aoCriarPersonal(id)` delega para `semearSeVazio(id)`.
  - `@Component SementeParaPersonaisExistentes implements ApplicationRunner`: para cada `usuarioRepository.findByPapel(PERSONAL)`, chama `semearSeVazio(id)`; falha num personal é logada (`log.error`) e não impede a aplicação de subir.

- [ ] **Step 1: Testes que falham** (`SementeExerciciosIT extends TesteIntegracao`; limpar `exercicio`, `aluno`, `usuario` no `@BeforeEach`):
```java
@Test void primeiroAcessoDoPersonalSemeiaOsExercicios() {
    // GET /api/me com token de "personal@teste.com" (bootstrap)
    // → countByPersonalId == 44; os 13 grupos aparecem; existe "Supino reto com barra" em PEITO; todos ativos
}
@Test void segundoAcessoNaoDuplicaSemente()   // dois GET /api/me → count continua 44
@Test void personalExistenteSemExerciciosRecebeSementeNaInicializacao() // grava Usuario PERSONAL direto; chama runner.run(null) → count 44
@Test void semearSeVazioNaoMexeEmCatalogoExistente() // personal com 1 exercício próprio → semearSeVazio → count continua 1
```
- [ ] **Step 2: Rodar e ver falhar** — `./mvnw -q test -Dtest=SementeExerciciosIT` → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar** — `./mvnw -q verify` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(backend): semente de 44 exercicios no bootstrap e para personais existentes"`

---

### Task 5: API de exercícios

**Files:**
- Create: `backend/src/main/java/com/projetofit/exercicio/ExercicioDTO.java`, `NovoExercicioRequest.java`, `AtualizarExercicioRequest.java`, `ExercicioService.java`, `ExercicioController.java`
- Test: `backend/src/test/java/com/projetofit/exercicio/ExercicioControllerIT.java`

**Interfaces:**
- Consumes: Task 2 (`Exercicio`, `ExercicioRepository`, `GrupoMuscular`); `UsuarioAtual.id()`.
- Produces:
  - `record ExercicioDTO(UUID id, String nome, GrupoMuscular grupoMuscular, String videoUrl, String observacao, boolean ativo)` com `static ExercicioDTO de(Exercicio e)`.
  - `record NovoExercicioRequest(@NotBlank @Size(max = 120) String nome, @NotNull GrupoMuscular grupoMuscular, @Size(max = 500) @Pattern(regexp = "^https?://\\S+$", message = "Informe um link começando com http:// ou https://.") String videoUrl, @Size(max = 1000) String observacao)`.
  - `record AtualizarExercicioRequest(<os mesmos campos e validações>, @NotNull Boolean ativo)`.
  - `ExercicioService`:
    - `@Transactional(readOnly = true) List<ExercicioDTO> listar(Boolean ativo, GrupoMuscular grupo)` — `ativo == null` → `true`; `grupo == null` → todos os grupos; ordem por nome.
    - `@Transactional(readOnly = true) ExercicioDTO buscar(UUID id)` — fora do personal → `RecursoNaoEncontradoException("Exercício não encontrado.")`.
    - `@Transactional ExercicioDTO criar(NovoExercicioRequest req)` — nome com `trim()`; `videoUrl`/`observacao` vazios viram `null`; nome repetido (`existsByPersonalIdAndNomeIgnoreCase`) → `ConflitoException("Já existe um exercício com este nome.")`; `DataIntegrityViolationException` no `saveAndFlush` → mesma `ConflitoException` (com `log.warn` da causa).
    - `@Transactional ExercicioDTO atualizar(UUID id, AtualizarExercicioRequest req)` — mesmas normalizações; nome repetido em outro exercício (`existsByPersonalIdAndNomeIgnoreCaseAndIdNot`) → a mesma `ConflitoException`.
  - `ExercicioController` `@RestController @RequestMapping("/api/exercicios") @PreAuthorize("hasRole('PERSONAL')")`: `GET` (`@RequestParam(required = false) Boolean ativo`, `@RequestParam(required = false) GrupoMuscular grupo`), `GET /{id}`, `POST` → **201**, `PUT /{id}` → 200.

- [ ] **Step 1: Testes que falham** (`ExercicioControllerIT extends TesteIntegracao`; `@BeforeEach` limpa as tabelas e grava o personal e um outro personal; cria exercícios pelo repositório quando precisar — a semente não roda porque o personal é gravado direto):
```java
@Test void criarExercicio()                       // POST {"nome":" Supino reto ","grupoMuscular":"PEITO"} → 201, nome "Supino reto", ativo true, videoUrl null
@Test void nomeDuplicadoSemDiferenciarMaiusculasRetorna409() // depois de "Supino reto", POST "SUPINO RETO" → 409 detail "Já existe um exercício com este nome."
@Test void renomearParaNomeExistenteRetorna409()  // PUT do exercício B com o nome de A → 409; PUT de A com o próprio nome em outra caixa → 200
@Test void linkDeVideoSemHttpRetorna400()         // videoUrl "javascript:alert(1)" → 400, $.errors.videoUrl = "Informe um link começando com http:// ou https://."
@Test void grupoInvalidoRetorna400()              // "grupoMuscular":"PESCOCO" → 400, title "Dados inválidos"
@Test void listarFiltraPorAtivoEGrupoOrdenadoPorNome() // ativos de PEITO e COSTAS + 1 inativo → GET → só ativos por nome; ?grupo=COSTAS → só COSTAS; ?ativo=false → só o inativo
@Test void desativarEReativarPeloPut()            // PUT com "ativo":false → 200 ativo false; some de GET padrão e aparece em ?ativo=false; PUT "ativo":true → volta
@Test void exercicioDeOutroPersonalRetorna404()   // GET/PUT /{id} de outro personal → 404 detail "Exercício não encontrado."; não aparece na lista
@Test void alunoNaoAcessaCatalogo()               // token de aluno ATIVO → GET /api/exercicios → 403
@Test void atualizarSemAtivoRetorna400()          // PUT sem "ativo" → 400, $.errors.ativo exists
```
- [ ] **Step 2: Rodar e ver falhar** — `./mvnw -q test -Dtest=ExercicioControllerIT` → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar** — `./mvnw -q verify` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(backend): API do catalogo de exercicios"`

---

### Task 6: Front — modelo e `ExercicioService`

**Files:**
- Create: `frontend/src/app/personal/exercicios/exercicio.model.ts`, `exercicio.service.ts`
- Test: `frontend/src/app/personal/exercicios/exercicio.service.spec.ts`

**Interfaces:**
- Produces:
  - `exercicio.model.ts`: `type GrupoMuscular = 'PEITO' | 'COSTAS' | 'OMBROS' | 'BICEPS' | 'TRICEPS' | 'ANTEBRACO' | 'QUADRICEPS' | 'POSTERIORES' | 'GLUTEOS' | 'PANTURRILHA' | 'ABDOMEN' | 'CARDIO' | 'CORPO_INTEIRO'`; `const GRUPOS: GrupoMuscular[]` (nessa ordem); `const ROTULO_GRUPO: Record<GrupoMuscular, string>` (rótulos das *Global Constraints*); `interface Exercicio { id: string; nome: string; grupoMuscular: GrupoMuscular; videoUrl: string | null; observacao: string | null; ativo: boolean }`; `interface NovoExercicio { nome: string; grupoMuscular: GrupoMuscular; videoUrl: string | null; observacao: string | null }`; `interface AtualizarExercicio extends NovoExercicio { ativo: boolean }`; `function linkSeguro(url: string | null): string | null` — devolve a URL só se começar com `http://` ou `https://` (sem diferenciar maiúsculas), senão `null`.
  - `ExercicioService` (`providedIn: 'root'`), base `${environment.apiUrl}/api/exercicios`: `listar(filtro: { ativo?: boolean; grupo?: GrupoMuscular } = {}): Promise<Exercicio[]>` (cada query param só quando informado), `buscar(id: string): Promise<Exercicio>`, `criar(dados: NovoExercicio): Promise<Exercicio>`, `atualizar(id: string, dados: AtualizarExercicio): Promise<Exercicio>`.

- [ ] **Step 1: Testes que falham** (`HttpTestingController` + `afterEach(() => http.verify())`):
```ts
it('listar sem filtro não envia query', ...)
it('listar envia ativo=false e grupo=COSTAS', ...)
it('criar faz POST', ...); it('atualizar faz PUT em /{id}', ...); it('buscar faz GET /{id}', ...)
it('linkSeguro aceita http(s) e recusa javascript:', ...)  // 'HTTPS://x.com' ok; 'javascript:alert(1)' → null; null → null
it('ROTULO_GRUPO cobre os 13 grupos', ...)                  // Object.keys(ROTULO_GRUPO).length === 13 e GRUPOS.length === 13
```
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar** — `npx ng test --watch=false` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): modelo e servico de exercicios"`

---

### Task 7: Front — lista de exercícios, navegação e rota

**Files:**
- Create: `frontend/src/app/personal/exercicios/lista-exercicios/lista-exercicios.ts|html|scss`, `lista-exercicios.spec.ts`
- Modify: `frontend/src/app/personal/personal.routes.ts`

**Interfaces:**
- Consumes: Task 6.
- Produces:
  - Item de navegação depois de Alunos: `{ rota: '/personal/exercicios', rotulo: 'Exercícios', icone: 'fitness_center' }`; rota filha `exercicios` → `ListaExercicios` (`loadComponent`). (As rotas `exercicios/novo` e `exercicios/:id/editar` são da Task 8.)
  - `ListaExercicios` (mesmo padrão visual e de estado da `ListaAlunos`: signals, "Carregando…", erro, proteção contra resposta atrasada): título `Exercícios`; botão `Novo exercício` → `/personal/exercicios/novo`; toggle `Ativos` (padrão, `listar({ ativo: true })`) / `Inativos` (`listar({ ativo: false })`); `mat-select` `Grupo muscular` com a opção `Todos` (padrão) e os 13 grupos (`ROTULO_GRUPO`) — mudar o grupo recarrega com `grupo`; campo `Buscar exercício` filtrando no cliente por nome, sem diferenciar maiúsculas. Cada item: nome, rótulo do grupo e, se `linkSeguro(videoUrl)`, um ícone `play_circle` com `aria-label="Ver vídeo"` que abre o link em nova aba (`target="_blank" rel="noopener noreferrer"`) sem navegar para a edição; clicar no item vai para `/personal/exercicios/<id>/editar`. Mensagens vazias: `Nenhum exercício encontrado.` (filtro/busca sem resultado) e `Nenhum exercício inativo.` (Inativos vazio, sem busca).

- [ ] **Step 1: Testes que falham**
```ts
it('carrega ativos ao abrir e mostra nome e grupo', ...)   // listar({ ativo: true }); 'Peito' visível
it('inativosChamaListarComAtivoFalse', ...)
it('trocar o grupo recarrega com grupo', ...)              // seleciona Costas → listar({ ativo: true, grupo: 'COSTAS' })
it('busca filtra por nome sem diferenciar maiúsculas', ...)
it('linkDoVideoAbreEmNovaAbaComNoopener', ...)             // a[target=_blank][rel~=noopener] com href do vídeo; item com 'javascript:...' não mostra o ícone
it('item leva à edição', ...)                              // href '/personal/exercicios/<id>/editar'
```
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar** componente, rota e item de navegação.
- [ ] **Step 4: Rodar e ver passar** — `npx ng test --watch=false && npx ng build` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): lista de exercicios com filtro por grupo e busca"`

---

### Task 8: Front — formulário de exercício (novo e editar)

**Files:**
- Create: `frontend/src/app/personal/exercicios/form-exercicio/form-exercicio.ts|html|scss`, `form-exercicio.spec.ts`
- Modify: `frontend/src/app/personal/personal.routes.ts` (rotas `exercicios/novo` e `exercicios/:id/editar`, `novo` declarada antes)

**Interfaces:**
- Consumes: Task 6.
- Produces: `FormExercicio` com `id = input<string>()` (ausente = novo), no mesmo padrão do `FormAluno` (Reactive Forms, `enviando` com `finally`, sem envio duplo, permanece no formulário em erro). Campos: `Nome` (obrigatório, não pode ser só espaços, máx. 120; erro `Informe o nome.`), `Grupo muscular` (`mat-select` com os 13 `ROTULO_GRUPO`, obrigatório; erro `Escolha o grupo muscular.`), `Link do vídeo` (opcional, máx. 500, `type="url"`, dica `Ex.: link do YouTube`, validado com o mesmo padrão `^https?://\S+$`; erro `Informe um link começando com http:// ou https://.`), `Observação` (textarea, máx. 1000). Só no modo editar: `mat-slide-toggle` `Exercício ativo` (dica quando desligado: `Exercícios inativos não aparecem para novas fichas.`). Títulos `Novo exercício` / `Editar exercício`; botões `Salvar` e `Cancelar` (volta para `/personal/exercicios`). Campos opcionais vazios enviados como `null`. Sucesso: snackbar `Exercício criado.` ou `Exercício atualizado.` e volta para a lista. No editar, campos e `Salvar` desabilitados enquanto o exercício carrega; se a carga falhar, volta para a lista.

- [ ] **Step 1: Testes que falham**
```ts
it('novo: envia criar com opcionais vazios como null e volta à lista', ...)
it('nome só com espaços mostra erro e não envia', ...)        // 'Informe o nome.'
it('link sem http mostra erro e não envia', ...)              // 'javascript:alert(1)'
it('não envia duas vezes enquanto salva', ...)
it('editar: carrega, mostra o toggle e envia atualizar com ativo', ...) // desliga o toggle → atualizar(id, { ..., ativo: false })
it('erro ao salvar mantém no formulário e reabilita o botão', ...)
```
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar** e registrar as rotas.
- [ ] **Step 4: Rodar e ver passar e verificar no navegador**

Run: `npx ng test --watch=false && npx ng build` → PASS.
Verificação manual (usuário): ao subir a API, o personal existente recebe os 44 exercícios; menu **Exercícios** lista os ativos por nome; filtrar por grupo e buscar; criar um exercício com link do YouTube e abrir o vídeo; tentar repetir um nome com outra caixa → mensagem de nome repetido; desativar pelo toggle → some dos Ativos e aparece em Inativos; reativar.

- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): formulario de exercicio (novo e editar)"`
