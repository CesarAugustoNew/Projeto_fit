# Etapa 2 — Alunos e convite: Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** O personal cadastra alunos (convite por e-mail via Supabase), lista, vê, edita, desativa/reativa e reenvia convite; o aluno convidado define a senha e entra; aluno inativo perde o acesso.

**Architecture:** Novo módulo `aluno` no back (entidade, repositório, serviço, controller `/api/alunos`, componente `AlunoAcesso` usado pelo `UsuarioAtualResolver`) e um cliente `SupabaseAdmin` (interface + implementação com `RestClient`) para o convite. A chamada externa fica fora da transação do banco, com compensação (exclusão do usuário no Supabase) se a gravação falhar. No front, área `personal/alunos` com lista, formulário (novo/editar) e detalhe, mais o tratamento de 403 e a tela "Servidor indisponível".

**Tech Stack:** Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation, Flyway, `RestClient`), PostgreSQL 17, Testcontainers, JUnit 5, Mockito, `MockRestServiceServer` · Angular 22, Angular Material, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-18-app-personal-trainer-design.md` (seções 5, 6 "Alunos e convite", 7 e 8). Pendências herdadas: `docs/superpowers/plans/2026-10-18-etapa1-pendencias.md` (seção "Incluir no plano da Etapa 2").

## Pré-requisitos (usuário)
- Anotar a **Secret key** (`sb_secret_...`) ou a **service_role** do projeto Supabase de dev (*Project Settings → API Keys*). Ela vai só na variável `SUPABASE_SERVICE_ROLE_KEY` da API, nunca no front nem no repositório.
- O SMTP padrão do Supabase só entrega e-mail para membros da equipe do projeto e com limite baixo. Para o teste manual, convide um e-mail seu (membro do projeto). Convidar alunos reais exige SMTP próprio (Resend), configurado na Etapa 7.

## Global Constraints
- Tudo de `docs/superpowers/plans/2026-10-18-etapa1-fundacao.md` → *Global Constraints* continua valendo (pacote `com.projetofit`, Flyway com RLS sem políticas em toda tabela nova, DTOs `record`, Problem Details pt-BR, pt-BR na interface, sem segredos no repositório).
- Nova variável obrigatória do back: `SUPABASE_SERVICE_ROLE_KEY` (propriedade `app.supabase-service-role-key`). A API não sobe sem ela.
- Toda consulta de aluno filtra por `personal_id = usuarioAtual.id()`; aluno de outro personal → **404** `Aluno não encontrado.`
- Endpoints de `/api/alunos` são só do papel `PERSONAL` (aluno → 403).
- E-mail de aluno sempre normalizado: `trim()` + minúsculas (`Locale.ROOT`). O e-mail não é editável.
- Datas `LocalDate` trafegam como `yyyy-MM-dd`; no front, `<input matInput type="date">` (sem datepicker).
- Testes: back `cd backend && ./mvnw -q test -Dtest=<Classe>` (focado) e `./mvnw -q verify` (completo); front `cd frontend && npx ng test --watch=false` e `npx ng build`.

## Review Focus
1. **E-mail com maiúsculas/espaços** (`" Ana@Teste.com "`): deve ser salvo como `ana@teste.com` e um segundo cadastro com outra caixa deve dar 409 **antes** de chamar o Supabase. → Task 4, `criarNormalizaEmailEDetectaDuplicadoSemChamarSupabase`.
2. **Gravação no banco falha depois do convite enviado**: sem compensação, o e-mail fica preso no Supabase e nunca mais pode ser convidado. → Task 4, `falhaAoGravarExcluiUsuarioNoSupabase`.
3. **Aluno inativo com token válido**: 403 em tudo, inclusive `/api/me`; reativado, volta a entrar. → Task 5, `alunoInativoRecebe403EReativadoVoltaAEntrar`.
4. **Duplo clique em "Salvar" no novo aluno**: não pode disparar dois convites. → Task 9, `naoEnviaDuasVezesEnquantoSalva`.
5. **Parâmetro inválido** (`?status=XYZ`, id que não é UUID): 400 em pt-BR, não 500 nem título em inglês. → Task 1 e Task 4, `statusInvalidoRetorna400`.

---

### Task 1: Erros pt-BR para as exceções restantes do Spring MVC

**Files:**
- Modify: `backend/src/main/java/com/projetofit/comum/erro/TratadorGlobalDeErros.java`
- Modify (teste): `backend/src/test/java/com/projetofit/comum/erro/ControllerDeTeste.java`, `TratadorGlobalDeErrosTest.java`

**Interfaces:**
- Produces: sobrescritas em `TratadorGlobalDeErros` (copiar textos exatamente):

| Exceção (método de `ResponseEntityExceptionHandler`) | status | `title` | `detail` |
|---|---|---|---|
| `HandlerMethodValidationException` (`handleHandlerMethodValidationException`) | 400 | `Dados inválidos` | `Um ou mais campos são inválidos.` + `errors: {parâmetro: mensagem}` |
| `HttpMessageNotReadableException` (`handleHttpMessageNotReadable`) | 400 | `Dados inválidos` | `Corpo da requisição inválido.` |
| `TypeMismatchException` (`handleTypeMismatch`) | 400 | `Dados inválidos` | `Parâmetro '<nome>' inválido.` (nome do parâmetro/variável de path) |
| `NoResourceFoundException` (`handleNoResourceFoundException`) | 404 | `Recurso não encontrado` | `Rota não encontrada.` |

- [ ] **Step 1: Testes que falham.** Em `TratadorGlobalDeErrosTest` (MockMvc standalone, como os existentes), acrescentando ao `ControllerDeTeste`: `GET /teste/numero/{n}` com `@PathVariable int n`; `GET /teste/param` com `@RequestParam @Min(1) int qtd` (a validação de método é nativa no Spring 7, sem `@Validated`); `POST /teste/validacao` já existe.

```java
@Test void corpoMalformadoRetorna400() // POST /teste/validacao com "{nome:" → 400, title "Dados inválidos", detail "Corpo da requisição inválido."
@Test void tipoInvalidoRetorna400()    // GET /teste/numero/abc → 400, detail "Parâmetro 'n' inválido."
@Test void validacaoDeParametroRetorna400ComErrors() // GET /teste/param?qtd=0 → 400, title "Dados inválidos", $.errors.qtd exists
```
E em `MeControllerIT` (contexto completo, onde o 404 de rota existe de verdade):
```java
@Test void rotaInexistenteRetorna404EmPtBr() // token de personal → GET /api/nao-existe → 404, title "Recurso não encontrado", detail "Rota não encontrada."
```

- [ ] **Step 2: Rodar e ver falhar** — `cd backend && ./mvnw -q test -Dtest='TratadorGlobalDeErrosTest,MeControllerIT'` → FAIL (títulos em inglês).
- [ ] **Step 3: Implementar** as quatro sobrescritas reutilizando o helper `problema(...)` existente.
- [ ] **Step 4: Rodar e ver passar** — mesmo comando → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(backend): erros pt-BR para validacao de parametros, corpo invalido, tipo invalido e rota inexistente"`

