package com.projetofit.sessao;

import com.projetofit.aluno.Aluno;
import com.projetofit.exercicio.Exercicio;
import com.projetofit.ficha.Divisao;
import com.projetofit.ficha.ItemDivisao;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sessao")
public class Sessao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "divisao_id")
    private Divisao divisao;

    @Column(name = "divisao_nome", nullable = false)
    private String divisaoNome;

    @Column(name = "iniciada_em", nullable = false)
    private Instant iniciadaEm;

    @Column(name = "finalizada_em", nullable = false)
    private Instant finalizadaEm;

    private String observacao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @OneToMany(mappedBy = "sessao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    private List<SerieExecutada> series = new ArrayList<>();

    protected Sessao() {
    }

    public Sessao(UUID clientId, Aluno aluno, Divisao divisao, String divisaoNome, Instant iniciadaEm,
                  Instant finalizadaEm, String observacao) {
        this.clientId = clientId;
        this.aluno = aluno;
        this.divisao = divisao;
        this.divisaoNome = divisaoNome;
        this.iniciadaEm = iniciadaEm;
        this.finalizadaEm = finalizadaEm;
        this.observacao = observacao;
        this.criadoEm = Instant.now();
    }

    public SerieExecutada adicionarSerie(Exercicio exercicio, ItemDivisao item, int ordem, int numeroSerie,
                                         int repeticoes, BigDecimal cargaKg) {
        SerieExecutada serie = new SerieExecutada(this, exercicio, item, ordem, numeroSerie, repeticoes, cargaKg);
        series.add(serie);
        return serie;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClientId() {
        return clientId;
    }

    public Aluno getAluno() {
        return aluno;
    }

    public Divisao getDivisao() {
        return divisao;
    }

    public String getDivisaoNome() {
        return divisaoNome;
    }

    public Instant getIniciadaEm() {
        return iniciadaEm;
    }

    public Instant getFinalizadaEm() {
        return finalizadaEm;
    }

    public String getObservacao() {
        return observacao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public List<SerieExecutada> getSeries() {
        return series;
    }
}
