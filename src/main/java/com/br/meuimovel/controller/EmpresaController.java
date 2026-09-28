package com.br.meuimovel.controller;

import com.br.meuimovel.dtos.empresa.EmpresaAutonomaCreateDto;
import com.br.meuimovel.dtos.empresa.EmpresaImobiliariaCreateDto;
import com.br.meuimovel.dtos.empresa.EmpresaUpdateDto;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.service.EmpresaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/empresa")
@RequiredArgsConstructor
public class EmpresaController {
    private final EmpresaService empresaService;

    @GetMapping
    public ResponseEntity<List<Empresa>> listAll(){
        return ResponseEntity.ok(empresaService.listAll());
    }
    @GetMapping("/{id}")
    public ResponseEntity<Empresa> getById(@PathVariable Integer id){
        return ResponseEntity.ok(empresaService.getById(id));
    }

    @PostMapping(value = "/imobiliaria", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Empresa> createImobiliaria(@RequestPart("dto") @Valid EmpresaImobiliariaCreateDto createDto,
                                                     @RequestPart("logoEmpresa") MultipartFile logoImg){
        return new ResponseEntity<>(empresaService.createImobiliaria(createDto, logoImg), HttpStatus.CREATED);
    }

    @PostMapping(value = "/autonoma", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Empresa> createAutonoma(@RequestPart("dto") @Valid EmpresaAutonomaCreateDto createDto,
                                                     @RequestPart("logoEmpresa") MultipartFile logoImg,
                                                     @RequestPart("fotoCorretor") MultipartFile fotoCorretor){
        return new ResponseEntity<>(empresaService.createAutonoma(createDto, logoImg, fotoCorretor), HttpStatus.CREATED);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> update(
            @PathVariable Integer id,
            @RequestPart("dto") @Valid EmpresaUpdateDto updateDto,
            @RequestPart(value = "logoEmpresa", required = false) MultipartFile logoImg
    ) {
        empresaService.update(id, updateDto, logoImg);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/activate/{id}")
    public ResponseEntity<Void> ativar(@PathVariable Integer id){
        empresaService.activate(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping("/inactivate/{id}")
    public ResponseEntity<Void> inativar(@PathVariable Integer id){
        empresaService.inactivate(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id){
        empresaService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}

