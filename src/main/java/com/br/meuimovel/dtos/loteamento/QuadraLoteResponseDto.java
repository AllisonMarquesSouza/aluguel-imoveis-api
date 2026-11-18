package com.br.meuimovel.dtos.loteamento;

public record QuadraLoteResponseDto(
        Integer id,
        String identificacao,
        LoteamentoResumoResponseDto loteamento
) {}
