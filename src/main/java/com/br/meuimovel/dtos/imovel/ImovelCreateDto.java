package com.br.meuimovel.dtos.imovel;

import com.br.meuimovel.enums.FinalidadeImovel;
import com.br.meuimovel.enums.NaturezaImovel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Set;

public record ImovelCreateDto(
    @NotBlank
    @Size(max = 50)
    String codigo,

    @NotBlank
    @Size(max = 180)
    String titulo,

    @NotNull
    @Positive
    Integer proprietarioId,

    @NotNull
    FinalidadeImovel finalidade,

    @PositiveOrZero
    @Digits(integer = 13, fraction = 2)
    BigDecimal valorVenda,

    @PositiveOrZero
    @Digits(integer = 13, fraction = 2)
    BigDecimal valorAluguel,

    @PositiveOrZero
    @Digits(integer = 13, fraction = 2)
    BigDecimal valorCondominio,

    @PositiveOrZero
    @Digits(integer = 13, fraction = 2)
    BigDecimal valorIptu,

    String descricao,

    @NotNull
    NaturezaImovel natureza,

    @Valid
    CasaCreateDto casa,

    @Valid
    TerrenoCreateDto terreno,

    @NotNull
    @Valid
    EnderecoImovelDto endereco,

    Set<Integer> caracteristicasIds,

    @Min(1)
    Integer fotoCapaOrdem
) {}
