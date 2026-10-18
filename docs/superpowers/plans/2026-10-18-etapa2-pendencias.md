# Etapa 2 — Pendências para as próximas etapas

Itens levantados nas revisões da Etapa 2 que ficaram para depois. Ler junto com `2026-10-18-etapa1-pendencias.md` antes do plano da Etapa 3.

## Antes do merge / teste manual (usuário)
- A API agora exige `SUPABASE_SERVICE_ROLE_KEY` (Secret key `sb_secret_...`).
- Se o banco local já tiver aplicado uma versão anterior da V2: `docker compose down -v` (checksum do Flyway).
- O SMTP padrão do Supabase só entrega para membros do projeto: convide um e-mail seu.
- Aluno desativado só é deslogado na próxima chamada à API. Como a área do aluno ainda não chama a API, no teste isso acontece ao recarregar a página (resolve sozinho na Etapa 5).

## Incluir no plano da Etapa 3 (Catálogo de exercícios)
- A semente de exercícios entra no bootstrap do personal em `UsuarioAtualResolver`, que agora também depende de `AlunoAcesso` (ciclo de pacotes `usuario` ↔ `aluno`). Ao estender o resolver, considerar uma interface no pacote `usuario` implementada em `aluno`.
- Títulos pt-BR para 405, 415 e parâmetro obrigatório ausente no `TratadorGlobalDeErros`; chave do `errors` deve usar o nome do `@RequestParam("x")` se algum parâmetro for renomeado.

## Melhorias menores (sem prazo)
- Back: ordenação de alunos por nome com collation pt-BR (o Postgres alpine ordena por bytes: "Ângela" depois de "Zé"); 400 do Supabase no convite mapeado para "verifique o e-mail" em vez de 502; mapear só `error_code` `email_exists`/`user_already_exists` para "E-mail já cadastrado."; DELETE 404 no Supabase tratado como sucesso na compensação; `SupabaseAdminClient` sem catch de NPE/IAE (checar resposta explicitamente) e com testes de I/O, resposta sem `id` e 4xx não-422; testes: `MigracoesIT` com `try/finally`, fixtures de aluno ATIVO com `primeiro_acesso_em`, helper `gravarAluno` compartilhado, resolver verificando que `liberar` não roda para PERSONAL; `PATCH status=CONVIDADO` validar depois do lookup.
- Front: lista de alunos com botão "tentar de novo" no erro e testes dos estados extras (carregando/erro/resposta atrasada); formulário com validação de nome só com espaços e bloqueio dos campos enquanto carrega; detalhe com botões desabilitados enquanto o diálogo de confirmação está aberto; `ROTULO`/tipos (`Exclude<StatusAluno,'CONVIDADO'>`).
- Bundle inicial ~660 kB (orçamento 500 kB): ~270 kB são do supabase-js, mas só o `auth-js` (~126 kB) é usado → importar só `@supabase/auth-js` ou carregar o cliente sob demanda; o `MatSnackBar` eager arrasta o overlay do CDK compartilhado com o `MatDialog`.
- Etapa 7: normalizar `APP_FRONT_URL` com barra final (afeta CORS e o `redirect_to` do convite); SMTP próprio (Resend) para convidar alunos reais.
