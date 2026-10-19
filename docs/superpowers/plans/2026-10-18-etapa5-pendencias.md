# Etapa 5 — Pendências para as próximas etapas

Itens levantados nas revisões da Etapa 5 que ficaram para depois. Ler junto com as pendências das Etapas 1–4 antes do plano da Etapa 6.

## Teste manual (usuário)
- Fluxo sem internet: abrir "Treino de hoje", ligar o modo avião, começar, marcar séries, finalizar ("Treino salvo…"), desligar o modo avião e conferir no Histórico e na próxima sugestão de divisão.
- Recarregar a página totalmente sem internet cai em "Servidor indisponível" (não há service worker; PWA está fora da spec) — o treino fica guardado e volta quando a conexão voltar.

## Incluir no plano da Etapa 6 (Desempenho e dashboard)
- Evolução/aderência usam `serie_executada` (carga, repetições) e `sessao.finalizada_em`; `ficha.encerrada_em` define a ficha ativa de cada semana.
- `sessao.divisao_nome` agora é sempre o nome enviado pelo aparelho (nome no momento do treino); `item_divisao_id` só é ligado se o exercício for o mesmo.
- `SessoesDoAluno` lê o `alunoId` só no `ngOnInit`: se o dashboard navegar de um aluno para outro reaproveitando o componente, trocar para `effect`/`computed`.
- Telas novas que listem sessões devem reutilizar `acrescentarPagina` (sem duplicar linhas) e a ordenação fixa do back (`finalizadaEm desc, id desc`).

## Melhorias menores (sem prazo)
- Fila local: duas abas podem sobrescrever a fila uma da outra (listener de `storage`); sem nenhum `localStorage` o treino não finaliza (fallback de envio direto); possível entrada duplicada em "com erro"; snackbar "Treino salvo…" cobre "Sua conta não tem acesso ao app." num 403 durante o envio; 408/429 em envio de fundo ainda mostram snackbar; sair (logout) não avisa que há treino em andamento/pendente.
- Execução: aria-labels repetidos por série; minutos com atraso de até 59 s; reps fracionárias não reescritas na tela; Concluir pode usar o valor antigo se o aluno digitar reps fora da faixa e tocar antes do `change`.
- Back: N+1 nas séries ao montar resumos; outras violações de integridade (FK concorrente) viram 500 (o front tenta de novo); testes faltando (idempotência antes da validação de horário, limite de 400 séries, 403 do personal em `/api/me/sessoes`, cascade sessao→série).
- Front: `shared/sessao` importa `Ficha` de `personal/fichas`; `formatarCarga` duplicado; botões de ação da tela de treino com ~40px; falha na 1ª carga do histórico do aluno (personal) sem botão de tentar de novo; listas de sessões do aluno e do personal quase duplicadas.
