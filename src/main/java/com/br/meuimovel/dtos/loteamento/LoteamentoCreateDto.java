package com.br.meuimovel.dtos.loteamento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record LoteamentoCreateDto(
        @NotBlank @Size(max = 180) String nome,
        @NotBlank @Size(max = 180) String incorporadora,
        @NotNull @Positive Integer cidadeId,
        @NotNull Boolean asfalto,
        @NotNull Boolean agua,
        @NotNull Boolean energia,
        @NotNull Boolean esgoto,
        @NotNull Boolean iluminacaoPublica,
        @Size(max = 4000) String areasLazer
) {}