---

### Task 2: Migration `aluno`, entidade e repositório

**Files:**
- Create: `backend/src/main/resources/db/migration/V2__aluno.sql`
- Create: `backend/src/main/java/com/projetofit/aluno/StatusAluno.java`, `Aluno.java`, `AlunoRepository.java`
- Modify: `backend/src/main/java/com/projetofit/usuario/Usuario.java` (método `alterarNome`)
- Modify (teste): `backend/src/test/java/com/projetofit/MigracoesIT.java`, `backend/src/test/java/com/projetofit/usuario/MeControllerIT.java` (limpeza)

**Interfaces:**
- Produces:
  - **Limpeza nos testes:** com a FK `aluno → usuario`, todo teste que apaga `usuario` precisa apagar `aluno` antes (o banco do Testcontainers é compartilhado entre classes). Ajustar `MeControllerIT.limpar()` e `MigracoesIT.emailEhUnicoSemDiferenciarMaiusculas` para executar `delete from aluno` primeiro (via `JdbcTemplate` ou `AlunoRepository.deleteAll()`); as classes novas desta etapa seguem o mesmo padrão.
  - Tabela `aluno` exatamente como na seção 5 da spec (`id uuid primary key`, `usuario_id uuid not null unique references usuario(id)`, `personal_id uuid not null references usuario(id)`, `telefone text`, `data_nascimento date`, `objetivo text`, `observacoes text`, `status text not null check (status in ('CONVIDADO','ATIVO','INATIVO'))`, `criado_em`/`atualizado_em timestamptz not null default now()`), índice `aluno_personal_id_idx on aluno (personal_id)`, RLS.
  - `enum StatusAluno { CONVIDADO, ATIVO, INATIVO }`.
  - `@Entity Aluno`: `@Id @GeneratedValue(strategy = GenerationType.UUID) UUID id`; `@OneToOne(fetch = LAZY, optional = false) @JoinColumn(name = "usuario_id") Usuario usuario`; `UUID personalId`; `String telefone`, `LocalDate dataNascimento`, `String objetivo`, `String observacoes`; `@Enumerated(STRING) StatusAluno status`; `Instant criadoEm`, `atualizadoEm` (mesmo padrão de `Usuario`: `@PreUpdate`). Construtor `Aluno(Usuario usuario, UUID personalId, String telefone, LocalDate dataNascimento, String objetivo, String observacoes)` com status `CONVIDADO`. Métodos: getters, `atualizarDados(String telefone, LocalDate dataNascimento, String objetivo, String observacoes)`, `alterarStatus(StatusAluno status)`.
  - `AlunoRepository extends JpaRepository<Aluno, UUID>`:
    - `@EntityGraph(attributePaths = "usuario") List<Aluno> findByPersonalIdAndStatusInOrderByUsuarioNomeAsc(UUID personalId, Collection<StatusAluno> status)`
    - `@EntityGraph(attributePaths = "usuario") Optional<Aluno> findByIdAndPersonalId(UUID id, UUID personalId)`
    - `Optional<Aluno> findByUsuarioId(UUID usuarioId)`
  - `Usuario.alterarNome(String nome)`.

- [ ] **Step 1: Testes que falham** em `MigracoesIT` (substituir `tabelaUsuarioExisteComRlsAtivo` pela versão geral):

