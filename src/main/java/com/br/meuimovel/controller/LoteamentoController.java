package com.br.meuimovel.controller;

import com.br.meuimovel.dtos.loteamento.LoteamentoCreateDto;
import com.br.meuimovel.dtos.loteamento.LoteamentoResponseDto;
import com.br.meuimovel.dtos.loteamento.QuadraCreateDto;
import com.br.meuimovel.dtos.loteamento.QuadraResponseDto;
import com.br.meuimovel.dtos.imovel.LoteResponseDto;
import com.br.meuimovel.service.LoteamentoService;
import com.br.meuimovel.service.LoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/loteamento")
public class LoteamentoController {
    private final LoteamentoService loteamentoService;
    private final LoteService loteService;

    @PostMapping
    public ResponseEntity<LoteamentoResponseDto> create(@RequestBody @Valid LoteamentoCreateDto createDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loteamentoService.create(createDto));
    }

    @GetMapping
    public ResponseEntity<List<LoteamentoResponseDto>> listByEmpresa() {
        return ResponseEntity.ok(loteamentoService.listByEmpresa());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoteamentoResponseDto> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(loteamentoService.getById(id));
    }

    @PostMapping("/{id}/quadras")
    public ResponseEntity<QuadraResponseDto> createQuadra(
            @PathVariable Integer id,
            @RequestBody @Valid QuadraCreateDto createDto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loteamentoService.createQuadra(id, createDto));
    }

    @GetMapping("/{loteamentoId}/quadras/{quadraId}/lotes")
    public ResponseEntity<List<LoteResponseDto>> listarLotes(
            @PathVariable Integer loteamentoId,
            @PathVariable Integer quadraId
    ) {
        return ResponseEntity.ok(loteService.listarPorQuadra(loteamentoId, quadraId));
    }

}
