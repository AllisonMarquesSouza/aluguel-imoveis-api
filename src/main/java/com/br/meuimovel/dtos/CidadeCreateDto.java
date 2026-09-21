package com.br.meuimovel.dtos;

import com.br.meuimovel.enums.Uf;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CidadeCreateDto(@NotBlank @Size(max = 120) String nome,
                              @NotNull Uf uf) {
}
