package com.br.meuimovel.controller;

import com.br.meuimovel.dtos.imovel.ImovelCreateDto;
import com.br.meuimovel.dtos.imovel.ImovelResponseDto;
import com.br.meuimovel.service.ImovelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/imovel")
public class ImovelController {
    private final ImovelService imovelService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImovelResponseDto> create(
            @RequestPart("dto") @Valid ImovelCreateDto createDto,
            @RequestPart("fotos") List<MultipartFile> fotos
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(imovelService.create(createDto, fotos));
    }

    @GetMapping
    public ResponseEntity<List<ImovelResponseDto>> listByEmpresa() {
        return ResponseEntity.ok(imovelService.listByEmpresa());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImovelResponseDto> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(imovelService.getById(id));
    }
}
