package com.br.meuimovel.dtos.loteamento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuadraCreateDto(
        @NotBlank @Size(max = 50) String identificacao
) {}
