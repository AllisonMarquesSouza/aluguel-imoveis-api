package com.br.meuimovel.dtos.empresa;

import com.br.meuimovel.dtos.corretor.CorretorCreateDto;
import jakarta.validation.constraints.NotNull;

public record EmpresaAutonomaCreateDto(
        @NotNull
        CorretorCreateDto corretor,
        @NotNull
        EmpresaCreateDto empresa
) {
}
