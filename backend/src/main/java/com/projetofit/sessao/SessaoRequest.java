package com.projetofit.sessao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Sessão finalizada no celular; {@code clientId} é gerado no aparelho e torna o envio idempotente. */
public record SessaoRequest(
        @NotNull UUID clientId,
        UUID divisaoId,
        @NotBlank @Size(max = 60) String divisaoNome,
        @NotNull Instant iniciadaEm,
        @NotNull Instant finalizadaEm,
        @Size(max = 1000) String observacao,
        @NotEmpty @Size(max = 400) List<@NotNull @Valid SerieRequest> series) {
}
