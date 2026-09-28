package com.br.meuimovel.controller;

import com.br.meuimovel.dtos.corretor.CorretorCreateDto;
import com.br.meuimovel.dtos.corretor.CorretorResponseDto;
import com.br.meuimovel.service.CorretorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/corretor")
public class CorretorController {
    private final CorretorService corretorService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CorretorResponseDto> create(
            @RequestPart("dto") @Valid CorretorCreateDto createDto,
            @RequestPart("foto") MultipartFile foto
    ) {
        return new ResponseEntity<>(
                corretorService.create(createDto, foto),
                HttpStatus.CREATED
        );
    }
}
