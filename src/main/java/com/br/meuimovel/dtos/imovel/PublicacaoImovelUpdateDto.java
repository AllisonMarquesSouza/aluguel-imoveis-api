package com.br.meuimovel.dtos.imovel;

import jakarta.validation.constraints.NotNull;

public record PublicacaoImovelUpdateDto(
        @NotNull Boolean publicado
) {}
