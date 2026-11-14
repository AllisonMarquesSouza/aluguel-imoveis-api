package com.br.meuimovel.dtos.imovel;

import com.br.meuimovel.enums.TopografiaTerreno;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TerrenoCreateDto(
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal areaTotal,
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal frente,
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal fundo,
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal lateralEsquerda,
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal lateralDireita,
        @NotNull TopografiaTerreno topografia,
        @NotNull Boolean muro,
        @NotNull Boolean esquina,
        @NotBlank String usoPermitido
) {}