```java
@Test void todaTabelaPublicTemRlsAtivoESemPoliticas() {
    List<String> semRls = jdbc.queryForList(
        "select c.relname from pg_class c join pg_namespace n on n.oid = c.relnamespace " +
        "where n.nspname = 'public' and c.relkind = 'r' and c.relname <> 'flyway_schema_history' " +
        "and not c.relrowsecurity", String.class);
    assertThat(semRls).isEmpty();
    Integer politicas = jdbc.queryForObject("select count(*) from pg_policies where schemaname = 'public'", Integer.class);
    assertThat(politicas).isZero();
    assertThat(jdbc.queryForList("select tablename from pg_tables where schemaname = 'public'", String.class))
        .contains("usuario", "aluno");
}
@Test void statusDeAlunoInvalidoEhRecusado() // insert com status 'BLOQUEADO' → DataIntegrityViolationException
```
`flyway_schema_history` fica fora da checagem porque o Flyway a cria sem RLS e ela não é exposta (o Spring conecta como dono); se preferir, habilite RLS nela também numa migration e remova a exclusão.

- [ ] **Step 2: Rodar e ver falhar** — `./mvnw -q test -Dtest=MigracoesIT` → FAIL (`aluno` inexistente).
- [ ] **Step 3: Implementar** migration, enum, entidade, repositório e `alterarNome`.
- [ ] **Step 4: Rodar e ver passar** — `./mvnw -q verify` (o `ddl-auto=validate` confere o mapeamento da entidade) → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(backend): tabela aluno com RLS, entidade e repositorio"`

---

### Task 3: Cliente `SupabaseAdmin` (convite e exclusão)

**Files:**
- Modify: `backend/src/main/java/com/projetofit/comum/AppProperties.java`, `backend/src/main/resources/application.yml`, `backend/src/test/resources/application-test.yml`
- Modify (teste, novo parâmetro do record): `backend/src/test/java/com/projetofit/comum/seguranca/SupabaseJwtConfigTest.java`, `backend/src/test/java/com/projetofit/usuario/UsuarioAtualResolverTest.java`
- Create: `backend/src/main/java/com/projetofit/comum/supabase/SupabaseAdmin.java`, `SupabaseAdminClient.java`
- Modify: `README.md` (variável agora obrigatória; aviso do SMTP padrão)
- Test: `backend/src/test/java/com/projetofit/comum/supabase/SupabaseAdminClientTest.java`

**Interfaces:**
- Produces:
  - `AppProperties(String supabaseUrl, String personalEmail, String frontUrl, String supabaseServiceRoleKey)`; `application.yml`: `supabase-service-role-key: ${SUPABASE_SERVICE_ROLE_KEY}`; `application-test.yml`: `supabase-service-role-key: chave-de-teste`.
  - `interface SupabaseAdmin { UUID convidar(String email, String nome); void excluirUsuario(UUID id); }`
  - `@Component SupabaseAdminClient implements SupabaseAdmin`, construtor `(RestClient.Builder builder, AppProperties props)`:
    - lança `IllegalStateException("app.supabase-service-role-key (SUPABASE_SERVICE_ROLE_KEY) não configurado")` se a chave estiver vazia;
    - base `{supabaseUrl sem barra final}/auth/v1`; headers padrão `apikey: <chave>` e `Authorization: Bearer <chave>`; timeouts: conexão 5 s, leitura 10 s (`JdkClientHttpRequestFactory` com `HttpClient.newBuilder().connectTimeout(...)` e `setReadTimeout(...)`);
    - `convidar`: `POST /invite?redirect_to={frontUrl}/definir-senha` com corpo `{"email": email, "data": {"nome": nome}}`; devolve o `id` (UUID) do JSON de resposta. Resposta **422** → `ConflitoException("E-mail já cadastrado.")`; qualquer outro erro HTTP ou de I/O → `IntegracaoExternaException("Não foi possível enviar o convite agora. Tente novamente.", causa)`;
    - `excluirUsuario`: `DELETE /admin/users/{id}`; erro → `IntegracaoExternaException("Não foi possível excluir o usuário no Supabase.", causa)`.

- [ ] **Step 1: Testes que falham** — `MockRestServiceServer.bindTo(builder).build()` antes de construir o cliente:

```java
@Test void convidarEnviaPostComChaveERedirectEDevolveId() {
    servidor.expect(requestTo("https://teste.supabase.co/auth/v1/invite?redirect_to=http://localhost:4200/definir-senha"))
        .andExpect(method(POST))
        .andExpect(header("apikey", "chave-de-teste"))
        .andExpect(header("Authorization", "Bearer chave-de-teste"))
        .andExpect(jsonPath("$.email").value("ana@teste.com"))
        .andExpect(jsonPath("$.data.nome").value("Ana"))
        .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"email\":\"ana@teste.com\"}", APPLICATION_JSON));
    assertThat(cliente.convidar("ana@teste.com", "Ana")).isEqualTo(ID);
}
@Test void convidarEmailExistenteViraConflito()   // 422 → ConflitoException, mensagem "E-mail já cadastrado."
@Test void convidarErroDoSupabaseViraIntegracaoExterna() // 500 → IntegracaoExternaException, mensagem "Não foi possível enviar o convite agora. Tente novamente."
@Test void excluirUsuarioEnviaDelete()             // DELETE https://teste.supabase.co/auth/v1/admin/users/{ID} → 200
@Test void semChaveNaoSobe()                       // AppProperties com chave "" → IllegalStateException
```
E no `TratadorGlobalDeErrosTest`, o teste de 502 passa a verificar `$.detail` (pendência da Etapa 1).

