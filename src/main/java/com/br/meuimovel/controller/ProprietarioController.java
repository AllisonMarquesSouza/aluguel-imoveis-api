package com.br.meuimovel.controller;

import com.br.meuimovel.dtos.proprietario.ProprietarioCreateDto;
import com.br.meuimovel.enums.TipoDocumento;
import com.br.meuimovel.model.Proprietario;
import com.br.meuimovel.service.ProprietarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/proprietario")
@RequiredArgsConstructor
public class ProprietarioController {
    private final ProprietarioService proprietarioService;

    @GetMapping("/documento")
    public ResponseEntity<Proprietario> getByDocumento(@RequestParam TipoDocumento tipoDocumento,
                                                       @RequestParam String documento){
        return new ResponseEntity<>(proprietarioService.getByDocumento(tipoDocumento, documento), HttpStatus.OK);
    }
    @GetMapping
    public ResponseEntity<List<Proprietario>> listByNome(@RequestParam String nome){
        return new ResponseEntity<>(proprietarioService.listByNome(nome), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Proprietario> create(@RequestBody @Valid ProprietarioCreateDto createDto){
        return new ResponseEntity<>(proprietarioService.create(createDto), HttpStatus.CREATED);
    }

}
