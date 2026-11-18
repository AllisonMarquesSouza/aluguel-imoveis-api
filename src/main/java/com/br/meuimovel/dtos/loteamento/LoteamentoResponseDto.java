package com.br.meuimovel.dtos.loteamento;

import java.util.List;

public record LoteamentoResponseDto(
        Integer id,
        Integer empresaId,
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
        String areasLazer,
        List<QuadraResponseDto> quadras
) {}
