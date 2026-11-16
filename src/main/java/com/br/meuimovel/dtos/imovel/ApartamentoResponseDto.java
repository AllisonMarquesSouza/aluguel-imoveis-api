package com.br.meuimovel.dtos.imovel;

import com.br.meuimovel.dtos.predio.PredioResponseDto;

import java.math.BigDecimal;

public record ApartamentoResponseDto(
        Integer andar,
        String numero,
        BigDecimal areaPrivativa,
        BigDecimal areaTotal,
        Integer quartos,
        Integer suites,
        Integer banheiros,
        Integer vagas,
        Boolean varanda,
        String posicaoSolar,
        PredioResponseDto predio
) {}
