package com.projetofit.ficha;

import com.projetofit.exercicio.Exercicio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "item_divisao")
public class ItemDivisao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "divisao_id", nullable = false)
    private Divisao divisao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercicio_id", nullable = false)
    private Exercicio exercicio;

    @Column(nullable = false)
    private int ordem;

    @Column(nullable = false)
    private int series;

    @Column(nullable = false)
    private String repeticoes;

    @Column(name = "carga_kg", precision = 6, scale = 2)
    private BigDecimal cargaKg;

    @Column(name = "descanso_seg")
    private Integer descansoSeg;

    private String observacao;

    protected ItemDivisao() {
    }

    ItemDivisao(Divisao divisao, Exercicio exercicio, int ordem, int series, String repeticoes,
                BigDecimal cargaKg, Integer descansoSeg, String observacao) {
        this.divisao = divisao;
        atualizar(exercicio, ordem, series, repeticoes, cargaKg, descansoSeg, observacao);
    }

    public void atualizar(Exercicio exercicio, int ordem, int series, String repeticoes, BigDecimal cargaKg,
                          Integer descansoSeg, String observacao) {
        this.exercicio = exercicio;
        this.ordem = ordem;
        this.series = series;
        this.repeticoes = repeticoes;
        this.cargaKg = cargaKg;
        this.descansoSeg = descansoSeg;
        this.observacao = observacao;
    }

    public UUID getId() {
        return id;
    }

    public Divisao getDivisao() {
        return divisao;
    }

    public Exercicio getExercicio() {
        return exercicio;
    }

    public int getOrdem() {
        return ordem;
    }

    public int getSeries() {
        return series;
    }

    public String getRepeticoes() {
        return repeticoes;
    }

    public BigDecimal getCargaKg() {
        return cargaKg;
    }

    public Integer getDescansoSeg() {
        return descansoSeg;
    }

    public String getObservacao() {
        return observacao;
    }
}