- [ ] **Step 2: Rodar e ver falhar** — `./mvnw -q test -Dtest='SupabaseAdminClientTest,TratadorGlobalDeErrosTest'` → FAIL.
- [ ] **Step 3: Implementar** e atualizar os construtores de `AppProperties` nos testes existentes. README: na tabela, `SUPABASE_SERVICE_ROLE_KEY` deixa de dizer "só a partir da Etapa 2" e passa a "obrigatória"; nos comandos de execução local, acrescentar a variável; uma linha sobre o SMTP padrão só entregar para membros do projeto.
- [ ] **Step 4: Rodar e ver passar** — `./mvnw -q verify` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(backend): cliente Supabase Admin para convite e exclusao de usuario"`

---

### Task 4: Cadastro, listagem e detalhe de alunos (`POST`/`GET`)

**Files:**
- Create: `backend/src/main/java/com/projetofit/aluno/AlunoDTO.java`, `NovoAlunoRequest.java`, `AlunoService.java`, `AlunoController.java`
- Test: `backend/src/test/java/com/projetofit/aluno/AlunoControllerIT.java`, `AlunoServiceTest.java`

**Interfaces:**
- Consumes: `Aluno`, `AlunoRepository`, `StatusAluno`, `Usuario.alterarNome` (Task 2); `SupabaseAdmin` (Task 3); `UsuarioAtual.id()`, `UsuarioRepository.findByEmailIgnoreCase` (Etapa 1).
- Produces:
  - `record AlunoDTO(UUID id, String nome, String email, String telefone, LocalDate dataNascimento, String objetivo, String observacoes, StatusAluno status, Instant criadoEm)` com `static AlunoDTO de(Aluno aluno)`.
  - `record NovoAlunoRequest(@NotBlank @Size(max = 120) String nome, @NotBlank @Email @Size(max = 254) String email, @Size(max = 20) String telefone, @Past LocalDate dataNascimento, @Size(max = 500) String objetivo, @Size(max = 2000) String observacoes)`.
  - `AlunoService` (construtor com `AlunoRepository`, `UsuarioRepository`, `SupabaseAdmin`, `UsuarioAtual`, `TransactionTemplate`):
    - `AlunoDTO criar(NovoAlunoRequest req)` — **sem** `@Transactional`: normaliza o e-mail; se `findByEmailIgnoreCase` achar → `ConflitoException("E-mail já cadastrado.")` sem chamar o Supabase; `UUID id = supabaseAdmin.convidar(email, nome.trim())`; grava `Usuario(id, nome, email, ALUNO)` + `Aluno(...)` dentro de `transactionTemplate.execute(...)`; se a gravação lançar qualquer `RuntimeException`, chama `supabaseAdmin.excluirUsuario(id)` (falha da exclusão só é logada com `log.error`) e relança — `DataIntegrityViolationException` é relançada como `ConflitoException("E-mail já cadastrado.")`.
    - `@Transactional(readOnly = true) List<AlunoDTO> listar(StatusAluno status)` — `status == null` → `{CONVIDADO, ATIVO}`; senão só o informado.
    - `@Transactional(readOnly = true) AlunoDTO buscar(UUID id)` — `findByIdAndPersonalId`; vazio → `RecursoNaoEncontradoException("Aluno não encontrado.")`.
    - método auxiliar package-private `Aluno doPersonal(UUID id)` reutilizado na Task 5.
  - `AlunoController` `@RestController @RequestMapping("/api/alunos") @PreAuthorize("hasRole('PERSONAL')")`: `POST` → **201** com o DTO; `GET` com `@RequestParam(required = false) StatusAluno status`; `GET /{id}`.

- [ ] **Step 1: Testes que falham**

