package com.br.meuimovel.dtos.autenticacao;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AutenticacaoDto(@NotBlank @Email String email, @NotBlank String senha) {
}
