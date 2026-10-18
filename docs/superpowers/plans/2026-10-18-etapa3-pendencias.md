# Etapa 3 — Pendências para as próximas etapas

Itens levantados nas revisões da Etapa 3 que ficaram para depois. Ler junto com as pendências das Etapas 1 e 2 antes do plano da Etapa 4.

## Incluir no plano da Etapa 4 (Fichas)
- O editor de ficha deve listar só exercícios **ativos** (`GET /api/exercicios` já devolve só ativos por padrão) e a validação do back deve recusar exercício inativo ou de outro personal (spec §6).
- A busca de exercício no editor deve usar `normalizarBusca` (`frontend/src/app/shared/texto/normalizar-busca.ts`) para ignorar acentos, como nas listas.
- Filtros das listas (Alunos/Exercícios) se perdem ao voltar da edição; avaliar guardar o filtro na URL (query params) se o editor de ficha navegar de/para essas telas.

## Melhorias menores (sem prazo)
- Back: unicidade do nome de exercício ignora maiúsculas mas não acentos ("Triceps testa" × "Tríceps testa" podem coexistir — a busca sem acento reduz o risco); `ExercicioService.gravar` trata qualquer violação de integridade como "nome repetido"; `existsBy...IgnoreCase` usa `upper()` e o índice `lower()` (diferem só em Unicode raro); teste de arquitetura recursivo e cobrindo `import static`/nomes qualificados; runner da semente sem teste de "continua após falha"; sem teste direto de integridade do `exercicios.json`; imports fora de ordem.
- Front: ids sem `encodeURIComponent` nos serviços; testes faltando (textos de `ROTULO_GRUPO`, casos extras de `linkSeguro`, toggle+grupo juntos, guarda de resposta atrasada, estado de erro, campos desabilitados durante a carga do formulário); `linkSeguro` aceita `https://` sem host.
- Bundle inicial ~675 kB (orçamento 500 kB): componentes Material usados sob demanda (`mat-select`, `MatDialog`) puxam mais do overlay do CDK que já está no chunk inicial por causa do `MatSnackBar`; o maior ganho continua sendo trocar o `supabase-js` completo só pelo `auth-js` (ver pendências da Etapa 2).
