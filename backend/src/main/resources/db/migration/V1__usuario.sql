create table usuario (
    id            uuid primary key,
    nome          text not null,
    email         text not null,
    papel         text not null check (papel in ('PERSONAL', 'ALUNO')),
    criado_em     timestamptz not null default now(),
    atualizado_em timestamptz not null default now()
);

create unique index usuario_email_lower_uk on usuario (lower(email));

alter table usuario enable row level security;
