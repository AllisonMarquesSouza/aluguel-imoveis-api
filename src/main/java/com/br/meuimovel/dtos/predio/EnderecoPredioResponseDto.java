package com.br.meuimovel.dtos.predio;

import java.math.BigDecimal;

public record EnderecoPredioResponseDto(
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        Integer cidadeId,
        String cidade,
        String uf,
        BigDecimal latitude,
        BigDecimal longitude
) {}
