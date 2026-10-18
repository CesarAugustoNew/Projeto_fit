# Etapa 1 — Fundação: Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Monorepo com API Spring Boot autenticando via JWT do Supabase (`GET /api/me`, bootstrap do personal, erros em Problem Details) e front Angular com login, definir/esqueci senha, guards por papel, layout responsivo e CI.

**Architecture:** O Supabase Auth emite o JWT; o Angular usa `supabase-js` só para autenticar e chama a API Spring com `Authorization: Bearer`. A API é OAuth2 Resource Server, valida o token (ES256/RS256, `iss`, `aud`) e resolve o papel a partir da tabela `usuario` própria. Pacotes por funcionalidade, camadas dentro de cada um.

**Tech Stack:** Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Security OAuth2 Resource Server, Validation, Flyway, Actuator), PostgreSQL 17, Testcontainers 2, JUnit 5, Mockito · Angular 22, Angular Material, `@supabase/supabase-js`, Vitest · Docker Compose · GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-10-18-app-personal-trainer-design.md`

**Planos seguintes** (um por etapa da seção 12 da spec, escritos ao fim da etapa anterior): 2 Alunos e convite · 3 Catálogo de exercícios (inclui a semente no bootstrap) · 4 Fichas · 5 Sessões do aluno · 6 Desempenho e dashboard · 7 Deploy.

## Pré-requisitos (feitos pelo usuário antes da Task 1)
- Node **24 LTS** instalado (`node -v` → `v24.x`). O Angular 22 não roda no Node 20.
- Docker Desktop rodando (Testcontainers e Compose).
- Um projeto Supabase **de desenvolvimento** (região `us-east-1`) com: JWT Signing Keys assimétricas ativas, *Disable signups* ligado e a URL `http://localhost:4200/definir-senha` em *Redirect URLs*. Anotar a URL do projeto e a *anon/publishable key*.
- Uma conta para o personal criada em *Authentication → Users* (e-mail + senha); esse e-mail vai em `APP_PERSONAL_EMAIL`.

## Global Constraints
- Pacote raiz Java: `com.projetofit`; código em `backend/`, front em `frontend/`.
- Java 21 (`<java.version>21</java.version>`), Spring Boot 4.1.x, PostgreSQL `postgres:17-alpine` em testes e Compose.
- Schema só via Flyway em `backend/src/main/resources/db/migration`; `spring.jpa.hibernate.ddl-auto=validate`; toda tabela nova recebe `alter table <t> enable row level security;` sem políticas.
- Controllers recebem e devolvem DTOs (`record`), nunca entidades.
- Erros em Problem Details (`application/problem+json`) com títulos em pt-BR definidos na Task 2.
- Variáveis de ambiente do back: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `SUPABASE_URL`, `APP_PERSONAL_EMAIL`, `APP_FRONT_URL` (padrão `http://localhost:4200`). `SUPABASE_SERVICE_ROLE_KEY` só a partir da Etapa 2.
- Nenhum segredo no repositório. A URL do Supabase e a *anon/publishable key* podem ir para `environment*.ts` (são públicas).
- Interface em pt-BR; nomes de domínio em português (`usuario`, `papel`, `PERSONAL`, `ALUNO`).
- Comandos de shell abaixo são para Git Bash, a partir da raiz do repositório.

## Review Focus
1. **Token real do Supabase é ES256 (curva P-256)**: um decoder configurado só para RS256 rejeitaria todo login com 401. → Task 3 gera os tokens de teste em ES256.
2. **Preflight CORS do navegador** (`OPTIONS` com `Authorization`) deve passar sem token, senão o front em outra origem não consegue chamar nada. → Task 3, teste `preflightCorsDoFrontEhPermitido`.
3. **E-mail do personal com maiúsculas/minúsculas diferentes** e **dois requests simultâneos no primeiro login** não podem falhar nem duplicar o usuário. → Task 3, testes `emailDoPersonalComMaiusculasTambemFazBootstrap` e `insercaoConcorrenteRelêOUsuario`.
4. **Sessão expira com a página aberta**: o 401 deve deslogar e levar ao login uma única vez, sem loop. → Task 5, teste do `erroInterceptor`.
5. **Usuário de um papel abrindo URL do outro** (aluno acessa `/personal/...`) deve ser levado para a própria área, não ver tela quebrada. → Task 5, teste do `papelGuard`.

---

### Task 1: Esqueleto do back-end, migration de `usuario` e Docker Compose

**Files:**
- Create: `backend/` (gerado pelo Spring Initializr), `docker-compose.yml`, `.gitignore`
- Create: `backend/src/main/resources/application.yml`, `application-dev.yml`
- Create: `backend/src/main/resources/db/migration/V1__usuario.sql`
- Create: `backend/src/main/java/com/projetofit/comum/AppProperties.java`
- Create: `backend/src/test/resources/application-test.yml`
- Create: `backend/src/test/java/com/projetofit/TesteIntegracao.java`
- Test: `backend/src/test/java/com/projetofit/MigracoesIT.java`

