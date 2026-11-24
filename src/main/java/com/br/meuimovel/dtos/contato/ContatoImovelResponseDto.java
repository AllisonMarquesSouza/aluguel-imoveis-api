package com.br.meuimovel.dtos.contato;

import com.br.meuimovel.enums.CanalContato;
import com.br.meuimovel.enums.RetornoContato;

import java.time.LocalDateTime;

public record ContatoImovelResponseDto(
        Integer id,
        Integer imovelId,
        String imovelCodigo,
        String imovelTitulo,
        Integer corretorId,
        String corretorNome,
        CanalContato canal,
        LocalDateTime criadoEm,
        RetornoContato retorno
) {}
