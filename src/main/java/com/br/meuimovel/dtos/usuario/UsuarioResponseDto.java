package com.br.meuimovel.dtos.usuario;

import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;

import java.time.LocalDateTime;

public record UsuarioResponseDto(
        Integer id,
        Integer empresaId,
        String nome,
        String email,
        UsuarioPerfil perfil,
        UsuarioStatus status,
        LocalDateTime criadoEm
) {}
