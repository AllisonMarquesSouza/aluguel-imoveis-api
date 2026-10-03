package com.br.meuimovel.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CaracteristicaCreateDto(
        @NotBlank @Size(max = 50) String nome
) {
}
