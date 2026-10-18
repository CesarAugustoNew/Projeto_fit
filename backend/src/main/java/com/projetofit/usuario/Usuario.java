package com.projetofit.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/**
 * Usuário da aplicação. O {@code id} é atribuído (é o {@code sub} do JWT do Supabase), por isso a entidade
 * implementa {@link Persistable}: {@code save} de um usuário novo faz {@code INSERT} direto, sem {@code merge}.
 */
@Entity
@Table(name = "usuario")
public class Usuario implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Papel papel;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Transient
    private boolean novo = true;

    protected Usuario() {
    }

    public Usuario(UUID id, String nome, String email, Papel papel) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.papel = papel;
        this.criadoEm = Instant.now();
        this.atualizadoEm = this.criadoEm;
    }

    @PostLoad
    @PostPersist
    void marcarComoPersistido() {
        this.novo = false;
    }

    @PreUpdate
    void aoAtualizar() {
        this.atualizadoEm = Instant.now();
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return novo;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public Papel getPapel() {
        return papel;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
