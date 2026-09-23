package com.br.meuimovel.controller;

import com.br.meuimovel.dtos.autenticacao.AutenticacaoDto;
import com.br.meuimovel.dtos.autenticacao.RegisterDto;
import com.br.meuimovel.dtos.autenticacao.TokenDto;
import com.br.meuimovel.dtos.autenticacao.UsuarioResponseDto;
import com.br.meuimovel.service.AutenticacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping( "/auth")
@RequiredArgsConstructor
public class AutenticacaoController {
    private final AutenticacaoService autenticacaoService;

    @PostMapping("/login")
    public ResponseEntity<TokenDto> login(@RequestBody @Valid AutenticacaoDto dto){
        return ResponseEntity.ok(autenticacaoService.login(dto));
    }

    @PostMapping("/register")
    public ResponseEntity<UsuarioResponseDto> register(@RequestBody @Valid RegisterDto dto){
        return ResponseEntity.ok(autenticacaoService.register(dto));
    }
}
