package com.br.meuimovel.dtos.imovel;

import com.br.meuimovel.enums.TipoDocumentoImovel;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DocumentoImovelResponseDto(
        Integer id,
        TipoDocumentoImovel tipo,
        String nomeArquivo,
        LocalDate dataDocumento,
        LocalDateTime criadoEm,
        String urlArquivo
) {}
