package com.projetofit.ficha;

import com.projetofit.exercicio.Exercicio;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "divisao")
public class Divisao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ficha_id", nullable = false)
    private Ficha ficha;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private int ordem;

    @OneToMany(mappedBy = "divisao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    private List<ItemDivisao> itens = new ArrayList<>();

    protected Divisao() {
    }

    Divisao(Ficha ficha, String nome, int ordem) {
        this.ficha = ficha;
        this.nome = nome;
        this.ordem = ordem;
    }

    public void atualizar(String nome, int ordem) {
        this.nome = nome;
        this.ordem = ordem;
    }

    public ItemDivisao adicionarItem(Exercicio exercicio, int ordem, int series, String repeticoes,
                                     BigDecimal cargaKg, Integer descansoSeg, String observacao) {
        ItemDivisao item = new ItemDivisao(this, exercicio, ordem, series, repeticoes, cargaKg, descansoSeg,
                observacao);
        itens.add(item);
        return item;
    }

    public void removerItensExceto(Set<UUID> idsMantidos) {
        itens.removeIf(i -> i.getId() != null && !idsMantidos.contains(i.getId()));
    }

    public UUID getId() {
        return id;
    }

    public Ficha getFicha() {
        return ficha;
    }

    public String getNome() {
        return nome;
    }

    public int getOrdem() {
        return ordem;
    }

    public List<ItemDivisao> getItens() {
        return itens;
    }
}
