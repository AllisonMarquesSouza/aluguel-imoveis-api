package com.br.meuimovel.dtos.imovel;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ApartamentoCreateDto(
        @NotNull @Positive Integer predioId,
        @NotNull @Min(0) Integer andar,
        @NotBlank @Size(max = 30) String numero,
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal areaPrivativa,
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal areaTotal,
        @NotNull @Min(0) Integer quartos,
        @NotNull @Min(0) Integer suites,
        @NotNull @Min(0) Integer banheiros,
        @NotNull @Min(0) Integer vagas,
        @NotNull Boolean varanda,
        @NotBlank @Size(max = 40) String posicaoSolar
) {}
