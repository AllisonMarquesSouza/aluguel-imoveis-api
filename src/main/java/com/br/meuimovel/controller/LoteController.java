package com.br.meuimovel.controller;

import com.br.meuimovel.dtos.imovel.LoteResponseDto;
import com.br.meuimovel.dtos.loteamento.LoteSituacaoUpdateDto;
import com.br.meuimovel.service.LoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/lote")
public class LoteController {
    private final LoteService loteService;

    @PatchMapping("/{id}/situacao")
    public ResponseEntity<LoteResponseDto> updateSituacao(
            @PathVariable Integer id,
            @RequestBody @Valid LoteSituacaoUpdateDto updateDto
    ) {
        return ResponseEntity.ok(loteService.updateSituacao(id, updateDto));
    }
}
