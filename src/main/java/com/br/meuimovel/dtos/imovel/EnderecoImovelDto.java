package com.br.meuimovel.dtos.imovel;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record EnderecoImovelDto(

    @NotBlank
    @Pattern(regexp = "\\d{8}")
    String cep,

    @NotBlank
    @Size(max = 180)
    String logradouro,

    @NotBlank
    @Size(max = 20)
    String numero,

    @Size(max = 100)
    String complemento,

    @NotBlank
    @Size(max = 100)
    String bairro,

    @NotNull
    @Positive
    Integer cidadeId,

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @Digits(integer = 2, fraction = 8)
    BigDecimal latitude,

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @Digits(integer = 3, fraction = 8)
    BigDecimal longitude

) {}
