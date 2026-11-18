package com.br.meuimovel.controller;

import com.br.meuimovel.dtos.imovel.HistoricoSituacaoImovelResponseDto;
import com.br.meuimovel.dtos.imovel.ImovelGestaoResponseDto;
import com.br.meuimovel.dtos.imovel.PublicacaoImovelUpdateDto;
import com.br.meuimovel.dtos.imovel.SituacaoImovelUpdateDto;
import com.br.meuimovel.service.ImovelGestaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/imovel")
public class ImovelGestaoController {
    private final ImovelGestaoService imovelGestaoService;

    @PatchMapping("/{id}/publicacao")
    public ResponseEntity<ImovelGestaoResponseDto> atualizarPublicacao(
            @PathVariable Integer id,
            @RequestBody @Valid PublicacaoImovelUpdateDto dto
    ) {
        return ResponseEntity.ok(imovelGestaoService.atualizarPublicacao(id, dto));
    }

    @PatchMapping("/{id}/situacao")
    public ResponseEntity<ImovelGestaoResponseDto> atualizarSituacao(
            @PathVariable Integer id,
            @RequestBody @Valid SituacaoImovelUpdateDto dto
    ) {
        return ResponseEntity.ok(imovelGestaoService.atualizarSituacao(id, dto));
    }

    @GetMapping("/{id}/historico-situacao")
    public ResponseEntity<List<HistoricoSituacaoImovelResponseDto>> listarHistorico(@PathVariable Integer id) {
        return ResponseEntity.ok(imovelGestaoService.listarHistorico(id));
    }
}