`AlunoControllerIT extends TesteIntegracao`, com `@MockitoBean SupabaseAdmin supabaseAdmin`; `@BeforeEach` apaga `aluno` e `usuario`, cria o personal (`Usuario(PERSONAL_ID, "personal", "personal@teste.com", PERSONAL)`) e configura `when(supabaseAdmin.convidar(any(), any())).thenAnswer(i -> UUID.randomUUID())`:
```java
@Test void personalCriaAlunoConvidado() throws Exception {
    mvc.perform(post("/api/alunos").header("Authorization", bearerPersonal())
            .contentType(APPLICATION_JSON).content("{\"nome\":\"Ana\",\"email\":\"ana@teste.com\",\"dataNascimento\":\"2000-05-10\"}"))
       .andExpect(status().isCreated())
       .andExpect(jsonPath("$.nome").value("Ana"))
       .andExpect(jsonPath("$.status").value("CONVIDADO"))
       .andExpect(jsonPath("$.dataNascimento").value("2000-05-10"));
    verify(supabaseAdmin).convidar("ana@teste.com", "Ana");
}
@Test void criarNormalizaEmailEDetectaDuplicadoSemChamarSupabase() // 1º " Ana@Teste.com " → 201 com email "ana@teste.com"; 2º "ANA@teste.com" → 409 detail "E-mail já cadastrado."; convidar chamado 1x
@Test void criarSemNomeRetorna400ComErrors()   // {"email":"x@teste.com"} → 400, $.errors.nome exists; convidar nunca chamado
@Test void conviteRecusadoPeloSupabaseNaoGravaNada() // convidar lança IntegracaoExternaException → 502; alunoRepository.count() == 0
@Test void listarTrazConvidadosEAtivosOrdenadosPorNome() // cria "Bruno" e "Ana"; marca um terceiro como INATIVO via repositório → GET /api/alunos → ["Ana","Bruno"]; ?status=INATIVO → só o inativo
@Test void statusInvalidoRetorna400()          // GET /api/alunos?status=XYZ → 400, detail "Parâmetro 'status' inválido."
@Test void buscarAlunoInexistenteRetorna404()  // GET /api/alunos/{uuid aleatório} → 404, detail "Aluno não encontrado."
@Test void alunoNaoAcessaEndpointsDoPersonal() // token de um usuário ALUNO (aluno ATIVO) → GET /api/alunos → 403
```
`AlunoServiceTest` (Mockito, sem Spring; `TransactionTemplate` real sobre um `PlatformTransactionManager` mock, ou um `TransactionTemplate` mock que executa o callback):
```java
@Test void falhaAoGravarExcluiUsuarioNoSupabase() {
    // convidar → ID; usuarioRepository.saveAndFlush lança DataIntegrityViolationException
    assertThatThrownBy(() -> service.criar(req)).isInstanceOf(ConflitoException.class).hasMessage("E-mail já cadastrado.");
    verify(supabaseAdmin).excluirUsuario(ID);
}
@Test void falhaNaExclusaoCompensatoriaNaoEscondeOErroOriginal() // excluirUsuario também lança → ainda ConflitoException
```

- [ ] **Step 2: Rodar e ver falhar** — `./mvnw -q test -Dtest='AlunoControllerIT,AlunoServiceTest'` → FAIL.
- [ ] **Step 3: Implementar** DTOs, serviço e controller conforme Interfaces.
- [ ] **Step 4: Rodar e ver passar** — `./mvnw -q verify` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(backend): cadastro com convite, listagem e detalhe de alunos"`

---

### Task 5: Edição, status, reenvio de convite e acesso do aluno

**Files:**
- Create: `backend/src/main/java/com/projetofit/aluno/AtualizarAlunoRequest.java`, `AlterarStatusRequest.java`, `AlunoAcesso.java`
- Modify: `AlunoService.java`, `AlunoController.java`, `backend/src/main/java/com/projetofit/usuario/UsuarioAtualResolver.java` (e seu Javadoc de ponto de extensão)
- Test: `AlunoControllerIT.java`, `backend/src/test/java/com/projetofit/aluno/AcessoAlunoIT.java`, `backend/src/test/java/com/projetofit/usuario/UsuarioAtualResolverTest.java`

**Interfaces:**
- Consumes: Task 4 (`AlunoService.doPersonal`, `AlunoDTO`), `SupabaseAdmin.convidar`.
- Produces:
  - `record AtualizarAlunoRequest(@NotBlank @Size(max = 120) String nome, @Size(max = 20) String telefone, @Past LocalDate dataNascimento, @Size(max = 500) String objetivo, @Size(max = 2000) String observacoes)`.
  - `record AlterarStatusRequest(@NotNull StatusAluno status)`.
  - `AlunoService`:
    - `@Transactional AlunoDTO atualizar(UUID id, AtualizarAlunoRequest req)` — altera nome (`Usuario.alterarNome`) e dados; e-mail intocado.
    - `@Transactional AlunoDTO alterarStatus(UUID id, StatusAluno novo)` — regras: `novo == CONVIDADO` → `ConflitoException("Status inválido.")`; `INATIVO` a partir de qualquer status; `ATIVO` a partir de `INATIVO` ou `ATIVO` (idempotente); `ATIVO` a partir de `CONVIDADO` → `ConflitoException("O aluno ainda não aceitou o convite.")`.
    - `void reenviarConvite(UUID id)` — status diferente de `CONVIDADO` → `ConflitoException("Só é possível reenviar o convite de um aluno com convite pendente.")`; senão `supabaseAdmin.convidar(email, nome)` (o id devolvido é ignorado).
  - `AlunoController`: `PUT /{id}` → 200 DTO; `PATCH /{id}/status` → 200 DTO; `POST /{id}/reenviar-convite` → **204**.
  - `@Component AlunoAcesso`: `@Transactional boolean liberar(UUID usuarioId)` — sem aluno → `false`; `INATIVO` → `false`; `CONVIDADO` → muda para `ATIVO` e devolve `true`; `ATIVO` → `true`.
  - `UsuarioAtualResolver` recebe `AlunoAcesso` no construtor: usuário encontrado com papel `ALUNO` e `!alunoAcesso.liberar(id)` → `Optional.empty()` (vira 403). Atualizar o Javadoc (a parte da Etapa 2 deixa de ser futura).

