<div align="center">

<img src="frontend/public/favicon.svg" alt="Logo do Projeto Fit: um raio azul" width="88" />

# Projeto Fit

**App web para um personal trainer acompanhar os treinos e a evolução dos seus alunos.**

![Frontend](https://img.shields.io/badge/Frontend-Angular_22-DD0031?logo=angular&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?logo=typescript&logoColor=white)
![Angular Material](https://img.shields.io/badge/UI-Angular_Material-1F6FEB?logo=materialdesign&logoColor=white)
![Chart.js](https://img.shields.io/badge/Gr%C3%A1ficos-Chart.js-FF6384?logo=chartdotjs&logoColor=white)

![Backend](https://img.shields.io/badge/Backend-Spring_Boot_4.1-6DB33F?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-OAuth2_JWT-6DB33F?logo=springsecurity&logoColor=white)
![Flyway](https://img.shields.io/badge/Migra%C3%A7%C3%B5es-Flyway-CC0200?logo=flyway&logoColor=white)

![PostgreSQL](https://img.shields.io/badge/Banco-PostgreSQL-4169E1?logo=postgresql&logoColor=white)
![Supabase](https://img.shields.io/badge/Auth-Supabase-3FCF8E?logo=supabase&logoColor=white)
![JUnit](https://img.shields.io/badge/Testes-JUnit_5-25A162?logo=junit5&logoColor=white)
![Testcontainers](https://img.shields.io/badge/Testcontainers-Docker-2496ED?logo=docker&logoColor=white)
![Vitest](https://img.shields.io/badge/Vitest-6E9F18?logo=vitest&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/CI-GitHub_Actions-2088FF?logo=githubactions&logoColor=white)

![Deploy front](https://img.shields.io/badge/Deploy-Vercel-000000?logo=vercel&logoColor=white)
![Deploy API](https://img.shields.io/badge/Deploy-Render-46E3B7?logo=render&logoColor=black)

</div>

<p align="center">
  <img src="docs/imagens/login-claro.png" alt="Tela de login no modo claro" width="49%" />
  <img src="docs/imagens/login-escuro.png" alt="Tela de login no modo escuro" width="49%" />
</p>

## Sobre

O Projeto Fit dá ao personal trainer mais controle sobre seus alunos. Em um só lugar, ele monta as fichas de treino, acompanha quem está treinando e quem sumiu, e vê como as cargas e a frequência de cada aluno estão evoluindo. Do outro lado, o aluno segue a ficha e registra o treino no celular durante a sessão, mesmo com a internet da academia oscilando, e tudo chega ao personal.

A interface é em português, pensada primeiro para o celular, e tem modo claro e escuro.

## Funcionalidades

**Para o personal**

- Dashboard com todos os alunos: treinos feitos na semana, último treino e alertas de quem sumiu ou está com a ficha vencida.
- Cadastro de alunos com convite por e-mail; o aluno define a própria senha.
- Catálogo de exercícios por grupo muscular, com link de vídeo opcional.
- Fichas de treino com divisões (A, B, C…), séries, repetições, carga e descanso prescritos.
- Desempenho de cada aluno: aderência semanal à ficha e evolução de carga e volume por exercício.
- Histórico de treinos de cada aluno, com as séries, cargas e repetições registradas.

**Para o aluno**

- Treino do dia com a próxima divisão sugerida.
- Registro de carga e repetições série a série, salvo no aparelho: o treino não se perde se a conexão cair e é enviado quando ela voltar.
- Histórico dos próprios treinos e gráficos da própria evolução.

## Destaques técnicos

- **API em camadas por funcionalidade** (`aluno`, `exercicio`, `ficha`, `sessao`, `desempenho`…): controllers só com DTOs, regras de negócio e verificação de posse nos services.
- **Autenticação com Supabase Auth** e autorização no Spring: a API é um OAuth2 Resource Server que valida o JWT pela chave pública (JWKS), e o papel do usuário vem do banco, nunca do token.
- **Dados isolados por usuário:** toda consulta é filtrada pelo usuário autenticado; o banco tem RLS ligado sem políticas, então os dados só são acessíveis pela API.
- **Registro de treino tolerante a falhas:** fila local no navegador e envio idempotente, para que um treino reenviado nunca seja gravado duas vezes.
- **Esquema versionado com Flyway** e erros no padrão Problem Details, com mensagens em português.
- **Testes de integração com Testcontainers** (PostgreSQL real) no back e Vitest no front, rodando no GitHub Actions a cada push.
- **Tema próprio sobre o Material 3**: paleta azul gerada a partir da cor da marca e modo claro/escuro que segue o sistema ou a escolha do usuário.

## Arquitetura

```
[Celular / Desktop] ──HTTPS──> Angular (Vercel)
        │                          │
        │ login e senha            │ REST + JWT (Bearer)
        ▼                          ▼
  Supabase Auth ──JWKS──>  Spring Boot API (Render)
                                   │
                                   ▼
                        PostgreSQL (Supabase)
```

O Angular usa o Supabase só para autenticação; todos os dados passam pela API, que é a única dona das regras de negócio e a única cliente do banco.

## Estrutura do repositório

```
backend/    API Spring Boot (Java 21, Maven, Flyway)
frontend/   App Angular (standalone components, signals, Angular Material)
docs/       imagens do README
```

## Status

Em desenvolvimento. As funcionalidades da primeira versão estão prontas; a próxima etapa é o deploy (Vercel, Render e Supabase).
