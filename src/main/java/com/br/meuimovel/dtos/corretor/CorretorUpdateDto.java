package com.br.meuimovel.dtos.corretor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CorretorUpdateDto(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 30) String creci,
        @NotBlank @Size(max = 20) String telefone,
        @NotBlank @Size(max = 20) String whatsapp,
        @NotBlank String apresentacao
) {}
