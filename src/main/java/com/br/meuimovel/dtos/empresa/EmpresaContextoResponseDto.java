package com.br.meuimovel.dtos.empresa;

import com.br.meuimovel.enums.TipoEmpresa;

public record EmpresaContextoResponseDto(
        Integer id,
        TipoEmpresa tipo,
        String nome,
        String logoMarcaUrl,
        String logoImagemId
) {}
