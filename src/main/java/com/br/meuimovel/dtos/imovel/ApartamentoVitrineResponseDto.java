package com.br.meuimovel.dtos.imovel;

import com.br.meuimovel.dtos.predio.PredioVitrineResponseDto;

import java.math.BigDecimal;

public record ApartamentoVitrineResponseDto(
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
        PredioVitrineResponseDto predio
) {}
