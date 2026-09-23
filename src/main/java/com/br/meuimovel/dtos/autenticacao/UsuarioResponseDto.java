package com.br.meuimovel.dtos.autenticacao;

import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.model.Empresa;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UsuarioResponseDto(
        Integer id,
        String email,
        Empresa empresa,
        UsuarioPerfil perfil,
        UsuarioStatus status,
        LocalDateTime criadoEm
) {
}
