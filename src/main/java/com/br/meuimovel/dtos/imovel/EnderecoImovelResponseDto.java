package com.br.meuimovel.dtos.imovel;

import java.math.BigDecimal;

public record EnderecoImovelResponseDto(
        Integer id,
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
