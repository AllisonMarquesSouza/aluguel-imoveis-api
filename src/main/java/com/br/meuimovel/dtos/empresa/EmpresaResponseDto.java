package com.br.meuimovel.dtos.empresa;

import com.br.meuimovel.dtos.CidadeResponseDto;
import com.br.meuimovel.enums.TipoDocumento;
import com.br.meuimovel.enums.TipoEmpresa;

import java.time.LocalDateTime;
import java.util.List;

public record EmpresaResponseDto(
        Integer id,
        TipoEmpresa tipo,
        String nome,
        String razaoSocial,
        TipoDocumento tipoDocumento,
        String documento,
        String creci,
        String logoMarcaUrl,
        String descricao,
        String telefone,
        String whatsapp,
        String email,
        String logradouro,
        String numero,
        String bairro,
        String cep,
        CidadeResponseDto cidadeBase,
        List<CidadeResponseDto> cidadesAtuacao,
        boolean ativa,
        LocalDateTime criadoEm
) {
}
