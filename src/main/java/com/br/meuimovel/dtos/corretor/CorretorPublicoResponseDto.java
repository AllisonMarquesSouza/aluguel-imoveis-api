package com.br.meuimovel.dtos.corretor;

import com.br.meuimovel.dtos.empresa.EmpresaPublicaResponseDto;

public record CorretorPublicoResponseDto(
        Integer id,
        String nome,
        String creci,
        String fotoUrl,
        String telefone,
        String whatsapp,
        String email,
        String apresentacao,
        EmpresaPublicaResponseDto empresa
) {
}
