# App Personal Trainer — Design (v1)

**Data:** 2026-10-18
**Status:** aguardando revisão

## 1. Objetivo

App web responsivo (prioridade mobile) para **um personal trainer** acompanhar os treinos e o desempenho dos seus alunos de academia. Uso real, com alunos reais.

**Critério de sucesso:** o personal abre o app e, em poucos segundos, vê quem está treinando, quem sumiu e como as cargas estão evoluindo, sem planilha nem WhatsApp. O aluno registra o treino no celular durante a sessão, sem atrito, mesmo com internet instável.

## 2. Escopo

### Dentro da v1
- Dois papéis com login: **Personal** e **Aluno**.
- Personal cadastra alunos; aluno recebe **convite por e-mail** e define a própria senha.
- Catálogo de exercícios do personal (com semente inicial e link de vídeo opcional).
- Fichas de treino com divisão **A/B/C…**, uma ficha ativa por aluno.
- Aluno registra a execução: carga, séries e repetições por exercício, com tolerância a queda de conexão.
- Desempenho: evolução de carga por exercício, frequência/aderência e dias sem treinar.
- Dashboard do personal com o resumo de todos os alunos.

### Fora da v1 (possíveis evoluções)
Modelos de ficha reutilizáveis, avaliação física (peso, medidas, % de gordura), percepção de esforço (RPE) e comentários, notificações push, chat, pagamentos, app nativo, offline completo (PWA), múltiplos personais, internacionalização.

### Premissas
- Existe **um único personal**. Mesmo assim, `aluno.personal_id` e `exercicio.personal_id` existem para permitir múltiplos personais no futuro sem refazer o modelo.
- Interface somente em pt-BR. Fuso horário de referência: `America/Sao_Paulo`.
- O aluno pode registrar valores diferentes dos prescritos; o personal vê os dois.

## 3. Arquitetura

```
[Celular/Desktop] ──HTTPS──> Angular (Vercel)
        │                         │
        │ login/senha             │ REST + JWT (Bearer)
        ▼                         ▼
  Supabase Auth  ──JWKS──>  Spring Boot API (Render, Virginia)
                                  │ JDBC (Session Pooler)
                                  ▼
                       PostgreSQL (Supabase, us-east-1)
```

- **Supabase Auth** cuida de credenciais, convite, redefinição de senha e emissão do JWT.
- O **Angular** usa `supabase-js` **somente para autenticação**. Todos os dados passam pela API Spring.
- A **API Spring** é *OAuth2 Resource Server*: valida o JWT pela chave pública do Supabase (JWKS em `https://<projeto>.supabase.co/auth/v1/.well-known/jwks.json`). O projeto Supabase deve usar **chaves de assinatura assimétricas** (JWT Signing Keys); se estiver no segredo HS256 legado, migrar no painel.
- O Spring é o **único dono das regras de negócio** e o único cliente do banco.
- Região: Render em Virginia e Supabase em `us-east-1`, lado a lado, para minimizar a latência API ↔ banco.

### Estrutura do repositório (monorepo)
```
/backend    Spring Boot (Java 21, Maven)
/frontend   Angular
/docs       specs e planos
```

## 4. Back-end

### Stack
Java 21, Spring Boot 3 (última versão estável no início da implementação), Spring Web, Spring Data JPA, Spring Security (OAuth2 Resource Server), Bean Validation, Flyway, Spring Boot Actuator, PostgreSQL driver. Testes: JUnit 5, Mockito, Testcontainers.

### Organização: pacotes por funcionalidade, camadas dentro de cada um
```
com.projetofit
 ├─ usuario/     vínculo com Supabase Auth, papéis, usuário atual
 ├─ aluno/       cadastro, convite, status
 ├─ exercicio/   catálogo
 ├─ ficha/       ficha, divisões, itens prescritos
 ├─ sessao/      registro de treinos executados
 ├─ desempenho/  consultas de evolução, aderência e dashboard (somente leitura)
 └─ comum/       segurança, CORS, tratamento global de erros, cliente Supabase Admin
```

Regras das camadas:
- **Controller:** só HTTP. Recebe e devolve **DTOs** (Java `record`), nunca entidades. Validação com Bean Validation.
- **Service:** regras de negócio, `@Transactional`, verificação de posse (o recurso pertence ao usuário atual?).
- **Repository:** Spring Data JPA; consultas agregadas de desempenho em JPQL ou SQL nativo.

