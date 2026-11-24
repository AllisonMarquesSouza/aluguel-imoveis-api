package com.br.meuimovel.dtos.contato;

import com.br.meuimovel.enums.RetornoContato;
import jakarta.validation.constraints.NotNull;

public record ContatoImovelRetornoUpdateDto(
        @NotNull RetornoContato retorno
) {}
