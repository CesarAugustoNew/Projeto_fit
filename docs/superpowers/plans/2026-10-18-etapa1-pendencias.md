# Etapa 1 — Pendências para as próximas etapas

Itens levantados nas revisões da Etapa 1 que ficaram para depois. Ler antes de escrever o plano da Etapa 2.

## Antes do merge (dependem do usuário)
- Preencher `frontend/src/environments/environment*.ts` com a URL e a anon key do Supabase de desenvolvimento e fazer o teste manual de login (personal → `/personal/dashboard`; barra inferior no modo celular; `Sair`; aluno/personal em rota do outro papel).
- Abrir PR para `main` para o GitHub Actions rodar pela primeira vez.
- Quem já subiu o Compose antes da correção da V1: `docker compose down -v` (o checksum do Flyway mudou).

## Incluir no plano da Etapa 2 (Alunos e convite)
- Títulos pt-BR e `errors` para `HandlerMethodValidationException`, `HttpMessageNotReadableException` e `NoResourceFoundException` antes do primeiro endpoint com entrada.
- Aluno desativado no meio da sessão: hoje o `erroInterceptor` ignora 403 e o `papelGuard` usa o usuário em cache, então a pessoa só recebe 403 silenciosos.
- `papelGuard` em 0/5xx cancela a navegação; na primeira navegação a tela fica em branco até recarregar. Criar uma tela "tentar de novo".
- Generalizar o `MigracoesIT`: toda tabela `public` com RLS ativo e nenhuma policy.
- Teste do 502 conferindo `detail` (cliente Supabase Admin).

## Melhorias menores (sem prazo)
- Back: validador JWT com `JwtValidators.createDefaultWithValidators`; teste de colisão concorrente real no bootstrap; `findById` duplicado no `/api/me`; `WWW-Authenticate` no 401 vindo do advice; erros fora da cadeia MVC em Problem Details (`ErrorController` próprio); `argLine` com `@{argLine}` se entrar JaCoCo; `.editorconfig`.
- Front: testes dos ramos não cobertos do `AuthService`; `http.verify()` nos specs HTTP; `ariaCurrentWhenActive="page"` na navegação; `Sair` com `try/finally`; mensagens específicas do Supabase em `DefinirSenha` (senha fraca/igual); SCSS duplicado das telas públicas; bundle inicial ~635 kB acima do orçamento de 500 kB (lazy-load do supabase-js ou revisar o orçamento).
- Etapa 7 (deploy): `apiUrl` de produção (hoje `''`; endurecer `ehChamadaApi` com `new URL(...)` e comparação de origin); CI com `permissions: contents: read` e `concurrency`.