**Interfaces:**
- Produces: `TesteIntegracao` (classe abstrata base de testes de integração: `@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`, `@Import(TestcontainersConfiguration.class)`, campo `protected MockMvc mvc`); `AppProperties(String supabaseUrl, String personalEmail, String frontUrl)` com prefixo `app`.

- [ ] **Step 1: Gerar o projeto**

```bash
curl -s https://start.spring.io/starter.zip \
  -d type=maven-project -d language=java -d bootVersion=4.1.1 -d javaVersion=21 \
  -d groupId=com.projetofit -d artifactId=projetofit -d name=projetofit -d packageName=com.projetofit \
  -d dependencies=web,data-jpa,validation,actuator,flyway,postgresql,testcontainers \
  -o backend.zip && mkdir backend && unzip -q backend.zip -d backend && rm backend.zip
```
Se `4.1.1` for recusado, use o 4.1.x mais recente listado em `https://start.spring.io/metadata/client`. Apague `ProjetofitApplicationTests.java`; mantenha `TestcontainersConfiguration.java` (com o container do Postgres usando a imagem `postgres:17-alpine`) e `TestProjetofitApplication.java`.

- [ ] **Step 2: Escrever o teste que falha** em `MigracoesIT extends TesteIntegracao`

```java
@Autowired JdbcTemplate jdbc;

@Test void tabelaUsuarioExisteComRlsAtivo() {
    Boolean rls = jdbc.queryForObject(
        "select relrowsecurity from pg_class where relname = 'usuario'", Boolean.class);
    assertThat(rls).isTrue();
}
```

- [ ] **Step 3: Rodar e ver falhar**

Run: `cd backend && ./mvnw -q test -Dtest=MigracoesIT`
Expected: FAIL (`TesteIntegracao` inexistente / tabela inexistente).

- [ ] **Step 4: Implementar**
  - `V1__usuario.sql`: tabela `usuario` exatamente como na seção 5 da spec (`papel` com `check (papel in ('PERSONAL','ALUNO'))`, `criado_em`/`atualizado_em timestamptz not null default now()`) + RLS.
  - `application.yml`: datasource por `${DB_URL}`, `${DB_USER}`, `${DB_PASSWORD}`; `spring.jpa.open-in-view=false`; `ddl-auto=validate`; `spring.mvc.problemdetails.enabled=true`; `management.endpoints.web.exposure.include=health`; bloco `app` com `supabase-url: ${SUPABASE_URL}`, `personal-email: ${APP_PERSONAL_EMAIL}`, `front-url: ${APP_FRONT_URL:http://localhost:4200}`.
  - `application-dev.yml`: `DB_URL` padrão `jdbc:postgresql://localhost:5432/projetofit`, usuário/senha `postgres`/`postgres`.
  - `application-test.yml`: `app.supabase-url=https://teste.supabase.co`, `app.personal-email=personal@teste.com` (o datasource vem do `@ServiceConnection`).
  - `AppProperties` como `@ConfigurationProperties("app") record`, habilitado com `@ConfigurationPropertiesScan` na classe `ProjetofitApplication`.
  - `docker-compose.yml` na raiz: serviço `postgres` (`postgres:17-alpine`, porta `5432`, `POSTGRES_DB=projetofit`, senha `postgres`, volume nomeado).
  - `.gitignore` na raiz: `backend/target/`, `frontend/node_modules/`, `frontend/dist/`, `frontend/.angular/`, `.env`, `.idea/`, `.vscode/`.

- [ ] **Step 5: Rodar e ver passar**

Run: `cd backend && ./mvnw -q test -Dtest=MigracoesIT`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add .gitignore docker-compose.yml backend
git commit -m "feat(backend): esqueleto Spring Boot 4.1 com Flyway e tabela usuario"
```

---

### Task 2: Tratamento global de erros (Problem Details)

**Files:**
- Create: `backend/src/main/java/com/projetofit/comum/erro/TratadorGlobalDeErros.java`
- Create: `.../comum/erro/RecursoNaoEncontradoException.java`, `ConflitoException.java`, `IntegracaoExternaException.java`
- Test: `backend/src/test/java/com/projetofit/comum/erro/TratadorGlobalDeErrosTest.java`, `ControllerDeTeste.java` (só em teste)

**Interfaces:**
- Produces: exceções `RecursoNaoEncontradoException(String detalhe)` → 404, `ConflitoException(String detalhe)` → 409, `IntegracaoExternaException(String detalhe, Throwable causa)` → 502, todas `extends RuntimeException`. Títulos fixos (copiar exatamente):

| Situação | status | `title` | `detail` |
|---|---|---|---|
| Bean Validation | 400 | `Dados inválidos` | `Um ou mais campos são inválidos.` + `errors: {campo: mensagem}` |
| `RecursoNaoEncontradoException` | 404 | `Recurso não encontrado` | mensagem da exceção |
| `ConflitoException` | 409 | `Conflito` | mensagem da exceção |
| `IntegracaoExternaException` | 502 | `Falha em serviço externo` | mensagem da exceção |
| qualquer outra `Exception` | 500 | `Erro interno` | `Ocorreu um erro inesperado.` (log com stack trace) |
| 401 (Task 3) | 401 | `Não autenticado` | `Token ausente, inválido ou expirado.` |
| 403 (Task 3) | 403 | `Acesso negado` | `Você não tem permissão para acessar este recurso.` |

- [ ] **Step 1: Escrever os testes que falham** — MockMvc standalone, sem contexto Spring:

```java
MockMvc mvc = MockMvcBuilders.standaloneSetup(new ControllerDeTeste())
    .setControllerAdvice(new TratadorGlobalDeErros()).build();

