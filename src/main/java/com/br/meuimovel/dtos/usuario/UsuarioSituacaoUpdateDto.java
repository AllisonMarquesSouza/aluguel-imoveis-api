package com.br.meuimovel.dtos.usuario;

import com.br.meuimovel.enums.UsuarioStatus;
import jakarta.validation.constraints.NotNull;

public record UsuarioSituacaoUpdateDto(@NotNull UsuarioStatus status) {}
