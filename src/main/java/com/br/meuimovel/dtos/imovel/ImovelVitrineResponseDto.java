package com.br.meuimovel.dtos.imovel;

import com.br.meuimovel.dtos.caracteristica.CaracteristicaResponseDto;
import com.br.meuimovel.enums.FinalidadeImovel;
import com.br.meuimovel.enums.NaturezaImovel;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record ImovelVitrineResponseDto(
        Integer id,
        String codigo,
        String titulo,
        FinalidadeImovel finalidade,
        NaturezaImovel natureza,
        CasaResponseDto casa,
        TerrenoResponseDto terreno,
        ApartamentoResponseDto apartamento,
        LoteResponseDto lote,
        BigDecimal valorVenda,
        BigDecimal valorAluguel,
        BigDecimal valorCondominio,
        BigDecimal valorIptu,
        String descricao,
        EnderecoImovelResponseDto endereco,
        Set<CaracteristicaResponseDto> caracteristicas,
        List<FotoImovelResponseDto> fotos
) {}
