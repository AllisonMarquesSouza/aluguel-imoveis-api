package com.br.meuimovel.dtos.imovel;

import com.br.meuimovel.dtos.loteamento.QuadraLoteResponseDto;
import com.br.meuimovel.enums.SituacaoLote;

import java.math.BigDecimal;

public record LoteResponseDto(
        String numero,
        BigDecimal area,
        BigDecimal frente,
        BigDecimal fundo,
        String condicoesParcelamento,
        SituacaoLote situacao,
        QuadraLoteResponseDto quadra
) {}