### Usuário atual e papéis
- Cada request autenticado tem o `sub` (uuid) e o `email` do JWT. Um componente `UsuarioAtual` carrega o `usuario` correspondente e o seu papel.
- **Bootstrap do personal:** a conta do personal é criada manualmente no painel do Supabase. A variável `APP_PERSONAL_EMAIL` indica o seu e-mail. No primeiro request autenticado com esse e-mail, se não existir `usuario`, o sistema cria um com papel `PERSONAL`.
- JWT válido, mas sem `usuario` correspondente (e que não é o personal) → **403**.
- Aluno com status `INATIVO` → **403** em qualquer endpoint.
- Autorização por papel com `@PreAuthorize`, além da verificação de posse no service.

## 5. Modelo de dados

Todas as tabelas no schema `public`, criadas e versionadas pelo **Flyway** (`backend/src/main/resources/db/migration`). Chaves primárias `uuid` (geradas pelo banco com `gen_random_uuid()`), exceto `usuario.id`, que recebe o id do Supabase Auth. Datas com hora em `timestamptz`. Tabelas com `criado_em` / `atualizado_em`.

```
usuario
 id uuid PK                 = auth.users.id do Supabase
 nome text NOT NULL
 email text NOT NULL UNIQUE
 papel text NOT NULL        PERSONAL | ALUNO

aluno
 id uuid PK
 usuario_id uuid NOT NULL UNIQUE → usuario
 personal_id uuid NOT NULL → usuario
 telefone text, data_nascimento date, objetivo text, observacoes text
 status text NOT NULL       CONVIDADO | ATIVO | INATIVO

exercicio
 id uuid PK
 personal_id uuid NOT NULL → usuario
 nome text NOT NULL
 grupo_muscular text NOT NULL   PEITO | COSTAS | OMBROS | BICEPS | TRICEPS | ANTEBRACO |
                                QUADRICEPS | POSTERIORES | GLUTEOS | PANTURRILHA | ABDOMEN |
                                CARDIO | CORPO_INTEIRO
 video_url text NULL, observacao text NULL
 ativo boolean NOT NULL DEFAULT true
 UNIQUE (personal_id, nome)

ficha
 id uuid PK
 aluno_id uuid NOT NULL → aluno
 nome text NOT NULL, objetivo text NULL
 frequencia_semanal int NOT NULL CHECK 1..7
 data_inicio date NOT NULL, data_fim date NULL
 status text NOT NULL       ATIVA | ENCERRADA
 UNIQUE INDEX parcial (aluno_id) WHERE status = 'ATIVA'

divisao
 id uuid PK
 ficha_id uuid NOT NULL → ficha ON DELETE CASCADE
 nome text NOT NULL         ex.: "A - Peito e tríceps"
 ordem int NOT NULL

item_divisao
 id uuid PK
 divisao_id uuid NOT NULL → divisao ON DELETE CASCADE
 exercicio_id uuid NOT NULL → exercicio
 ordem int NOT NULL
 series int NOT NULL CHECK 1..20
 repeticoes text NOT NULL   ex.: "10" ou "8-12"
 carga_kg numeric(6,2) NULL CHECK >= 0
 descanso_seg int NULL CHECK >= 0
 observacao text NULL

sessao
 id uuid PK
 client_id uuid NOT NULL UNIQUE    gerado no celular (idempotência)
 aluno_id uuid NOT NULL → aluno
 divisao_id uuid NULL → divisao ON DELETE SET NULL
 divisao_nome text NOT NULL        cópia do nome no momento do treino
 iniciada_em timestamptz NOT NULL, finalizada_em timestamptz NOT NULL
 observacao text NULL

serie_executada
 id uuid PK
 sessao_id uuid NOT NULL → sessao ON DELETE CASCADE
 exercicio_id uuid NOT NULL → exercicio
 item_divisao_id uuid NULL → item_divisao ON DELETE SET NULL
 numero_serie int NOT NULL CHECK >= 1
 repeticoes int NOT NULL CHECK >= 0
 carga_kg numeric(6,2) NULL CHECK >= 0
```

Índices adicionais: `sessao (aluno_id, finalizada_em DESC)`, `serie_executada (exercicio_id)`, `ficha (aluno_id)`, `aluno (personal_id)`.

**RLS ativado e sem políticas** em todas as tabelas `public`, para que a API automática do Supabase (PostgREST) não exponha nada com a *anon key*. O Spring conecta com o usuário `postgres` (que ignora RLS) pela string do **Session Pooler** (porta 5432).

