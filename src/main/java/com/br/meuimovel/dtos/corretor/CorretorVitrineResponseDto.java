package com.br.meuimovel.dtos.corretor;

public record CorretorVitrineResponseDto(
        Integer id,
        String nome,
        String creci,
        String fotoUrl,
        String fotoImagemId,
        String telefone,
        String whatsapp,
        String email,
        String apresentacao
) {}
