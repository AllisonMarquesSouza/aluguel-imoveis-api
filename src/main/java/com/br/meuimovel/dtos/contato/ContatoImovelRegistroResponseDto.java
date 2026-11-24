package com.br.meuimovel.dtos.contato;

import com.br.meuimovel.enums.CanalContato;

public record ContatoImovelRegistroResponseDto(
        CanalContato canal,
        String whatsappUrl
) {}
