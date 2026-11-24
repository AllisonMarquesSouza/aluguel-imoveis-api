package com.br.meuimovel.dtos.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioUpdateDto(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Email @Size(max = 255) String email
) {}
