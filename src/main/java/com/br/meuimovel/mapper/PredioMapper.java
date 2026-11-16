package com.br.meuimovel.mapper;

import com.br.meuimovel.dtos.imovel.EnderecoImovelResponseDto;
import com.br.meuimovel.dtos.predio.EnderecoPredioResponseDto;
import com.br.meuimovel.dtos.predio.PredioResponseDto;
import com.br.meuimovel.model.Predio;

public final class PredioMapper {
    private PredioMapper() {}

    public static PredioResponseDto toResponse(Predio predio) {
        return new PredioResponseDto(
                predio.getId(), predio.getEmpresa().getId(), predio.getNome(),
                new EnderecoPredioResponseDto(
                        predio.getCep(), predio.getLogradouro(), predio.getNumero(), predio.getComplemento(),
                        predio.getBairro(), predio.getCidade().getId(), predio.getCidade().getNome(),
                        predio.getCidade().getUf().name(), predio.getLatitude(), predio.getLongitude()),
                predio.getNumeroAndares(), predio.getUnidadesPorAndar(), predio.getAnoConstrucao(),
                predio.getElevadores(), predio.getPortaria24h(), predio.getPiscina(), predio.getAcademia(),
                predio.getSalaoFestas(), predio.getPlayground(), predio.getValorCondominioBase());
    }

    public static EnderecoImovelResponseDto toImovelEndereco(Predio predio) {
        return new EnderecoImovelResponseDto(
                null, predio.getCep(), predio.getLogradouro(), predio.getNumero(), predio.getComplemento(),
                predio.getBairro(), predio.getCidade().getId(), predio.getCidade().getNome(),
                predio.getCidade().getUf().name(), predio.getLatitude(), predio.getLongitude());
    }
}
