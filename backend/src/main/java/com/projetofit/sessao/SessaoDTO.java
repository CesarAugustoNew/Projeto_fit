package com.projetofit.sessao;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record SessaoDTO(UUID id, UUID clientId, UUID alunoId, UUID divisaoId, String divisaoNome,
                        Instant iniciadaEm, Instant finalizadaEm, String observacao,
                        List<SerieExecutadaDTO> series) {

    public static SessaoDTO de(Sessao s) {
        return new SessaoDTO(s.getId(), s.getClientId(), s.getAluno().getId(),
                s.getDivisao() == null ? null : s.getDivisao().getId(), s.getDivisaoNome(),
                s.getIniciadaEm(), s.getFinalizadaEm(), s.getObservacao(),
                s.getSeries().stream()
                        .sorted(Comparator.comparingInt(SerieExecutada::getOrdem))
                        .map(SerieExecutadaDTO::de).toList());
    }
}
