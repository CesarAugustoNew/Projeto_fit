create table exercicio (
    id              uuid primary key default gen_random_uuid(),
    personal_id     uuid not null references usuario (id),
    nome            text not null,
    grupo_muscular  text not null check (grupo_muscular in (
        'PEITO', 'COSTAS', 'OMBROS', 'BICEPS', 'TRICEPS', 'ANTEBRACO', 'QUADRICEPS',
        'POSTERIORES', 'GLUTEOS', 'PANTURRILHA', 'ABDOMEN', 'CARDIO', 'CORPO_INTEIRO')),
    video_url       text,
    observacao      text,
    ativo           boolean not null default true,
    criado_em       timestamptz not null default now(),
    atualizado_em   timestamptz not null default now()
);

create unique index exercicio_personal_nome_uk on exercicio (personal_id, lower(nome));

alter table exercicio enable row level security;
