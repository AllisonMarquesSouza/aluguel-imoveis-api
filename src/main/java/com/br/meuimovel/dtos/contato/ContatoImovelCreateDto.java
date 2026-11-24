package com.br.meuimovel.dtos.contato;

import com.br.meuimovel.enums.CanalContato;
import jakarta.validation.constraints.NotNull;

public record ContatoImovelCreateDto(
        @NotNull CanalContato canal
) {}
