package com.br.meuimovel.dtos.imovel;

import com.br.meuimovel.enums.SituacaoImovel;

import java.time.LocalDateTime;

public record HistoricoSituacaoImovelResponseDto(
        Integer id,
        SituacaoImovel situacaoAnterior,
        SituacaoImovel situacaoNova,
        Integer autorId,
        String autorNome,
        LocalDateTime alteradoEm,
        String observacao
) {}
