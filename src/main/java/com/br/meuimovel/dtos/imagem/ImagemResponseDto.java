package com.br.meuimovel.dtos.imagem;

import com.br.meuimovel.enums.TipoImagem;

public record ImagemResponseDto(
        String id,
        TipoImagem tipo,
        Integer entidadeId,
        String url
) {}
