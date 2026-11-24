package com.br.meuimovel.controller;

import com.br.meuimovel.dtos.usuario.UsuarioResponseDto;
import com.br.meuimovel.dtos.usuario.UsuarioSenhaUpdateDto;
import com.br.meuimovel.dtos.usuario.UsuarioSituacaoUpdateDto;
import com.br.meuimovel.dtos.usuario.UsuarioUpdateDto;
import com.br.meuimovel.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuario")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDto>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDto> buscar(@PathVariable Integer id) {
        return ResponseEntity.ok(usuarioService.buscar(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDto> atualizar(
            @PathVariable Integer id,
            @RequestBody @Valid UsuarioUpdateDto dto
    ) {
        return ResponseEntity.ok(usuarioService.atualizar(id, dto));
    }

    @PatchMapping("/{id}/situacao")
    public ResponseEntity<UsuarioResponseDto> atualizarSituacao(
            @PathVariable Integer id,
            @RequestBody @Valid UsuarioSituacaoUpdateDto dto
    ) {
        return ResponseEntity.ok(usuarioService.atualizarSituacao(id, dto));
    }

    @PatchMapping("/senha")
    public ResponseEntity<Void> atualizarSenha(@RequestBody @Valid UsuarioSenhaUpdateDto dto) {
        usuarioService.atualizarSenha(dto);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
