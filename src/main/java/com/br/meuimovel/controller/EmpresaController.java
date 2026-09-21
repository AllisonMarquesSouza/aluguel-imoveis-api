package com.br.meuimovel.controller;

import com.br.meuimovel.dtos.EmpresaCreateDto;
import com.br.meuimovel.dtos.EmpresaUpdateDto;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.service.EmpresaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public ResponseEntity<Empresa> create(@RequestBody @Valid EmpresaCreateDto createDto){
        return new ResponseEntity<>(empresaService.create(createDto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id, @RequestBody @Valid EmpresaUpdateDto updateDto){
        empresaService.update(id, updateDto);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
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

