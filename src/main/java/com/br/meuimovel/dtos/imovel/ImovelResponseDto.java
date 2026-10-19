package com.br.meuimovel.dtos.imovel;


import com.br.meuimovel.dtos.caracteristica.CaracteristicaResponseDto;
import com.br.meuimovel.enums.FinalidadeImovel;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record ImovelResponseDto(

    Integer id,
    String codigo,
    String titulo,

    Integer empresaId,
    Integer corretorId,
    String nomeCorretor,

    Integer proprietarioId,
    String nomeProprietario,

    FinalidadeImovel finalidade,

    BigDecimal valorVenda,
    BigDecimal valorAluguel,
    BigDecimal valorCondominio,
    BigDecimal valorIptu,

    String descricao,

    EnderecoImovelResponseDto endereco,
    Set<CaracteristicaResponseDto> caracteristicas,
    List<FotoImovelResponseDto> fotos

) {}
