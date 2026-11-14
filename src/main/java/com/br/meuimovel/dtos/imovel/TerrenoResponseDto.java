package com.br.meuimovel.dtos.imovel;

import com.br.meuimovel.enums.TopografiaTerreno;

import java.math.BigDecimal;

public record TerrenoResponseDto(
        BigDecimal areaTotal,
        BigDecimal frente,
        BigDecimal fundo,
        BigDecimal lateralEsquerda,
        BigDecimal lateralDireita,
        TopografiaTerreno topografia,
        Boolean muro,
        Boolean esquina,
        String usoPermitido
) {}