- [ ] **Step 1: Testes que falham**

Em `AlunoControllerIT`:
```java
@Test void atualizarAlteraNomeEDadosMasNaoEmail()   // PUT com nome "Ana Souza", telefone "11999990000" → 200; email continua "ana@teste.com"
@Test void desativarEReativar()                      // aluno ATIVO → PATCH {"status":"INATIVO"} → 200 INATIVO → PATCH {"status":"ATIVO"} → 200 ATIVO
@Test void ativarConvidadoRetorna409()               // aluno CONVIDADO → PATCH {"status":"ATIVO"} → 409 detail "O aluno ainda não aceitou o convite."
@Test void statusConvidadoNoPatchRetorna409()        // PATCH {"status":"CONVIDADO"} → 409 detail "Status inválido."
@Test void reenviarConviteSoParaConvidado()          // CONVIDADO → 204 e convidar chamado de novo; ATIVO → 409 detail "Só é possível reenviar o convite de um aluno com convite pendente."
@Test void editarAlunoDeOutroPersonalRetorna404()    // aluno com personal_id aleatório gravado direto no repositório → PUT → 404
```
`AcessoAlunoIT extends TesteIntegracao` (sem mock do `SupabaseAdmin`; grava `Usuario` ALUNO + `Aluno` direto pelos repositórios):
```java
@Test void primeiroAcessoDoConvidadoAtiva()            // CONVIDADO → GET /api/me com token do aluno → 200 papel ALUNO; status no banco = ATIVO
@Test void alunoInativoRecebe403EReativadoVoltaAEntrar() // INATIVO → /api/me 403 title "Acesso negado"; muda para ATIVO no banco → /api/me 200
```
Em `UsuarioAtualResolverTest`: `alunoInativoNaoTemPapel` (usuário ALUNO encontrado, `alunoAcesso.liberar` → false → `Optional.empty()`), e ajustar o construtor nos testes existentes.

- [ ] **Step 2: Rodar e ver falhar** — `./mvnw -q test -Dtest='AlunoControllerIT,AcessoAlunoIT,UsuarioAtualResolverTest'` → FAIL.
- [ ] **Step 3: Implementar** conforme Interfaces.
- [ ] **Step 4: Rodar e ver passar** — `./mvnw -q verify` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(backend): edicao, status e reenvio de convite de aluno; acesso do aluno por status"`

---

### Task 6: Front — 403 no meio da sessão e tela "Servidor indisponível"

**Files:**
- Modify: `frontend/src/app/core/http/erro.interceptor.ts`, `erro.interceptor.spec.ts`
- Modify: `frontend/src/app/core/guards/papel.guard.ts`, `papel.guard.spec.ts`
- Create: `frontend/src/app/publico/indisponivel/indisponivel.ts|html|scss`, `indisponivel.spec.ts`
- Modify: `frontend/src/app/app.routes.ts` (rota pública `indisponivel` com `loadComponent`)

**Interfaces:**
- Produces:
  - `erroInterceptor`, para **403**: se o caminho atual (sem query string) não for `/login`: `auth.logout()`, `usuario.limpar()`, `navigateByUrl('/login')` e snackbar `Sua conta não tem acesso ao app.`; em `/login` não faz nada (a tela de login já mostra a mensagem). Demais status inalterados.
  - `papelGuard`: em status 0 ou 5xx, em vez de `false`, devolve `router.createUrlTree(['/indisponivel'], { queryParams: { voltar: state.url } })` (o guard passa a usar `(route, state)`).
  - `Indisponivel` (standalone): título `Servidor indisponível`; texto `Não foi possível conectar ao servidor. Verifique sua internet e tente de novo.`; botão `Tentar de novo` → `navigateByUrl(voltar)`, onde `voltar` vem do query param e só é aceito se começar com `/` e não com `//` (senão usa `/login`). Mesmo layout de card das telas públicas.

- [ ] **Step 1: Testes que falham**

```ts
// erro.interceptor.spec.ts
it('403 fora do login desloga, limpa, vai ao login e avisa', ...)   // open('Sua conta não tem acesso ao app.', 'Fechar', ...)
it('403 no /login não desloga nem navega', ...)
// papel.guard.spec.ts
it('500 vai para /indisponivel com voltar', ...)  // state.url '/personal/alunos' → '/indisponivel?voltar=%2Fpersonal%2Falunos'; logout não chamado
it('status 0 vai para /indisponivel', ...)
// indisponivel.spec.ts
it('Tentar de novo volta para a URL pedida', ...)       // ?voltar=/personal/alunos → navigateByUrl('/personal/alunos')
it('voltar externo é ignorado', ...)                    // ?voltar=//evil.com → navigateByUrl('/login')
```

- [ ] **Step 2: Rodar e ver falhar** — `cd frontend && npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar** — `npx ng test --watch=false && npx ng build` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): trata 403 no meio da sessao e tela de servidor indisponivel"`

---

### Task 7: Front — modelo e `AlunoService`

**Files:**
- Create: `frontend/src/app/personal/alunos/aluno.model.ts`, `aluno.service.ts`
- Test: `frontend/src/app/personal/alunos/aluno.service.spec.ts`

