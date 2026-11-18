package com.br.meuimovel.dtos.loteamento;

public record LoteamentoResumoResponseDto(
        Integer id,
        String nome,
        String incorporadora,
        Integer cidadeId,
        String cidade,
        String uf,
        Boolean asfalto,
        Boolean agua,
        Boolean energia,
        Boolean esgoto,
        Boolean iluminacaoPublica,
        String areasLazer
) {}