@Test void validacaoRetorna400ComCampos() throws Exception {
    mvc.perform(post("/teste/validacao").contentType(APPLICATION_JSON).content("{\"nome\":\"\",\"series\":0}"))
       .andExpect(status().isBadRequest())
       .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
       .andExpect(jsonPath("$.title").value("Dados inválidos"))
       .andExpect(jsonPath("$.errors.nome").exists())
       .andExpect(jsonPath("$.errors.series").exists());
}
@Test void naoEncontradoRetorna404() // GET /teste/nao-encontrado → 404, title "Recurso não encontrado", detail "Aluno não encontrado"
@Test void conflitoRetorna409()      // GET /teste/conflito → 409, title "Conflito", detail "E-mail já cadastrado"
@Test void integracaoRetorna502()    // GET /teste/externo → 502, title "Falha em serviço externo"
@Test void erroInesperadoNaoVazaDetalhes() throws Exception {
    mvc.perform(get("/teste/erro"))
       .andExpect(status().isInternalServerError())
       .andExpect(jsonPath("$.detail").value("Ocorreu um erro inesperado."))
       .andExpect(content().string(not(containsString("segredo interno"))));
}
```
`ControllerDeTeste` (`@RestController @RequestMapping("/teste")`) lança cada exceção; `/teste/erro` lança `IllegalStateException("segredo interno")`; `POST /teste/validacao` recebe `@Valid record Entrada(@NotBlank String nome, @Min(1) int series)`. Não verificar o texto das mensagens de validação (dependem do locale da JVM).

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd backend && ./mvnw -q test -Dtest=TratadorGlobalDeErrosTest`
Expected: FAIL (classes inexistentes).

- [ ] **Step 3: Implementar `TratadorGlobalDeErros`** como `@RestControllerAdvice extends ResponseEntityExceptionHandler`, sobrescrevendo `handleMethodArgumentNotValid` (title/detail da tabela + propriedade `errors` com o primeiro erro por campo) e com um `@ExceptionHandler` por exceção da tabela, montando `ProblemDetail.forStatusAndDetail(...)` + `setTitle(...)`.

- [ ] **Step 4: Rodar e ver passar**

Run: `cd backend && ./mvnw -q test -Dtest=TratadorGlobalDeErrosTest`
Expected: PASS (5 testes).

- [ ] **Step 5: Commit**

```bash
git add backend/src
git commit -m "feat(backend): tratamento global de erros em Problem Details"
```

---

### Task 3: Segurança JWT, módulo `usuario` e `GET /api/me`

**Files:**
- Modify: `backend/pom.xml` (adicionar `spring-boot-starter-security-oauth2-resource-server` e, em teste, `spring-boot-starter-security-test`; se um artifactId não resolver, conferir no Initializr com as dependências `security,oauth2-resource-server` para Boot 4.1)
- Create: `backend/src/main/java/com/projetofit/comum/seguranca/SegurancaConfig.java`, `SupabaseJwtConfig.java`, `UsuarioJwtAuthenticationConverter.java`, `RespostasDeSegurancaProblem.java`
- Create: `backend/src/main/java/com/projetofit/usuario/Papel.java`, `Usuario.java`, `UsuarioRepository.java`, `UsuarioAtualResolver.java`, `UsuarioAtual.java`, `MeController.java`, `UsuarioDTO.java`
- Modify: `backend/src/test/java/com/projetofit/TesteIntegracao.java` (importar `JwtDeTesteConfig`)
- Create (teste): `backend/src/test/java/com/projetofit/seguranca/JwtDeTesteConfig.java`, `TokenDeTeste.java`
- Test: `backend/src/test/java/com/projetofit/usuario/MeControllerIT.java`, `UsuarioAtualResolverTest.java`

