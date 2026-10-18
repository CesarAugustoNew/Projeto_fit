create table aluno (
    id              uuid primary key,
    usuario_id      uuid not null unique references usuario (id),
    personal_id     uuid not null references usuario (id),
    telefone        text,
    data_nascimento date,
    objetivo        text,
    observacoes     text,
    status          text not null check (status in ('CONVIDADO', 'ATIVO', 'INATIVO')),
    criado_em       timestamptz not null default now(),
    atualizado_em   timestamptz not null default now()
);

create index aluno_personal_id_idx on aluno (personal_id);

alter table aluno enable row level security;
