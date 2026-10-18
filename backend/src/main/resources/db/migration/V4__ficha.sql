create table ficha (
    id                  uuid primary key default gen_random_uuid(),
    aluno_id            uuid not null references aluno (id),
    nome                text not null,
    objetivo            text,
    frequencia_semanal  int not null check (frequencia_semanal between 1 and 7),
    data_inicio         date not null,
    data_fim            date,
    status              text not null check (status in ('ATIVA', 'ENCERRADA')),
    criado_em           timestamptz not null default now(),
    atualizado_em       timestamptz not null default now()
);

create unique index ficha_ativa_por_aluno_uk on ficha (aluno_id) where status = 'ATIVA';
create index ficha_aluno_id_idx on ficha (aluno_id);

create table divisao (
    id        uuid primary key default gen_random_uuid(),
    ficha_id  uuid not null references ficha (id) on delete cascade,
    nome      text not null,
    ordem     int not null
);

create table item_divisao (
    id            uuid primary key default gen_random_uuid(),
    divisao_id    uuid not null references divisao (id) on delete cascade,
    exercicio_id  uuid not null references exercicio (id),
    ordem         int not null,
    series        int not null check (series between 1 and 20),
    repeticoes    text not null,
    carga_kg      numeric(6, 2) check (carga_kg >= 0),
    descanso_seg  int check (descanso_seg >= 0),
    observacao    text
);

alter table ficha enable row level security;
alter table divisao enable row level security;
alter table item_divisao enable row level security;