**Interfaces:**
- Produces:
  - `aluno.model.ts`: `type StatusAluno = 'CONVIDADO' | 'ATIVO' | 'INATIVO'`; `interface Aluno { id: string; nome: string; email: string; telefone: string | null; dataNascimento: string | null; objetivo: string | null; observacoes: string | null; status: StatusAluno; criadoEm: string }`; `interface NovoAluno { nome: string; email: string; telefone?: string | null; dataNascimento?: string | null; objetivo?: string | null; observacoes?: string | null }`; `type DadosAluno = Omit<NovoAluno, 'email'>`; `const ROTULO_STATUS: Record<StatusAluno, string> = { CONVIDADO: 'Convite pendente', ATIVO: 'Ativo', INATIVO: 'Inativo' }`.
  - `AlunoService` (`providedIn: 'root'`), base `${environment.apiUrl}/api/alunos`: `listar(status?: StatusAluno): Promise<Aluno[]>` (query `status` só quando informado), `buscar(id: string): Promise<Aluno>`, `criar(dados: NovoAluno): Promise<Aluno>`, `atualizar(id: string, dados: DadosAluno): Promise<Aluno>`, `alterarStatus(id: string, status: 'ATIVO' | 'INATIVO'): Promise<Aluno>`, `reenviarConvite(id: string): Promise<void>`.

- [ ] **Step 1: Testes que falham** (`HttpTestingController`, com `afterEach(() => http.verify())`):
```ts
it('listar sem status não envia query', ...)           // GET .../api/alunos (params vazios)
it('listar com status envia ?status=INATIVO', ...)
it('criar faz POST com os dados', ...)                 // body igual ao NovoAluno
it('atualizar faz PUT em /{id}', ...)
it('alterarStatus faz PATCH /{id}/status com { status }', ...)
it('reenviarConvite faz POST /{id}/reenviar-convite', ...)
```
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Rodar e ver passar** — `npx ng test --watch=false` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): modelo e servico de alunos"`

---

### Task 8: Front — lista de alunos, navegação e rotas da área

**Files:**
- Create: `frontend/src/app/personal/alunos/lista-alunos/lista-alunos.ts|html|scss`, `lista-alunos.spec.ts`
- Modify: `frontend/src/app/personal/personal.routes.ts`

**Interfaces:**
- Consumes: `AlunoService`, `Aluno`, `ROTULO_STATUS` (Task 7).
- Produces:
  - Item de navegação adicionado depois de Dashboard: `{ rota: '/personal/alunos', rotulo: 'Alunos', icone: 'group' }`.
  - Rotas filhas do `Shell` em `personal.routes.ts`: `alunos` → `ListaAlunos`; `alunos/novo` → `FormAluno`; `alunos/:id` → `DetalheAluno`; `alunos/:id/editar` → `FormAluno` (todas com `loadComponent`; `FormAluno` e `DetalheAluno` são criados nas Tasks 9 e 10 — nesta task registre só `alunos`, e as Tasks 9 e 10 registram as suas).
  - `ListaAlunos`: título `Alunos`; botão `Novo aluno` (`routerLink="/personal/alunos/novo"`); `mat-button-toggle-group` com `Ativos` (padrão, `listar()`) e `Inativos` (`listar('INATIVO')`); campo `Buscar aluno` que filtra no cliente por nome ou e-mail, sem diferenciar maiúsculas; cada item mostra nome, e-mail e o rótulo de status (`ROTULO_STATUS`), e leva a `/personal/alunos/{id}`. Mensagens vazias: `Nenhum aluno cadastrado ainda.` (Ativos sem alunos), `Nenhum aluno inativo.` (Inativos sem alunos), `Nenhum aluno encontrado.` (busca sem resultado). Estado em signals.

- [ ] **Step 1: Testes que falham**
```ts
it('carrega ativos ao abrir e mostra nome, e-mail e status', ...) // listar() chamado sem argumento; 'Convite pendente' visível para CONVIDADO
it('Inativos chama listar("INATIVO")', ...)
it('busca filtra por nome ou e-mail sem diferenciar maiúsculas', ...) // "ANA" mostra "Ana" e esconde "Bruno"
it('mostra mensagens de lista vazia', ...)  // 'Nenhum aluno cadastrado ainda.' / 'Nenhum aluno encontrado.'
it('item leva ao detalhe', ...)             // href '/personal/alunos/<id>'
```
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar** componente, rota `alunos` e item de navegação (ajustar o teste de rotas existente se ele contar os itens).
- [ ] **Step 4: Rodar e ver passar** — `npx ng test --watch=false && npx ng build` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): lista de alunos com busca e filtro de status"`

---

### Task 9: Front — formulário de aluno (novo e editar)

**Files:**
- Create: `frontend/src/app/personal/alunos/form-aluno/form-aluno.ts|html|scss`, `form-aluno.spec.ts`
- Modify: `frontend/src/app/personal/personal.routes.ts` (rotas `alunos/novo` e `alunos/:id/editar`)

