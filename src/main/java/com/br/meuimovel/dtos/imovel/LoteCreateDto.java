package com.br.meuimovel.dtos.imovel;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record LoteCreateDto(
        @NotNull @Positive Integer quadraId,
        @NotBlank @Size(max = 30) String numero,
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal area,
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal frente,
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal fundo,
        @NotBlank @Size(max = 4000) String condicoesParcelamento
) {}
