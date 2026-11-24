package com.br.meuimovel.dtos.imovel;

import com.br.meuimovel.dtos.loteamento.QuadraLoteResponseDto;
import com.br.meuimovel.enums.SituacaoImovel;

import java.math.BigDecimal;

public record LoteResponseDto(
        Integer id,
        String numero,
        BigDecimal area,
        BigDecimal frente,
        BigDecimal fundo,
        String condicoesParcelamento,
        SituacaoImovel situacao,
        QuadraLoteResponseDto quadra
) {}
