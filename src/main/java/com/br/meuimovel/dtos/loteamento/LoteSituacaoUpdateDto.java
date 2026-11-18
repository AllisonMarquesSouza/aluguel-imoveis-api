package com.br.meuimovel.dtos.loteamento;

import com.br.meuimovel.enums.SituacaoImovel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoteSituacaoUpdateDto(
        @NotNull SituacaoImovel situacao,
        @NotBlank @Size(max = 2000) String observacao
) {}
