package com.br.meuimovel.dtos.predio;

import java.math.BigDecimal;

public record PredioVitrineResponseDto(
        String nome,
        EnderecoPredioResponseDto endereco,
        Integer numeroAndares,
        Integer unidadesPorAndar,
        Integer anoConstrucao,
        Integer elevadores,
        Boolean portaria24h,
        Boolean piscina,
        Boolean academia,
        Boolean salaoFestas,
        Boolean playground,
        BigDecimal valorCondominioBase
) {}
