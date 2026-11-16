package com.br.meuimovel.controller;

import com.br.meuimovel.dtos.predio.PredioCreateDto;
import com.br.meuimovel.dtos.predio.PredioResponseDto;
import com.br.meuimovel.service.PredioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/predio")
public class PredioController {
    private final PredioService predioService;

    @PostMapping
    public ResponseEntity<PredioResponseDto> create(@RequestBody @Valid PredioCreateDto createDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(predioService.create(createDto));
    }

    @GetMapping
    public ResponseEntity<List<PredioResponseDto>> listByEmpresa() {
        return ResponseEntity.ok(predioService.listByEmpresa());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PredioResponseDto> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(predioService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PredioResponseDto> update(
            @PathVariable Integer id,
            @RequestBody @Valid PredioCreateDto updateDto
    ) {
        return ResponseEntity.ok(predioService.update(id, updateDto));
    }
}
