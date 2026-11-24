package com.br.meuimovel.dtos.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioSenhaUpdateDto(
        @NotBlank @Size(max = 255) String senhaAtual,
        @NotBlank @Size(min = 12, max = 255) String novaSenha,
        @NotBlank @Size(min = 12, max = 255) String confirmacaoNovaSenha
) {}