**Interfaces:**
- Consumes: `AppProperties` (Task 1); `TratadorGlobalDeErros` e títulos 401/403 (Task 2).
- Produces:
  - `enum Papel { PERSONAL, ALUNO }`
  - `@Entity Usuario` (`UUID id` atribuído, `String nome`, `String email`, `@Enumerated(STRING) Papel papel`, `Instant criadoEm`, `Instant atualizadoEm`) e `UsuarioRepository extends JpaRepository<Usuario, UUID>` com `Optional<Usuario> findByEmailIgnoreCase(String email)`.
  - `UsuarioAtualResolver.resolver(UUID sub, String email): Optional<Usuario>`: busca por `id`; se não existir e `email` for igual (ignorando caixa) a `app.personal-email`, cria `Usuario(sub, <parte antes do @>, email em minúsculas, PERSONAL)` com `saveAndFlush`; em `DataIntegrityViolationException`, busca de novo por `id`. Sem `@Transactional` no método. **Ponto de extensão:** a Etapa 2 acrescenta aqui a verificação de aluno `CONVIDADO`→`ATIVO` e `INATIVO`→sem papel; a Etapa 3 acrescenta a semente de exercícios no bootstrap.
  - `UsuarioJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken>`: chama o resolver com `sub` e a claim `email`; devolve `JwtAuthenticationToken(jwt, authorities)` com `ROLE_<PAPEL>` ou sem authorities se não houver usuário.
  - `UsuarioAtual` (`@Component`): `UUID id()` (o `sub` do token no `SecurityContext`) e `Papel papel()`.
  - `SupabaseJwtConfig`: `@Bean OAuth2TokenValidator<Jwt> supabaseJwtValidator(AppProperties)` = timestamp + `iss = {supabaseUrl}/auth/v1` + `aud` contém `authenticated`; `@Bean JwtDecoder jwtDecoder(...)` = `NimbusJwtDecoder.withJwkSetUri({supabaseUrl}/auth/v1/.well-known/jwks.json)` aceitando `ES256` e `RS256`, com esse validador.
  - `SegurancaConfig`: stateless, CSRF desligado, CORS com origens `{app.front-url}` e `http://localhost:4200` (métodos GET/POST/PUT/PATCH/DELETE/OPTIONS, header `Authorization` e `Content-Type`), `/actuator/health` público, `/api/**` exige `hasAnyRole("PERSONAL","ALUNO")`, resto negado; `@EnableMethodSecurity`; `authenticationEntryPoint` e `accessDeniedHandler` de `RespostasDeSegurancaProblem` escrevendo o Problem Details 401/403 da Task 2 (serializar com o `JsonMapper` do Jackson 3 injetado, content-type `application/problem+json`).
  - `GET /api/me` → `UsuarioDTO(UUID id, String nome, String email, Papel papel)`.
  - Teste: `TokenDeTeste.gerar(UUID sub, String email)` e `TokenDeTeste.gerar(UUID sub, String email, Instant expiracao, String issuer)`, assinados em **ES256** com uma chave EC P-256 estática de `JwtDeTesteConfig`; padrão `iss = https://teste.supabase.co/auth/v1`, `aud = authenticated`, `exp = agora + 1h`. `JwtDeTesteConfig` fornece um `@Primary JwtDecoder` = `new NimbusJwtDecoder(jwtProcessor)` sobre um `ImmutableJWKSet` com a chave pública, usando o mesmo bean `supabaseJwtValidator`.

- [ ] **Step 1: Escrever os testes que falham** em `MeControllerIT extends TesteIntegracao` (`@BeforeEach usuarioRepository.deleteAll()`):

```java
@Test void semTokenRetorna401ComProblemDetail() // GET /api/me → 401, content-type application/problem+json, $.title "Não autenticado"
@Test void tokenExpiradoRetorna401()            // exp = agora - 1min → 401
@Test void tokenDeOutroIssuerRetorna401()       // iss = "https://outro.supabase.co/auth/v1" → 401
@Test void primeiroAcessoDoPersonalCriaUsuarioPersonal() throws Exception {
    UUID sub = UUID.randomUUID();
    mvc.perform(get("/api/me").header("Authorization", "Bearer " + TokenDeTeste.gerar(sub, "personal@teste.com")))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.id").value(sub.toString()))
       .andExpect(jsonPath("$.nome").value("personal"))
       .andExpect(jsonPath("$.papel").value("PERSONAL"));
    assertThat(usuarioRepository.findById(sub)).isPresent();
}
@Test void emailDoPersonalComMaiusculasTambemFazBootstrap() // "Personal@Teste.COM" → 200 PERSONAL; email salvo "personal@teste.com"
@Test void segundoAcessoNaoDuplica()            // duas chamadas com o mesmo token → usuarioRepository.count() == 1
@Test void usuarioDesconhecidoRetorna403()      // "outro@teste.com" sem usuario → 403, $.title "Acesso negado"
@Test void alunoExistenteRetornaPapelAluno()    // salvar Usuario(sub, "Ana", "ana@teste.com", ALUNO) → 200, $.papel "ALUNO"
@Test void healthEhPublico()                    // GET /actuator/health sem token → 200
@Test void preflightCorsDoFrontEhPermitido() throws Exception {
    mvc.perform(options("/api/me").header("Origin", "http://localhost:4200")
            .header("Access-Control-Request-Method", "GET")
            .header("Access-Control-Request-Headers", "authorization"))
       .andExpect(status().isOk())
       .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
}
@Test void origemNaoPermitidaEhBloqueada()      // mesmo preflight com Origin "https://malicioso.com" → 403
```
E em `UsuarioAtualResolverTest` (Mockito, sem Spring):
```java
@Test void insercaoConcorrenteRelêOUsuario() {
    // findById(sub) → Optional.empty() na 1ª chamada e Optional.of(usuario) na 2ª;
    // saveAndFlush(...) lança DataIntegrityViolationException
    assertThat(resolver.resolver(sub, "personal@teste.com")).contains(usuario);
}
@Test void emailDiferenteDoPersonalNaoCriaNada() // resolver(sub, "x@teste.com") vazio; saveAndFlush nunca chamado
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd backend && ./mvnw -q test -Dtest='MeControllerIT,UsuarioAtualResolverTest'`
Expected: FAIL (classes inexistentes).

