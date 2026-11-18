package com.br.meuimovel.controller;

import com.br.meuimovel.dtos.imovel.ImovelVitrineResponseDto;
import com.br.meuimovel.service.ImovelVitrineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/vitrine/imoveis")
public class ImovelVitrineController {
    private final ImovelVitrineService imovelVitrineService;

    @GetMapping
    public ResponseEntity<List<ImovelVitrineResponseDto>> listarDisponiveis() {
        return ResponseEntity.ok(imovelVitrineService.listarDisponiveis());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImovelVitrineResponseDto> buscarDisponivel(@PathVariable Integer id) {
        return ResponseEntity.ok(imovelVitrineService.buscarDisponivel(id));
    }
}
