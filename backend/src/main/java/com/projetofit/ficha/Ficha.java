package com.projetofit.ficha;

import com.projetofit.aluno.Aluno;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "ficha")
public class Ficha {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    @Column(nullable = false)
    private String nome;

    private String objetivo;

    @Column(name = "frequencia_semanal", nullable = false)
    private int frequenciaSemanal;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusFicha status;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @OneToMany(mappedBy = "ficha", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    private List<Divisao> divisoes = new ArrayList<>();

    protected Ficha() {
    }

    public Ficha(Aluno aluno, String nome, String objetivo, int frequenciaSemanal, LocalDate dataInicio,
                 LocalDate dataFim) {
        this.aluno = aluno;
        this.nome = nome;
        this.objetivo = objetivo;
        this.frequenciaSemanal = frequenciaSemanal;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.status = StatusFicha.ATIVA;
        this.criadoEm = Instant.now();
        this.atualizadoEm = this.criadoEm;
    }

    @PreUpdate
    void aoAtualizar() {
        this.atualizadoEm = Instant.now();
    }

    public void atualizarCabecalho(String nome, String objetivo, int frequenciaSemanal, LocalDate dataInicio,
                                   LocalDate dataFim) {
        this.nome = nome;
        this.objetivo = objetivo;
        this.frequenciaSemanal = frequenciaSemanal;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
    }

    public void encerrar() {
        this.status = StatusFicha.ENCERRADA;
    }

    public boolean vencida(LocalDate hoje) {
        return status == StatusFicha.ATIVA && dataFim != null && dataFim.isBefore(hoje);
    }

    public Divisao adicionarDivisao(String nome, int ordem) {
        Divisao divisao = new Divisao(this, nome, ordem);
        divisoes.add(divisao);
        return divisao;
    }

    public void removerDivisoesExceto(Set<UUID> idsMantidos) {
        divisoes.removeIf(d -> d.getId() != null && !idsMantidos.contains(d.getId()));
    }

    public UUID getId() {
        return id;
    }

    public Aluno getAluno() {
        return aluno;
    }

    public String getNome() {
        return nome;
    }

    public String getObjetivo() {
        return objetivo;
    }

    public int getFrequenciaSemanal() {
        return frequenciaSemanal;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public StatusFicha getStatus() {
        return status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public List<Divisao> getDivisoes() {
        return divisoes;
    }
}
