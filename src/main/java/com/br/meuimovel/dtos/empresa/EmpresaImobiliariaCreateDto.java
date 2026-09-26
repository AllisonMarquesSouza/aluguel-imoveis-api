package com.br.meuimovel.dtos.empresa;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmpresaImobiliariaCreateDto(

        @NotBlank
        @Size(max = 150)
        String nomeGestor,

        @NotBlank
        @Email
        @Size(max = 255)
        String emailGestor,

        @NotBlank
        @Size(min = 12, max = 255)
        String senhaGestor,

        @NotNull
        EmpresaCreateDto empresa
) {
}
