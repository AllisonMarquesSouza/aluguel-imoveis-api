package com.br.meuimovel.dtos.imovel;

import com.br.meuimovel.enums.SituacaoImovel;

public record ImovelGestaoResponseDto(
        Integer id,
        SituacaoImovel situacao,
        boolean publicado
) {}
