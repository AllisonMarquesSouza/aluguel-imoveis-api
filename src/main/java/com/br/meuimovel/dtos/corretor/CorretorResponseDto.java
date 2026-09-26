package com.br.meuimovel.dtos.corretor;

import lombok.Builder;

@Builder
public record CorretorResponseDto(
        Integer id,
        String nome,
        String creci,
        String fotoUrl,
        String telefone,
        String whatsapp,
        String email,
        String apresentacao
) {
}