- [ ] **Step 3: Implementar** as classes listadas em Interfaces, na ordem: `Papel`/`Usuario`/`UsuarioRepository` → `UsuarioAtualResolver` → `SupabaseJwtConfig` → `UsuarioJwtAuthenticationConverter` → `RespostasDeSegurancaProblem` → `SegurancaConfig` → `UsuarioAtual` → `MeController`. Depois, `JwtDeTesteConfig`/`TokenDeTeste` e `@Import` em `TesteIntegracao`.

- [ ] **Step 4: Rodar a suíte inteira do back**

Run: `cd backend && ./mvnw -q verify`
Expected: BUILD SUCCESS, todos os testes passando (inclusive `MigracoesIT` e `TratadorGlobalDeErrosTest`).

- [ ] **Step 5: Commit**

```bash
git add backend
git commit -m "feat(backend): seguranca JWT do Supabase, bootstrap do personal e GET /api/me"
```

---

### Task 4: Esqueleto do front-end, `AuthService` e `UsuarioService`

**Files:**
- Create: `frontend/` (gerado pelo Angular CLI 22)
- Create: `frontend/src/environments/environment.ts`, `environment.development.ts`
- Create: `frontend/src/app/core/auth/supabase.ts`, `auth.service.ts`
- Create: `frontend/src/app/core/usuario/usuario.model.ts`, `usuario.service.ts`
- Modify: `frontend/src/app/app.config.ts`
- Test: `frontend/src/app/core/auth/auth.service.spec.ts`, `frontend/src/app/core/usuario/usuario.service.spec.ts`

**Interfaces:**
- Produces:
  - `environment: { production: boolean; apiUrl: string; supabaseUrl: string; supabaseAnonKey: string }`. Em desenvolvimento, `apiUrl = 'http://localhost:8080'`; URL e chave do projeto Supabase de desenvolvimento (pré-requisitos).
  - `SUPABASE_CLIENT = new InjectionToken<SupabaseClient>('SUPABASE_CLIENT')` e `provideSupabase(): Provider` (factory com `createClient(environment.supabaseUrl, environment.supabaseAnonKey)`), registrado em `app.config.ts` junto com `provideHttpClient(withInterceptors([...]))` (a lista de interceptors fica vazia até a Task 5) e `provideAnimationsAsync()`, se o Material instalado pedir.
  - `AuthService` (`providedIn: 'root'`): `login(email: string, senha: string): Promise<void>` (lança `Error` se o Supabase devolver erro), `logout(): Promise<void>`, `token(): Promise<string | null>` (`access_token` de `auth.getSession()`), `estaLogado(): Promise<boolean>`, `definirSenha(senha: string): Promise<void>` (`auth.updateUser({ password })`), `enviarRedefinicao(email: string): Promise<void>` (`auth.resetPasswordForEmail(email, { redirectTo: window.location.origin + '/definir-senha' })`).
  - `usuario.model.ts`: `type Papel = 'PERSONAL' | 'ALUNO'`; `interface Usuario { id: string; nome: string; email: string; papel: Papel }`; `rotaInicial(papel: Papel): string` → `'/personal/dashboard'` ou `'/aluno/treino'`.
  - `UsuarioService` (`providedIn: 'root'`): `readonly usuario: Signal<Usuario | null>`; `carregar(): Promise<Usuario>` (GET `${apiUrl}/api/me`, guarda no signal); `limpar(): void`.

- [ ] **Step 1: Gerar o projeto e instalar as dependências**

```bash
npx @angular/cli@22 new frontend --routing --style=scss --ssr=false --skip-git --defaults
cd frontend && npx ng add @angular/material --defaults && npm i @supabase/supabase-js && npx ng g environments
```
Expected: `npx ng test --watch=false` roda o teste padrão do `App` com Vitest e passa.

- [ ] **Step 2: Escrever os testes que falham**