**Interfaces:**
- Consumes: `AlunoService`, `NovoAluno`, `DadosAluno` (Task 7).
- Produces: `FormAluno` com `id = input<string>()` (ausente = novo). Reactive Form: `Nome` (obrigatório, máx. 120; erro `Informe o nome.`), `E-mail` (obrigatório e `Validators.email`, só no modo novo; erro `Informe um e-mail válido.`; dica `O aluno receberá um convite neste e-mail.`; no modo editar aparece desabilitado com o e-mail atual), `Telefone` (máx. 20, `type="tel"`), `Data de nascimento` (`type="date"`), `Objetivo` (textarea, máx. 500), `Observações` (textarea, máx. 2000). Título `Novo aluno` / `Editar aluno`; botões `Salvar` e `Cancelar` (volta para a lista no modo novo e para o detalhe no editar). Campos opcionais vazios são enviados como `null`. Ao salvar: botão desabilitado enquanto a requisição está em andamento; sucesso no modo novo → snackbar `Convite enviado para <email>.` e navega para `/personal/alunos/<id>`; no editar → snackbar `Dados do aluno atualizados.` e navega para o detalhe. Em erro, permanece no formulário (o `erroInterceptor` já mostra a mensagem do servidor).

- [ ] **Step 1: Testes que falham**
```ts
it('novo: envia criar com campos opcionais vazios como null e navega ao detalhe', ...)
it('novo: e-mail inválido mostra erro e não envia', ...)        // 'Informe um e-mail válido.'
it('naoEnviaDuasVezesEnquantoSalva', ...)                       // dois cliques com criar pendente → criar chamado 1x
it('editar: carrega o aluno, e-mail desabilitado, envia atualizar sem e-mail', ...)
it('erro ao salvar mantém no formulário e reabilita o botão', ...)
```
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar** e registrar as duas rotas.
- [ ] **Step 4: Rodar e ver passar** — `npx ng test --watch=false && npx ng build` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): formulario de novo aluno e edicao"`

---

### Task 10: Front — detalhe do aluno com ações

**Files:**
- Create: `frontend/src/app/shared/confirmacao/confirmacao-dialog.ts`, `confirmacao-dialog.spec.ts`
- Create: `frontend/src/app/personal/alunos/detalhe-aluno/detalhe-aluno.ts|html|scss`, `detalhe-aluno.spec.ts`
- Modify: `frontend/src/app/personal/personal.routes.ts` (rota `alunos/:id`)

**Interfaces:**
- Consumes: `AlunoService`, `Aluno`, `ROTULO_STATUS` (Task 7).
- Produces:
  - `ConfirmacaoDialog` (MatDialog) com `data: { titulo: string; mensagem: string; confirmar: string }`, botões `Cancelar` e `<confirmar>`; fecha com `true` ao confirmar. Função auxiliar `confirmar(dialog: MatDialog, dados): Promise<boolean>` no mesmo arquivo.
  - `DetalheAluno` com `id = input.required<string>()`: mostra nome, e-mail, status (`ROTULO_STATUS`), telefone, data de nascimento (formato `dd/MM/yyyy`), objetivo e observações (campos vazios aparecem como `—`). Ações:
    - `Editar` → `/personal/alunos/<id>/editar`;
    - `Reenviar convite` (só `CONVIDADO`) → `reenviarConvite`, snackbar `Convite reenviado.`;
    - `Desativar` (`CONVIDADO` ou `ATIVO`) → confirmação (título `Desativar aluno`, mensagem `<nome> perderá o acesso ao app até ser reativado.`, botão `Desativar`) → `alterarStatus(id, 'INATIVO')`, snackbar `Aluno desativado.`;
    - `Reativar` (só `INATIVO`) → `alterarStatus(id, 'ATIVO')`, snackbar `Aluno reativado.`.
    Após cada ação, a tela mostra o aluno devolvido pela API. Botões desabilitados durante a requisição. Link `Voltar` para a lista.

- [ ] **Step 1: Testes que falham**
```ts
// confirmacao-dialog.spec.ts
it('confirmar devolve true ao clicar no botão de confirmação e false ao cancelar', ...)
// detalhe-aluno.spec.ts
it('mostra os dados e os campos vazios como —', ...)
it('CONVIDADO mostra Reenviar convite e Desativar, sem Reativar', ...)
it('Desativar pede confirmação e só altera se confirmado', ...) // confirmar → false: alterarStatus não chamado
it('Reativar altera para ATIVO e mostra Aluno reativado.', ...)
it('Reenviar convite chama o serviço e mostra Convite reenviado.', ...)
```
- [ ] **Step 2: Rodar e ver falhar** — `npx ng test --watch=false` → FAIL.
- [ ] **Step 3: Implementar** e registrar a rota.
- [ ] **Step 4: Rodar e ver passar e verificar no navegador**

Run: `npx ng test --watch=false && npx ng build` → PASS.
Verificação manual (usuário, com a API rodando com `SUPABASE_SERVICE_ROLE_KEY`): como personal, cadastrar um aluno com um e-mail seu → aparece "Convite pendente" na lista; o e-mail do convite chega e o link abre "Definir senha"; definida a senha, cai em `/aluno/treino`; o personal vê o aluno como "Ativo"; desativar → o aluno, ao navegar, é levado ao login com "Sua conta não tem acesso ao app."; reativar → volta a entrar.

- [ ] **Step 5: Commit** — `git commit -m "feat(frontend): detalhe do aluno com reenvio de convite, desativar e reativar"`
