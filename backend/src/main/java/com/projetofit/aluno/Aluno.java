package com.projetofit.aluno;

import com.projetofit.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "aluno")
public class Aluno {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "personal_id", nullable = false)
    private UUID personalId;

    private String telefone;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    private String objetivo;

    private String observacoes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAluno status;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Aluno() {
    }

    public Aluno(Usuario usuario, UUID personalId, String telefone, LocalDate dataNascimento,
                 String objetivo, String observacoes) {
        this.usuario = usuario;
        this.personalId = personalId;
        this.telefone = telefone;
        this.dataNascimento = dataNascimento;
        this.objetivo = objetivo;
        this.observacoes = observacoes;
        this.status = StatusAluno.CONVIDADO;
        this.criadoEm = Instant.now();
        this.atualizadoEm = this.criadoEm;
    }

    @PreUpdate
    void aoAtualizar() {
        this.atualizadoEm = Instant.now();
    }

    public void atualizarDados(String telefone, LocalDate dataNascimento, String objetivo, String observacoes) {
        this.telefone = telefone;
        this.dataNascimento = dataNascimento;
        this.objetivo = objetivo;
        this.observacoes = observacoes;
    }

    public void alterarStatus(StatusAluno status) {
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public UUID getPersonalId() {
        return personalId;
    }

    public String getTelefone() {
        return telefone;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public String getObjetivo() {
        return objetivo;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public StatusAluno getStatus() {
        return status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