### Regras de dados
- **O histórico é imutável e independente da ficha.** `serie_executada` guarda exercício e valores reais; `sessao.divisao_nome` guarda o nome da divisão. Editar ou encerrar uma ficha não altera o histórico.
- **Exercícios não são apagados**, apenas desativados (`ativo = false`). Exercícios inativos não aparecem para novas fichas, mas continuam no histórico.
- **Fichas não são apagadas**, apenas encerradas.
- **Semente:** cerca de 40 exercícios básicos (cobrindo todos os grupos musculares) são inseridos para o personal **no bootstrap** (seção 4), na mesma transação que cria o seu `usuario`. A lista fica num arquivo de recurso (`backend/src/main/resources/semente/exercicios.json`). Não é uma migration Flyway porque as migrations rodam antes de o personal existir.

## 6. Regras de negócio

### Alunos e convite
1. `POST /api/alunos` (personal): cria o usuário no Supabase via API Admin (`POST /auth/v1/invite`, usando a `service_role`, com `redirectTo = {FRONT_URL}/definir-senha`). Com o id retornado, cria `usuario` (papel `ALUNO`) e `aluno` (status `CONVIDADO`) na mesma transação. Se a chamada ao Supabase falhar, nada é gravado.
2. E-mail já cadastrado (no banco ou no Supabase) → **409**.
3. O aluno clica no link, cai em `/definir-senha` no Angular e define a senha via `supabase-js`.
4. No primeiro request autenticado do aluno, se o status for `CONVIDADO`, ele passa a `ATIVO`.
5. `POST /api/alunos/{id}/reenviar-convite`: só para status `CONVIDADO`; caso contrário → **409**.
6. `PATCH /api/alunos/{id}/status`: personal alterna entre `ATIVO` e `INATIVO`. Aluno inativo não acessa o app e não aparece no dashboard (aparece num filtro "inativos").

"Esqueci minha senha" usa `supabase-js` (`resetPasswordForEmail`) com redirect para `/definir-senha`; não passa pela API.

### Fichas
- Criar uma ficha a torna **ATIVA**. Se o aluno já tiver ficha ativa, ela é **encerrada automaticamente na mesma transação** (o front pede confirmação antes).
- A ficha é criada e editada **inteira** (ficha → divisões → itens) num único request. Na edição (`PUT`), divisões e itens com `id` são atualizados, os sem `id` são criados e os ausentes são removidos.
- Só fichas `ATIVA` podem ser editadas. Editar uma `ENCERRADA` → **409**.
- `data_fim` é opcional. Ficha ativa com `data_fim` no passado continua ativa, mas aparece como **"vencida"** no dashboard.
- Validações: pelo menos 1 divisão, cada divisão com pelo menos 1 item, exercícios do próprio personal e ativos.

### Sessões (registro do aluno)
- A tela "Treino de hoje" sugere a **próxima divisão**: a seguinte (por `ordem`) à divisão do último treino do aluno nessa ficha. Sem treinos na ficha, sugere a primeira. Depois da última, volta para a primeira. O aluno pode escolher outra.
- `POST /api/sessoes` envia a sessão completa (divisão, horários, séries). Regras:
  - Se o `client_id` já existir para esse aluno → devolve **200** com a sessão existente (idempotente). Se existir para outro aluno → **409**.
  - A divisão deve pertencer a uma ficha do próprio aluno (ativa ou encerrada, porque a ficha pode ter sido encerrada enquanto o aluno treinava offline).
  - `finalizada_em >= iniciada_em`; `finalizada_em` não pode estar mais de 5 minutos no futuro.
  - Pelo menos 1 série executada.
- Sessões não são editadas nem apagadas na v1.

### Métricas de desempenho
- **Evolução por exercício:** para cada sessão do aluno com aquele exercício, um ponto com `data`, `cargaMaxima` (maior `carga_kg`) e `volume` (Σ repetições × carga). Período padrão: últimos 6 meses (parâmetros `de`/`ate`).
- **Aderência semanal:** semanas de segunda a domingo (`America/Sao_Paulo`). Para cada uma das últimas N semanas (padrão 8): `realizados` (sessões finalizadas na semana), `planejado` (`frequencia_semanal` da ficha ativa naquela semana; 0 se não havia ficha) e `percentual` (`realizados / planejado`, limitado a 100%, nulo se `planejado = 0`).
- **Dias sem treinar:** dias corridos entre hoje e a data da última `finalizada_em`. Nulo se o aluno nunca treinou.
- **Alerta "sumiu":** dias sem treinar > 7, ou ficha ativa há mais de 7 dias sem nenhum treino.

## 7. API REST

Base `/api`. JSON. Datas em ISO-8601. Listas longas paginadas (`page`, `size`) no formato padrão do Spring Data.

