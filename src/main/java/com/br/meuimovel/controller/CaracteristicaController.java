package com.br.meuimovel.controller;

import com.br.meuimovel.dtos.CaracteristicaCreateDto;
import com.br.meuimovel.model.Caracteristica;
import com.br.meuimovel.service.CaracteristicaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/caracteristica")
@RequiredArgsConstructor
public class CaracteristicaController {
    private final CaracteristicaService caracteristicaService;

    @GetMapping("/nome")
    public ResponseEntity<Caracteristica> getByNome(@RequestParam String nome){
        return ResponseEntity.ok(caracteristicaService.getByNomeIgnoreCase(nome));
    }

    @GetMapping
    public ResponseEntity<List<Caracteristica>> listAll(){
        return ResponseEntity.ok(caracteristicaService.listAll());
    }

    @PostMapping
    public ResponseEntity<Caracteristica> create(@RequestBody @Valid CaracteristicaCreateDto createDto){
        return new ResponseEntity<>(caracteristicaService.create(createDto), HttpStatus.CREATED);
    }
}
