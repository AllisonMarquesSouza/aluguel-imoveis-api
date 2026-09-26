package com.br.meuimovel.controller;

import com.br.meuimovel.dtos.corretor.CorretorCreateDto;
import com.br.meuimovel.dtos.corretor.CorretorResponseDto;
import com.br.meuimovel.service.CorretorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/corretor")
public class CorretorController {
    private final CorretorService corretorService;

    @PostMapping
    public ResponseEntity<CorretorResponseDto> create(@RequestBody @Valid CorretorCreateDto createDto){
        return new ResponseEntity<>(corretorService.create(createDto), HttpStatus.CREATED);
    }
}
