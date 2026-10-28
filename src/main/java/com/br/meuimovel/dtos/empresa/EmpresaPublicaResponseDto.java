package com.br.meuimovel.dtos.empresa;

import com.br.meuimovel.dtos.CidadeResponseDto;
import com.br.meuimovel.enums.TipoEmpresa;

import java.util.List;

public record EmpresaPublicaResponseDto(
        Integer id,
        TipoEmpresa tipo,
        String nome,
        String razaoSocial,
        String creci,
        String logoMarcaUrl,
        String logoImagemId,
        String descricao,
        String telefone,
        String whatsapp,
        String email,
        String logradouro,
        String numero,
        String bairro,
        String cep,
        CidadeResponseDto cidadeBase,
        List<CidadeResponseDto> cidadesAtuacao
) {
}
