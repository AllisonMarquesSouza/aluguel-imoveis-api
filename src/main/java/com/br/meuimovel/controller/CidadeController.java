package com.br.meuimovel.controller;

import com.br.meuimovel.dtos.CidadeCreateDto;
import com.br.meuimovel.dtos.CidadeResponseDto;
import com.br.meuimovel.service.CidadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cidade")
@RequiredArgsConstructor
public class CidadeController {
    private final CidadeService cidadeService;

    @GetMapping
    public ResponseEntity<List<CidadeResponseDto>> listAll(){
        return ResponseEntity.ok(cidadeService.listAll());
    }
    @GetMapping("/{id}")
    public ResponseEntity<CidadeResponseDto> getById(@PathVariable Integer id){
        return ResponseEntity.ok(cidadeService.getResponseById(id));
    }

    @PostMapping
    public ResponseEntity<CidadeResponseDto> create(@RequestBody @Valid CidadeCreateDto createDto){
        return new ResponseEntity<>(cidadeService.create(createDto), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Integer id){
        cidadeService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


}
