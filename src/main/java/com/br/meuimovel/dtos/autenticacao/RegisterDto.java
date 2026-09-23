package com.br.meuimovel.dtos.autenticacao;

import com.br.meuimovel.enums.UsuarioPerfil;

public record RegisterDto(Integer empresaId, String nome, String email, String senha, UsuarioPerfil perfil) {

}
