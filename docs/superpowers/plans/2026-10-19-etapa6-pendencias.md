# Etapa 6 — Pendências para a próxima etapa

Itens levantados nas revisões da Etapa 6 que ficaram para depois. Ler junto com as pendências das Etapas 1–5 antes do plano da Etapa 7 (Deploy).

## Teste manual (usuário)
- Como personal: dashboard com os alunos ativos, treinos da semana, último treino, "Sumiu" e "Ficha vencida"; no detalhe do aluno, seção "Desempenho" com aderência semanal e evolução por exercício (trocar exercício, alternar Carga/Volume).
- Como aluno: menu "Evolução" com os próprios gráficos.
- A migration V6 (índice) é só de acréscimo.

## Incluir no plano da Etapa 7 (Deploy)
- O orçamento do bundle inicial (500 kB) continua estourado (~680 kB); o Chart.js está fora do inicial. Decidir no deploy: reduzir (trocar `supabase-js` completo por `auth-js`, ver pendências da Etapa 2) ou ajustar o orçamento conscientemente.
- Pendências antigas que dependem do ambiente de produção: `APP_FRONT_URL` com barra final (CORS e `redirect_to`), SMTP próprio (Resend), `apiUrl` de produção e endurecimento de `ehChamadaApi`, CI com `permissions`/`concurrency`.

## Melhorias menores (sem prazo)
- Back: testes do limite exato de 7 dias e de ficha não vencida; testes noturnos/fuso e `de/ate` explícitos na evolução; `exercicioId` ausente; projeções `record` no lugar de `Object[]`; cálculo do início da semana ainda repetido em `DesempenhoService.aderencia`; a frequência de semanas passadas usa a frequência atual da ficha (não há histórico).
- Front: exercício padrão do painel é o primeiro por nome (poderia ser o mais recente); a medida Carga/Volume volta a Carga depois de um erro ao carregar; specs de gráfico silenciam `console.error` de forma ampla (filtrar só "Failed to create chart" como no painel); `GrupoMuscular` importado de `personal/` por `shared/`; "1/1 treinos" no plural; rótulos das barras não dizem que são o início da semana; `alvo` do painel lido só no `ngOnInit`.
