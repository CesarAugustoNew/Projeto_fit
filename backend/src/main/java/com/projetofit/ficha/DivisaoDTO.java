package com.projetofit.ficha;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record DivisaoDTO(UUID id, String nome, int ordem, List<ItemDivisaoDTO> itens) {

    static DivisaoDTO de(Divisao d) {
        return new DivisaoDTO(d.getId(), d.getNome(), d.getOrdem(),
                d.getItens().stream()
                        .sorted(Comparator.comparingInt(ItemDivisao::getOrdem))
                        .map(ItemDivisaoDTO::de).toList());
    }
}
