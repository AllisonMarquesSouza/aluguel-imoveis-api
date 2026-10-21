package com.br.meuimovel.dtos.proprietario;

import com.br.meuimovel.enums.TipoDocumento;

public record ProprietarioResponseDto(
        Integer id,
        TipoDocumento tipoDocumento,
        String documento,
        String nome,
        String telefone,
        String email,
        String observacoes
) {
}