`auth.service.spec.ts`, com `{ provide: SUPABASE_CLIENT, useValue: supabaseFalso }`, em que `supabaseFalso.auth` tem `signInWithPassword`, `signOut`, `getSession`, `updateUser` e `resetPasswordForEmail` como `vi.fn()`:
```ts
it('login chama signInWithPassword com email e senha', ...)  // espera { email: 'a@b.com', password: '12345678' }
it('login lança erro quando o Supabase devolve erro', async () => {
  supabaseFalso.auth.signInWithPassword.mockResolvedValue({ data: {}, error: { message: 'Invalid login credentials' } });
  await expect(service.login('a@b.com', 'x')).rejects.toThrow();
});
it('token devolve access_token da sessão ou null', ...)  // getSession → { data: { session: { access_token: 'abc' } } } → 'abc'; session null → null
it('enviarRedefinicao usa redirectTo origin + /definir-senha', ...)  // expect.objectContaining({ redirectTo: `${window.location.origin}/definir-senha` })
```
`usuario.service.spec.ts`, com `provideHttpClient()` e `provideHttpClientTesting()`:
```ts
it('carregar faz GET /api/me e guarda no signal', async () => {
  const p = service.carregar();
  http.expectOne(`${environment.apiUrl}/api/me`).flush({ id: '1', nome: 'Ana', email: 'a@b.com', papel: 'ALUNO' });
  expect((await p).papel).toBe('ALUNO');
  expect(service.usuario()?.nome).toBe('Ana');
});
it('limpar zera o usuário', ...)
it('rotaInicial mapeia os papéis', ...)  // PERSONAL → '/personal/dashboard', ALUNO → '/aluno/treino'
```

- [ ] **Step 3: Rodar e ver falhar**

Run: `cd frontend && npx ng test --watch=false`
Expected: FAIL (módulos inexistentes).

- [ ] **Step 4: Implementar** `supabase.ts`, `auth.service.ts`, `usuario.model.ts`, `usuario.service.ts`, os environments e `app.config.ts` conforme Interfaces.

- [ ] **Step 5: Rodar e ver passar**

Run: `cd frontend && npx ng test --watch=false && npx ng build`
Expected: testes PASS e build OK.

- [ ] **Step 6: Commit**

```bash
git add frontend
git commit -m "feat(frontend): esqueleto Angular 22 com AuthService (Supabase) e UsuarioService"
```

---

### Task 5: Interceptors HTTP e guards de rota

**Files:**
- Create: `frontend/src/app/core/http/auth.interceptor.ts`, `erro.interceptor.ts`
- Create: `frontend/src/app/core/guards/auth.guard.ts`, `papel.guard.ts`
- Modify: `frontend/src/app/app.config.ts` (`withInterceptors([authInterceptor, erroInterceptor])`)
- Test: `auth.interceptor.spec.ts`, `erro.interceptor.spec.ts`, `auth.guard.spec.ts`, `papel.guard.spec.ts` (ao lado de cada arquivo)

**Interfaces:**
- Consumes: `AuthService`, `UsuarioService`, `rotaInicial`, `environment.apiUrl` (Task 4).
- Produces:
  - `authInterceptor: HttpInterceptorFn`: só para URLs que começam com `environment.apiUrl`, adiciona `Authorization: Bearer <token>` quando `token()` não é nulo.
  - `erroInterceptor: HttpInterceptorFn`: em 401 → `auth.logout()`, `usuario.limpar()`, `router.navigateByUrl('/login')`, apenas se a rota atual ainda não for `/login`. Status 0 → snackbar `Sem conexão com o servidor.`. 403 → nada aqui (o `papelGuard` trata). Outros erros → snackbar com `error.detail ?? error.title ?? 'Erro inesperado.'`. Snackbar: `MatSnackBar.open(msg, 'Fechar', { duration: 5000 })`. Sempre relança o erro.
  - `authGuard: CanActivateFn`: `estaLogado()` → `true`; senão `UrlTree('/login')`.
  - `papelGuard(papel: Papel): CanActivateFn`: usa `usuario()` ou `carregar()`. Papel igual → `true`; diferente → `UrlTree(rotaInicial(papelDoUsuario))`; se `carregar()` falhar → `logout()` e `UrlTree('/login')`.

- [ ] **Step 1: Escrever os testes que falham**

```ts
// auth.interceptor.spec.ts
it('adiciona Bearer nas chamadas à API', ...)        // token 'abc' → header Authorization === 'Bearer abc'
it('não envia token para outras origens', ...)       // GET https://exemplo.com → sem Authorization
it('não adiciona header sem sessão', ...)            // token null → sem Authorization
// erro.interceptor.spec.ts
it('401 desloga, limpa usuário e vai ao login', ...)  // logout e limpar chamados 1x; navigateByUrl('/login')
it('401 já estando no /login não navega de novo', ...)  // router.url '/login' → navigateByUrl não chamado
it('erro com Problem Details mostra o detail', ...)   // 409 { title: 'Conflito', detail: 'E-mail já cadastrado' } → open('E-mail já cadastrado', 'Fechar', ...)
it('status 0 mostra sem conexão', ...)                // open('Sem conexão com o servidor.', 'Fechar', ...)
it('relança o erro', ...)
// auth.guard.spec.ts
it('sem sessão redireciona para /login', ...); it('com sessão permite', ...)
// papel.guard.spec.ts
it('papel igual permite', ...)
it('aluno em rota de personal vai para /aluno/treino', ...)   // UrlTree serializada === '/aluno/treino'
it('falha ao carregar /me desloga e vai para /login', ...)   // carregar rejeita (403) → logout chamado; '/login'
```
Use `TestBed.runInInjectionContext` para executar guards e interceptors, com `vi.fn()` em `AuthService`, `UsuarioService`, `Router` e `MatSnackBar`.

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd frontend && npx ng test --watch=false`
Expected: FAIL.

- [ ] **Step 3: Implementar** os quatro arquivos conforme Interfaces e registrar os interceptors em `app.config.ts`.

- [ ] **Step 4: Rodar e ver passar**

Run: `cd frontend && npx ng test --watch=false`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src
git commit -m "feat(frontend): interceptors de token e erro, guards de autenticacao e papel"
```

