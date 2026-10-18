package com.projetofit.ficha;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Uma divisão da ficha; {@code id} só vem na edição de uma divisão existente. */
public record DivisaoRequest(
        UUID id,
        @NotBlank @Size(max = 60) String nome,
        @NotEmpty @Size(max = 20) List<@Valid ItemRequest> itens) {
}
