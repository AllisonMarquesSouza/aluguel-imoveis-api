package com.br.meuimovel.dtos.imovel;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CasaCreateDto(
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal areaTerreno,
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal areaConstruida,
        @NotNull @Min(0) Integer quartos,
        @NotNull @Min(0) Integer suites,
        @NotNull @Min(0) Integer banheiros,
        @NotNull @Min(0) Integer salas,
        @NotNull @Min(0) Integer vagas,
        @NotNull @Min(0) Integer pavimentos,
        @NotNull @Min(0) Integer anoConstrucao,
        @NotNull Boolean piscina,
        @NotNull Boolean quintal,
        @NotNull Boolean churrasqueira,
        @NotNull Boolean mobiliada
) {}
