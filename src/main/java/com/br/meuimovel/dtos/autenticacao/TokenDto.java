package com.br.meuimovel.dtos.autenticacao;

import com.br.meuimovel.enums.UsuarioPerfil;

public record TokenDto(String token, UsuarioPerfil perfil) {
}