| Método e rota | Papel | Descrição |
|---|---|---|
| `GET /me` | ambos | dados do usuário atual e papel |
| `GET /alunos?status=` | personal | lista de alunos |
| `POST /alunos` | personal | cria aluno e envia convite |
| `GET /alunos/{id}` | personal | detalhe |
| `PUT /alunos/{id}` | personal | edita dados (e-mail não é editável na v1) |
| `PATCH /alunos/{id}/status` | personal | ativa/inativa |
| `POST /alunos/{id}/reenviar-convite` | personal | reenvia convite |
| `GET /exercicios?ativo=&grupo=` | personal | catálogo |
| `POST /exercicios` / `PUT /exercicios/{id}` | personal | cria/edita (inclui `ativo`) |
| `GET /alunos/{id}/fichas` | personal | fichas do aluno (ativa e encerradas) |
| `POST /alunos/{id}/fichas` | personal | cria ficha completa (encerra a anterior) |
| `GET /fichas/{id}` | personal | ficha completa |
| `PUT /fichas/{id}` | personal | edita ficha completa |
| `POST /fichas/{id}/encerrar` | personal | encerra |
| `GET /me/ficha-ativa` | aluno | ficha ativa completa + `proximaDivisaoId` |
| `POST /sessoes` | aluno | registra sessão (idempotente por `clientId`) |
| `GET /me/sessoes` | aluno | histórico próprio (paginado) |
| `GET /alunos/{id}/sessoes` | personal | histórico do aluno (paginado) |
| `GET /sessoes/{id}` | ambos | detalhe (aluno só a própria) |
| `GET /alunos/{id}/desempenho/evolucao?exercicioId=&de=&ate=` | ambos* | série temporal |
| `GET /alunos/{id}/desempenho/aderencia?semanas=` | ambos* | aderência semanal |
| `GET /alunos/{id}/desempenho/exercicios` | ambos* | exercícios que o aluno já executou (para o seletor do gráfico) |
| `GET /dashboard` | personal | por aluno ativo: nome, realizados/planejado na semana atual, dias sem treinar, ficha vencida, alerta "sumiu" |

\* O aluno só acessa o próprio `id`; outro `id` → **404**.

### Erros
`@RestControllerAdvice` global, respostas em **Problem Details (RFC 7807)**:

| Situação | Status |
|---|---|
| Validação (Bean Validation, regras de formato) | 400, com campo `errors` (campo → mensagem) |
| Sem token / token inválido ou expirado | 401 |
| Papel sem permissão, usuário inativo ou desconhecido | 403 |
| Recurso inexistente **ou** de outro usuário | 404 |
| Conflito de estado (e-mail duplicado, editar ficha encerrada, reenviar convite de aluno ativo) | 409 |
| Falha ao chamar o Supabase Admin | 502 |
| Erro inesperado | 500 (sem detalhes internos; log com stack trace) |

## 8. Front-end

### Stack
Angular (última versão estável no início da implementação), standalone components, signals, Angular Material, `@supabase/supabase-js`, Chart.js via `ng2-charts`. Layout **mobile-first**, com breakpoints do Angular CDK (`BreakpointObserver`).

### Organização
```
src/app
 ├─ core/       auth (supabase client, AuthService), interceptor HTTP, guards, tratamento de erros
 ├─ shared/     componentes e pipes reutilizáveis
 ├─ personal/   dashboard, alunos, fichas, exercicios (rotas lazy /personal/**)
 ├─ aluno/      treino, execucao, historico, evolucao (rotas lazy /aluno/**)
 └─ publico/    login, definir-senha, esqueci-senha
```

- **Guards:** `authGuard` (logado) e `papelGuard` (papel obtido de `GET /me`). Depois do login, o usuário é levado para `/personal/dashboard` ou `/aluno/treino`.
- **Interceptor:** anexa `Authorization: Bearer <token>` (o `supabase-js` renova o token sozinho); em 401 faz logout e redireciona ao login; outros erros viram snackbar com o `title`/`detail` do Problem Details.
- **Navegação:** no celular, barra inferior (bottom navigation); no desktop, menu lateral.

### Telas
**Personal:** Dashboard (cards/lista com alertas), Lista de alunos (busca, filtro de status, novo aluno), Detalhe do aluno (dados, ficha ativa, histórico de sessões, gráficos de evolução e aderência), Editor de ficha (divisões e itens, com seleção de exercício por grupo muscular), Catálogo de exercícios.

