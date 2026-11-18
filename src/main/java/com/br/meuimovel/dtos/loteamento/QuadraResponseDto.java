package com.br.meuimovel.dtos.loteamento;

public record QuadraResponseDto(
        Integer id,
        String identificacao,
        long lotesDisponiveis,
        long lotesVendidos
) {}
