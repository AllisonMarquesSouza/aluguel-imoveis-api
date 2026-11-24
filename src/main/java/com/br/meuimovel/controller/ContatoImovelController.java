package com.br.meuimovel.controller;

import com.br.meuimovel.dtos.contato.ContatoImovelResponseDto;
import com.br.meuimovel.dtos.contato.ContatoImovelRetornoUpdateDto;
import com.br.meuimovel.service.ContatoImovelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/contatos")
public class ContatoImovelController {
    private final ContatoImovelService contatoImovelService;

    @GetMapping
    public ResponseEntity<List<ContatoImovelResponseDto>> listar(
            @RequestParam(required = false) Integer imovelId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
    ) {
        return ResponseEntity.ok(contatoImovelService.listar(imovelId, dataInicio, dataFim));
    }

    @PatchMapping("/{id}/retorno")
    public ResponseEntity<ContatoImovelResponseDto> atualizarRetorno(
            @PathVariable Integer id,
            @RequestBody @Valid ContatoImovelRetornoUpdateDto dto
    ) {
        return ResponseEntity.ok(contatoImovelService.atualizarRetorno(id, dto));
    }
}
