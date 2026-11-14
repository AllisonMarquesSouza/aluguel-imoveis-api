package com.br.meuimovel.dtos.imovel;

import java.math.BigDecimal;

public record CasaResponseDto(
        BigDecimal areaTerreno,
        BigDecimal areaConstruida,
        BigDecimal areaLivre,
        Integer quartos,
        Integer suites,
        Integer banheiros,
        Integer salas,
        Integer vagas,
        Integer pavimentos,
        Integer anoConstrucao,
        Boolean piscina,
        Boolean quintal,
        Boolean churrasqueira,
        Boolean mobiliada
) {}
