package com.br.meuimovel.dtos.imovel;

import com.br.meuimovel.enums.FinalidadeImovel;
import com.br.meuimovel.enums.NaturezaImovel;
import com.br.meuimovel.enums.OrdenacaoVitrine;

import java.math.BigDecimal;
import java.util.List;

public record ImovelVitrineFiltro(
        Integer cidadeId,
        String texto,
        FinalidadeImovel finalidade,
        NaturezaImovel natureza,
        BigDecimal precoMinimo,
        BigDecimal precoMaximo,
        Integer quartos,
        Integer suites,
        Integer vagas,
        String bairro,
        BigDecimal areaMinima,
        BigDecimal areaMaxima,
        List<Integer> caracteristicas,
        OrdenacaoVitrine ordenacao,
        Integer pagina,
        Integer tamanho
) {}
