package com.br.meuimovel.controller;

import com.br.meuimovel.dtos.corretor.CorretorCreateDto;
import com.br.meuimovel.dtos.corretor.CorretorUpdateDto;
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

    @GetMapping("/empresa/ativos")
    public ResponseEntity<List<CorretorResponseDto>> listarAtivosDaEmpresa() {
        return ResponseEntity.ok(corretorService.listarAtivosDaEmpresa());
    }

    @GetMapping("/empresa/inativos")
    public ResponseEntity<List<CorretorResponseDto>> listarInativosDaEmpresa() {
        return ResponseEntity.ok(corretorService.listarInativosDaEmpresa());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CorretorPublicoResponseDto> getPerfilPublico(@PathVariable Integer id) {
        return ResponseEntity.ok(corretorService.getPerfilPublico(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CorretorResponseDto> atualizar(
            @PathVariable Integer id,
            @RequestBody @Valid CorretorUpdateDto dto
    ) {
        return ResponseEntity.ok(corretorService.atualizar(id, dto));
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
