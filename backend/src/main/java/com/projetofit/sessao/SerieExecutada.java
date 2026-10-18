package com.projetofit.sessao;

import com.projetofit.exercicio.Exercicio;
import com.projetofit.ficha.ItemDivisao;
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
@Table(name = "serie_executada")
public class SerieExecutada {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sessao_id", nullable = false)
    private Sessao sessao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercicio_id", nullable = false)
    private Exercicio exercicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_divisao_id")
    private ItemDivisao itemDivisao;

    @Column(nullable = false)
    private int ordem;

    @Column(name = "numero_serie", nullable = false)
    private int numeroSerie;

    @Column(nullable = false)
    private int repeticoes;

    @Column(name = "carga_kg", precision = 6, scale = 2)
    private BigDecimal cargaKg;

    protected SerieExecutada() {
    }

    SerieExecutada(Sessao sessao, Exercicio exercicio, ItemDivisao itemDivisao, int ordem, int numeroSerie,
                   int repeticoes, BigDecimal cargaKg) {
        this.sessao = sessao;
        this.exercicio = exercicio;
        this.itemDivisao = itemDivisao;
        this.ordem = ordem;
        this.numeroSerie = numeroSerie;
        this.repeticoes = repeticoes;
        this.cargaKg = cargaKg;
    }

    public UUID getId() {
        return id;
    }

    public Sessao getSessao() {
        return sessao;
    }

    public Exercicio getExercicio() {
        return exercicio;
    }

    public ItemDivisao getItemDivisao() {
        return itemDivisao;
    }

    public int getOrdem() {
        return ordem;
    }

    public int getNumeroSerie() {
        return numeroSerie;
    }

    public int getRepeticoes() {
        return repeticoes;
    }

    public BigDecimal getCargaKg() {
        return cargaKg;
    }
}