**Aluno:** Treino de hoje (ficha ativa, divisão sugerida, troca de divisão), Execução (um card por exercício com séries prescritas; para cada série, campos de carga e repetições pré-preenchidos com a prescrição e um botão de concluir, com alvos de toque grandes), Histórico, Minha evolução.

### Sessão com tolerância a queda de conexão
- Ao iniciar, o front gera um `clientId` (`crypto.randomUUID()`) e cria a sessão no `localStorage` (chave `sessao-em-andamento`).
- Cada série concluída atualiza o `localStorage` imediatamente.
- Se o app for fechado ou recarregado, a sessão em andamento é restaurada ao reabrir.
- Ao finalizar, a sessão vai para uma fila `sessoes-pendentes` e o front tenta o `POST /api/sessoes`. Em caso de sucesso (200/201), remove da fila. Em falha de rede ou 5xx, mantém na fila, mostra o aviso "treino pendente de envio" e tenta de novo ao voltar a conexão (evento `online`), ao abrir o app e por um botão "enviar agora". Em 4xx (exceto 401), remove da fila e mostra o erro.
- Essa lógica fica num serviço isolado (`SessaoLocalService`) com testes unitários próprios.

## 9. Segurança
- O Spring valida todo JWT (assinatura via JWKS, `iss`, expiração) e filtra toda consulta por dono.
- Segredos (`SUPABASE_SERVICE_ROLE_KEY`, credenciais do banco) só em variáveis de ambiente do Render; nunca no repositório. O front recebe apenas `SUPABASE_URL` e a *anon key* (públicas por design).
- CORS: apenas o domínio de produção na Vercel e `http://localhost:4200`.
- RLS ativado sem políticas (seção 5).
- No Supabase: cadastro público desativado (*Disable signups*), redirect URLs restritas ao domínio do front e SMTP próprio (**Resend**) para os convites, porque o SMTP embutido tem limite muito baixo.

## 10. Testes
- **Back:**
  - Unitários dos services (JUnit 5 + Mockito), com foco nas regras da seção 6.
  - Integração com **Testcontainers** (PostgreSQL): repositórios, migrations Flyway, consultas de desempenho (evolução, aderência, dashboard) e endpoints críticos.
  - Permissões: aluno não acessa dados de outro aluno; aluno não acessa endpoints do personal; usuário inativo recebe 403. JWT simulado com `spring-security-test` (`jwt()`).
  - Cliente Supabase Admin testado com servidor HTTP simulado (WireMock ou `MockRestServiceServer`).
- **Front:** unitários de services, guards, interceptor e, com prioridade, `SessaoLocalService` (salvar, restaurar, fila, reenvio, idempotência).
- Não há testes E2E na v1.

## 11. Configuração, ambientes e deploy

| Variável (back) | Uso |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` ou `prod` |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | conexão Postgres (Session Pooler em prod) |
| `SUPABASE_URL` | base do Auth/JWKS e do Admin API |
| `SUPABASE_SERVICE_ROLE_KEY` | convites (Admin API) |
| `APP_PERSONAL_EMAIL` | bootstrap do personal |
| `APP_FRONT_URL` | CORS e `redirectTo` dos convites |

- **Perfil `dev`:** Postgres local via `docker-compose.yml` na raiz e um projeto Supabase de desenvolvimento para Auth. **Perfil `prod`:** Supabase de produção.
- **Front:** `environment.ts` com `apiUrl`, `supabaseUrl` e `supabaseAnonKey`.
- **Render:** Web Service (Starter, região Virginia) com `Dockerfile` multi-stage em `/backend`, health check em `/actuator/health` e deploy automático a cada push na `main`.
- **Vercel:** root `/frontend`, build Angular, *rewrite* de todas as rotas para `index.html`.
- **Supabase:** região `us-east-1`, plano Free. O projeto pausa após cerca de 1 semana sem acesso.
- **CI (GitHub Actions):** em push e PR, roda os testes do back (`mvn verify`, com Docker para Testcontainers) e do front (`ng test` headless) e o build.

## 12. Ordem de entrega sugerida
1. **Fundação:** monorepo, esqueleto do back (segurança JWT, Flyway, tratamento de erros, `/me`, bootstrap do personal), esqueleto do front (login, guards, interceptor, layouts), Docker Compose, CI.
2. **Alunos e convite.**
3. **Catálogo de exercícios** (com semente).
4. **Fichas** (editor completo).
5. **Sessões do aluno** (treino de hoje, execução, fila offline, histórico).
6. **Desempenho e dashboard** (evolução, aderência, alertas, gráficos).
7. **Deploy** (Render, Vercel, Supabase de produção, SMTP Resend).
