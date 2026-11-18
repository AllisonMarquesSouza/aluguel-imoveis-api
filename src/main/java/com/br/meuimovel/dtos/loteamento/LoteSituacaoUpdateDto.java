package com.br.meuimovel.dtos.loteamento;

import com.br.meuimovel.enums.SituacaoLote;
import jakarta.validation.constraints.NotNull;

public record LoteSituacaoUpdateDto(
        @NotNull SituacaoLote situacao
) {}
