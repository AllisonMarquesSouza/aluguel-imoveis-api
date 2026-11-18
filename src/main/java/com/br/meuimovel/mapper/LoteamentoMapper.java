package com.br.meuimovel.mapper;

import com.br.meuimovel.dtos.loteamento.LoteamentoResponseDto;
import com.br.meuimovel.dtos.loteamento.LoteamentoResumoResponseDto;
import com.br.meuimovel.dtos.loteamento.QuadraResponseDto;
import com.br.meuimovel.model.Loteamento;

import java.util.List;

public final class LoteamentoMapper {
    private LoteamentoMapper() {}

    public static LoteamentoResumoResponseDto toResumoResponse(Loteamento loteamento) {
        return new LoteamentoResumoResponseDto(
                loteamento.getId(), loteamento.getNome(), loteamento.getIncorporadora(),
                loteamento.getCidade().getId(), loteamento.getCidade().getNome(),
                loteamento.getCidade().getUf().name(), loteamento.getAsfalto(), loteamento.getAgua(),
                loteamento.getEnergia(), loteamento.getEsgoto(), loteamento.getIluminacaoPublica(),
                loteamento.getAreasLazer());
    }

    public static LoteamentoResponseDto toResponse(Loteamento loteamento, List<QuadraResponseDto> quadras) {
        return new LoteamentoResponseDto(
                loteamento.getId(), loteamento.getEmpresa().getId(), loteamento.getNome(),
                loteamento.getIncorporadora(), loteamento.getCidade().getId(), loteamento.getCidade().getNome(),
                loteamento.getCidade().getUf().name(), loteamento.getAsfalto(), loteamento.getAgua(),
                loteamento.getEnergia(), loteamento.getEsgoto(), loteamento.getIluminacaoPublica(),
                loteamento.getAreasLazer(), quadras);
    }
}
