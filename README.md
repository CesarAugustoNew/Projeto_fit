# Projeto Fit

Aplicativo web para personal trainers acompanharem seus alunos: cadastro e convite de alunos, catálogo de exercícios, fichas de treino, execução de treinos pelo aluno e acompanhamento de desempenho. O repositório é um monorepo com o back-end (`backend/`, Spring Boot 4.1 e Java 21, PostgreSQL via Flyway) e o front-end (`frontend/`, Angular 22). A autenticação usa o Supabase Auth; o back valida o JWT emitido por ele. O desenho completo está em `docs/superpowers/specs/`.

## Pré-requisitos

- Java 21 ou superior
- Node 24 (com npm)
- Docker (Postgres local via Compose e Testcontainers nos testes)
- Um projeto Supabase de desenvolvimento

## Variáveis de ambiente do back-end

| Variável | Uso |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` ou `prod` |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | conexão Postgres (Session Pooler em prod) |
| `SUPABASE_URL` | base do Auth/JWKS e do Admin API |
| `SUPABASE_SERVICE_ROLE_KEY` | convites (Admin API); só a partir da Etapa 2 |
| `APP_PERSONAL_EMAIL` | bootstrap do personal |
| `APP_FRONT_URL` | CORS e `redirectTo` dos convites (padrão `http://localhost:4200`) |

Nunca versione segredos (`SUPABASE_SERVICE_ROLE_KEY`, senha do banco). A URL do Supabase e a anon key do front são públicas.

## Configuração do Supabase (desenvolvimento)

No projeto Supabase de desenvolvimento:

- Habilitar as JWT Signing Keys assimétricas.
- Authentication: deixar "Disable signups" ligado e "Confirm email" ligado.
- Permitir a redirect URL `http://localhost:4200/definir-senha`.
- Criar manualmente a conta do personal em Authentication → Users, com o mesmo e-mail de `APP_PERSONAL_EMAIL`.

Os cadastros precisam ficar desabilitados porque o bootstrap do personal confia no e-mail do token: se qualquer pessoa pudesse se cadastrar com esse e-mail, viraria personal.

Os arquivos `frontend/src/environments/environment*.ts` trazem placeholders (`https://SEU-PROJETO.supabase.co` e `SUA_ANON_KEY`). Substitua pelos valores do projeto Supabase de desenvolvimento antes de tentar o login.

## Rodando localmente

Com o back rodando (`docker compose up -d`, depois `cd backend && SPRING_PROFILES_ACTIVE=dev SUPABASE_URL=<url-dev> APP_PERSONAL_EMAIL=<email> ./mvnw spring-boot:run`) e o front (`cd frontend && npx ng serve`), acesse `http://localhost:4200`.

## Testes

```bash
# back-end (inclui testes com Testcontainers; exige Docker)
cd backend && ./mvnw -B verify

# front-end
cd frontend && npm ci && npx ng test --watch=false && npx ng build
```

O mesmo roda no GitHub Actions (`.github/workflows/ci.yml`) em push e pull request para `main`.
