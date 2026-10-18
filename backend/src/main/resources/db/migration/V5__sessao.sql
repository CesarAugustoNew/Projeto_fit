create table sessao (
    id             uuid primary key default gen_random_uuid(),
    client_id      uuid not null unique,
    aluno_id       uuid not null references aluno (id),
    divisao_id     uuid references divisao (id) on delete set null,
    divisao_nome   text not null,
    iniciada_em    timestamptz not null,
    finalizada_em  timestamptz not null,
    observacao     text,
    criado_em      timestamptz not null default now()
);

create index sessao_aluno_finalizada_idx on sessao (aluno_id, finalizada_em desc);

create table serie_executada (
    id               uuid primary key default gen_random_uuid(),
    sessao_id        uuid not null references sessao (id) on delete cascade,
    exercicio_id     uuid not null references exercicio (id),
    item_divisao_id  uuid references item_divisao (id) on delete set null,
    ordem            int not null,
    numero_serie     int not null check (numero_serie >= 1),
    repeticoes       int not null check (repeticoes >= 0),
    carga_kg         numeric(6, 2) check (carga_kg >= 0)
);

create index serie_executada_exercicio_idx on serie_executada (exercicio_id);

alter table sessao enable row level security;
alter table serie_executada enable row level security;
