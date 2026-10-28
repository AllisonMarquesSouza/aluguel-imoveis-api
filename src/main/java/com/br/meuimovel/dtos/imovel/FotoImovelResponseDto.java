package com.br.meuimovel.dtos.imovel;

public record FotoImovelResponseDto(
    Integer id,
    String imagemId,
    String url,
    Integer ordem,
    boolean capa
) {}
