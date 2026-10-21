package com.br.meuimovel.controller;

import com.br.meuimovel.dtos.corretor.CorretorCreateDto;
import com.br.meuimovel.dtos.corretor.CorretorPublicoResponseDto;
import com.br.meuimovel.dtos.corretor.CorretorResponseDto;
import com.br.meuimovel.service.CorretorService;
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

    @GetMapping
    public ResponseEntity<List<CorretorPublicoResponseDto>> listarPublicos() {
        return ResponseEntity.ok(corretorService.listarPublicos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CorretorPublicoResponseDto> getPerfilPublico(@PathVariable Integer id) {
        return ResponseEntity.ok(corretorService.getPerfilPublico(id));
    }

    @PatchMapping("/ativar/{id}")
    public ResponseEntity<Void> ativar(@PathVariable Integer id) {
        corretorService.ativar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/desativar/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Integer id) {
        corretorService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
