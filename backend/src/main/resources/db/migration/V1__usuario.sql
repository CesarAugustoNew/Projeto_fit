create table usuario (
    id            uuid primary key,
    nome          text not null,
    email         text not null unique,
    papel         text not null check (papel in ('PERSONAL', 'ALUNO')),
    criado_em     timestamptz not null default now(),
    atualizado_em timestamptz not null default now()
);

alter table usuario enable row level security;