---

### Task 6: Telas públicas: login, definir senha, esqueci a senha

**Files:**
- Create: `frontend/src/app/publico/login/login.ts|html|scss`, `frontend/src/app/publico/definir-senha/definir-senha.ts|html|scss`, `frontend/src/app/publico/esqueci-senha/esqueci-senha.ts|html|scss`
- Test: `login.spec.ts`, `definir-senha.spec.ts`, `esqueci-senha.spec.ts`

**Interfaces:**
- Consumes: `AuthService`, `UsuarioService`, `rotaInicial` (Task 4).
- Produces: componentes standalone `Login`, `DefinirSenha` e `EsqueciSenha`, com Reactive Forms e Angular Material (`mat-form-field`, `mat-button`), centralizados num card de largura máxima de 400px e largura total no celular. Textos (copiar exatamente):
  - Login: título `Entrar`; campos `E-mail` e `Senha`; botão `Entrar`; link `Esqueci minha senha` → `/esqueci-senha`; erro `E-mail ou senha inválidos.`
  - DefinirSenha: título `Definir senha`; campos `Nova senha` (mínimo 8) e `Confirmar senha`; botão `Salvar`; erro `As senhas não conferem.`
  - EsqueciSenha: título `Recuperar senha`; campo `E-mail`; botão `Enviar link`; sucesso `Se o e-mail estiver cadastrado, você receberá um link para redefinir a senha.`

- [ ] **Step 1: Escrever os testes que falham**

```ts
// login.spec.ts
it('botão desabilitado com formulário inválido', ...)
it('login válido carrega /me e navega para a rota do papel', async () => {
  // preencher 'personal@teste.com' / '12345678', clicar Entrar
  // expect(auth.login).toHaveBeenCalledWith('personal@teste.com', '12345678');
  // usuario.carregar → { papel: 'PERSONAL' } ⇒ expect(router.navigateByUrl).toHaveBeenCalledWith('/personal/dashboard');
});
it('credenciais inválidas mostram mensagem', ...)  // login rejeita → texto 'E-mail ou senha inválidos.' na tela
it('com sessão ativa ao abrir, redireciona direto', ...)  // estaLogado true + carregar ALUNO → '/aluno/treino'
// definir-senha.spec.ts
it('senhas diferentes mostram erro e não chamam definirSenha', ...)
it('senha válida salva, carrega /me e navega', ...)  // definirSenha('novaSenha1') → navigateByUrl(rotaInicial(papel))
// esqueci-senha.spec.ts
it('envia o link e mostra a mensagem de sucesso', ...)  // enviarRedefinicao('a@b.com') + texto de sucesso
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd frontend && npx ng test --watch=false`
Expected: FAIL.

- [ ] **Step 3: Implementar** os três componentes.

- [ ] **Step 4: Rodar e ver passar**

