package com.br.meuimovel.dtos.proprietario;

import com.br.meuimovel.enums.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProprietarioCreateDto(
        @NotNull
        TipoDocumento tipoDocumento,

        @NotBlank
        @Size(max = 14)
        String documento,

        @NotBlank
        @Size(max = 150)
        String nome,

        @NotBlank
        @Size(max = 20 )
        String telefone,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        String observacoes
) {
}
