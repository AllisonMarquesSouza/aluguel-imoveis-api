package com.br.meuimovel.dtos.autenticacao;

import com.br.meuimovel.enums.UsuarioPerfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterDto(
        Integer empresaId,

        @NotBlank
        @Size(max = 150)
        String nome,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 12, max = 255)
        String senha,

        @NotNull
        UsuarioPerfil perfil

) {
}