package com.br.meuimovel.dtos.predio;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PredioCreateDto(
        @NotBlank @Size(max = 180) String nome,
        @NotNull @Valid EnderecoPredioDto endereco,
        @NotNull @Positive Integer numeroAndares,
        @NotNull @Positive Integer unidadesPorAndar,
        @NotNull @Min(0) Integer anoConstrucao,
        @NotNull @Min(0) Integer elevadores,
        @NotNull Boolean portaria24h,
        @NotNull Boolean piscina,
        @NotNull Boolean academia,
        @NotNull Boolean salaoFestas,
        @NotNull Boolean playground,
        @NotNull @Digits(integer = 13, fraction = 2) @PositiveOrZero BigDecimal valorCondominioBase
) {}
