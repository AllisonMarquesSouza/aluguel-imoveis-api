package com.br.meuimovel.dtos.imovel;

import com.br.meuimovel.enums.SituacaoImovel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SituacaoImovelUpdateDto(
        @NotNull SituacaoImovel situacao,
        @NotBlank @Size(max = 2000) String observacao
) {}