Run: `cd frontend && npx ng test --watch=false`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src
git commit -m "feat(frontend): telas de login, definir senha e esqueci a senha"
```

---

### Task 7: Layout responsivo (shell), áreas por papel e rotas

**Files:**
- Create: `frontend/src/app/layout/shell.ts|html|scss`, `frontend/src/app/layout/item-nav.ts`
- Create: `frontend/src/app/personal/personal.routes.ts`, `frontend/src/app/personal/dashboard/dashboard.ts`
- Create: `frontend/src/app/aluno/aluno.routes.ts`, `frontend/src/app/aluno/treino/treino.ts`
- Modify: `frontend/src/app/app.routes.ts`
- Test: `frontend/src/app/layout/shell.spec.ts`, `frontend/src/app/app.routes.spec.ts`

**Interfaces:**
- Consumes: guards (Task 5), telas públicas (Task 6), `AuthService`/`UsuarioService` (Task 4).
- Produces:
  - `interface ItemNav { rota: string; rotulo: string; icone: string }` (ícone do Material Icons).
  - `Shell`: `itens = input.required<ItemNav[]>()`, `titulo = input('Projeto Fit')`. Usa `BreakpointObserver` com `Breakpoints.Handset`. **Celular:** `mat-toolbar` no topo, `<router-outlet>` e uma barra inferior fixa (`nav.nav-inferior`) com um link por item (ícone + rótulo, `routerLinkActive`), sem esconder conteúdo atrás da barra. **Desktop:** `mat-sidenav-container` com `mat-sidenav mode="side" opened` listando os itens. Botão `Sair` (ícone `logout`) na toolbar: `auth.logout()`, `usuario.limpar()`, `navigateByUrl('/login')`.
  - `app.routes.ts`: `''` → redirect `'login'`; `login`, `definir-senha` e `esqueci-senha` públicas; `personal` com `canActivate: [authGuard, papelGuard('PERSONAL')]`, `loadChildren` de `personal.routes.ts`; `aluno` com `[authGuard, papelGuard('ALUNO')]`, `loadChildren` de `aluno.routes.ts`; `**` → redirect `'login'`.
  - `personal.routes.ts`: rota `''` com `component: Shell` e `data: { itens: [...] }` com `[{ rota: '/personal/dashboard', rotulo: 'Dashboard', icone: 'dashboard' }]`, filho `dashboard` → `Dashboard` (placeholder: `<h1>Dashboard</h1>` + `Olá, {nome}`). `aluno.routes.ts`: igual, com `[{ rota: '/aluno/treino', rotulo: 'Treino', icone: 'fitness_center' }]` e filho `treino` → `Treino` (placeholder `<h1>Treino de hoje</h1>`). Para passar `itens` via rota, habilitar `withComponentInputBinding()` no `provideRouter`. As etapas seguintes acrescentam itens nessas listas.

- [ ] **Step 1: Escrever os testes que falham**

```ts
// shell.spec.ts — BreakpointObserver falso com observe() → of({ matches: true/false })
it('no celular mostra a barra inferior com os itens', ...)   // matches true → nav.nav-inferior com 1 link 'Dashboard'
it('no desktop mostra o menu lateral', ...)                  // matches false → mat-sidenav presente, sem nav.nav-inferior
it('Sair desloga, limpa e vai ao login', ...)
// app.routes.spec.ts — RouterTestingHarness
it('rota vazia redireciona para /login', ...)
it('/personal usa authGuard e papelGuard', ...)  // inspecionar a config: canActivate com 2 guards
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd frontend && npx ng test --watch=false`
Expected: FAIL.

- [ ] **Step 3: Implementar** `Shell`, as rotas e os placeholders; remover o conteúdo de exemplo do `App` gerado, que fica só com `<router-outlet />`, e ajustar o teste padrão do `App`.

- [ ] **Step 4: Rodar e ver passar e verificar no navegador**

Run: `cd frontend && npx ng test --watch=false && npx ng build`
Expected: PASS e build OK.
Verificação manual com o back rodando (`docker compose up -d`, depois `cd backend && SPRING_PROFILES_ACTIVE=dev SUPABASE_URL=<url-dev> APP_PERSONAL_EMAIL=<email> ./mvnw spring-boot:run`) e o front (`cd frontend && npx ng serve`): login com a conta do personal → cai em `/personal/dashboard` com "Olá, ..."; no DevTools em modo celular aparece a barra inferior; `Sair` volta ao login; acessar `/aluno/treino` logado como personal volta ao dashboard.

- [ ] **Step 5: Commit**

```bash
git add frontend/src
git commit -m "feat(frontend): shell responsivo, areas por papel e rotas protegidas"
```

---

### Task 8: CI e instruções de desenvolvimento

**Files:**
- Create: `.github/workflows/ci.yml`
- Modify: `README.md`

**Interfaces:**
- Consumes: comandos de build e teste das Tasks 1–7.

- [ ] **Step 1: Criar `ci.yml`**: em `push` e `pull_request` para `main`, dois jobs em `ubuntu-latest`:
  - `backend`: `actions/setup-java` (Temurin 21, cache maven), `working-directory: backend`, `./mvnw -B verify`. O Docker do runner atende o Testcontainers.
  - `frontend`: `actions/setup-node` (Node 24, cache npm com `frontend/package-lock.json`), `working-directory: frontend`, `npm ci`, `npx ng test --watch=false`, `npx ng build`.

- [ ] **Step 2: Atualizar `README.md`** com: o que é o projeto (1 parágrafo), pré-requisitos (Java 21+, Node 24, Docker), variáveis de ambiente do back (tabela da seção 11 da spec, sem valores reais), como rodar localmente (Compose + back + front, exatamente como no Step 4 da Task 7) e como rodar os testes.

- [ ] **Step 3: Verificar**

Run (só com autorização do usuário, porque publica no GitHub): `git push` da branch de trabalho e `gh run watch`.
Expected: os dois jobs verdes no GitHub Actions. Sem autorização para push, registrar a verificação do CI como pendente no resumo final, em vez de dá-la como feita.

- [ ] **Step 4: Commit**

```bash
git add .github README.md
git commit -m "ci: testes de backend e frontend no GitHub Actions; README de desenvolvimento"
```
