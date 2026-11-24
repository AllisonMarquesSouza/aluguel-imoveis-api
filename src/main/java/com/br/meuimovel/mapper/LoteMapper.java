package com.br.meuimovel.mapper;

import com.br.meuimovel.dtos.imovel.LoteResponseDto;
import com.br.meuimovel.dtos.loteamento.QuadraLoteResponseDto;
import com.br.meuimovel.model.Lote;

public final class LoteMapper {
    private LoteMapper() {}

    public static LoteResponseDto toResponse(Lote lote) {
        return new LoteResponseDto(
                lote.getId(), lote.getNumero(), lote.getArea(), lote.getFrente(), lote.getFundo(),
                lote.getCondicoesParcelamento(), lote.getSituacao(),
                new QuadraLoteResponseDto(
                        lote.getQuadra().getId(), lote.getQuadra().getIdentificacao(),
                        LoteamentoMapper.toResumoResponse(lote.getQuadra().getLoteamento())));
    }
}
