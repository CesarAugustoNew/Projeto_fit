package com.projetofit.exercicio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exercicio")
public class Exercicio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "personal_id", nullable = false)
    private UUID personalId;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "grupo_muscular", nullable = false)
    private GrupoMuscular grupoMuscular;

    @Column(name = "video_url")
    private String videoUrl;

    private String observacao;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Exercicio() {
    }

    public Exercicio(UUID personalId, String nome, GrupoMuscular grupoMuscular, String videoUrl, String observacao) {
        this.personalId = personalId;
        this.nome = nome;
        this.grupoMuscular = grupoMuscular;
        this.videoUrl = videoUrl;
        this.observacao = observacao;
        this.ativo = true;
        this.criadoEm = Instant.now();
        this.atualizadoEm = this.criadoEm;
    }

    @PreUpdate
    void aoAtualizar() {
        this.atualizadoEm = Instant.now();
    }

    public void atualizar(String nome, GrupoMuscular grupoMuscular, String videoUrl, String observacao,
                          boolean ativo) {
        this.nome = nome;
        this.grupoMuscular = grupoMuscular;
        this.videoUrl = videoUrl;
        this.observacao = observacao;
        this.ativo = ativo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPersonalId() {
        return personalId;
    }

    public String getNome() {
        return nome;
    }

    public GrupoMuscular getGrupoMuscular() {
        return grupoMuscular;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getObservacao() {
        return observacao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
