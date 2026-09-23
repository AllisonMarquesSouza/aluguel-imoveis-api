package com.br.meuimovel.dtos;

import com.br.meuimovel.enums.TipoDocumento;
import com.br.meuimovel.enums.TipoEmpresa;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record EmpresaCreateDto(
        @NotNull
        TipoEmpresa tipo,

        @NotBlank
        @Size(max = 150)
        String nome,

        @Size(max = 180)
        String razaoSocial,

        @NotNull
        TipoDocumento tipoDocumento,

        @NotBlank
        @Size(max = 14)
        String documento,

        @Size(max = 30)
        String creci,

        @Size(max = 500)
        String logoMarcaUrl,

        String descricao,

        @Size(max = 20)
        String telefone,

        @Size(max = 20)
        String whatsapp,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @Size(max = 180)
        String logradouro,

        @Size(max = 20)
        String numero,

        @Size(max = 100)
        String bairro,

        @Size(max = 8)
        String cep,

        @Positive
        Integer cidadeBaseId,

        @NotEmpty
        Set<@NotNull @Positive Integer> cidadesAtuacaoIds
) {
}
