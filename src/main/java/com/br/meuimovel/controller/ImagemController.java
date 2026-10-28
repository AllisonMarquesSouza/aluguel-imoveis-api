package com.br.meuimovel.controller;

import com.br.meuimovel.dtos.imagem.ImagemResponseDto;
import com.br.meuimovel.enums.TipoImagem;
import com.br.meuimovel.exception.FotoInvalidaException;
import com.br.meuimovel.service.ImagemService;
import com.br.meuimovel.storage.StoredImage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping("/imagens")
public class ImagemController {
    private final ImagemService imagemService;

    @GetMapping("/{tipo}/{entidadeId}/{imagemId}")
    public ResponseEntity<Resource> buscar(
            @PathVariable String tipo,
            @PathVariable Integer entidadeId,
            @PathVariable String imagemId
    ) {
        StoredImage imagem = imagemService.buscar(parseTipo(tipo), entidadeId, imagemId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagem.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(imagemId).build().toString())
                .body(imagem.resource());
    }

    @PostMapping(value = "/{tipo}/{entidadeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImagemResponseDto> adicionar(
            @PathVariable String tipo,
            @PathVariable Integer entidadeId,
            @RequestPart("arquivo") MultipartFile arquivo
    ) {
        ImagemResponseDto imagem = imagemService.adicionar(parseTipo(tipo), entidadeId, arquivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(imagem);
    }

    @PutMapping(value = "/{tipo}/{entidadeId}/{imagemId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImagemResponseDto> atualizar(
            @PathVariable String tipo,
            @PathVariable Integer entidadeId,
            @PathVariable String imagemId,
            @RequestPart("arquivo") MultipartFile arquivo
    ) {
        return ResponseEntity.ok(imagemService.atualizar(parseTipo(tipo), entidadeId, imagemId, arquivo));
    }

    @DeleteMapping("/{tipo}/{entidadeId}/{imagemId}")
    public ResponseEntity<Void> remover(
            @PathVariable String tipo,
            @PathVariable Integer entidadeId,
            @PathVariable String imagemId
    ) {
        imagemService.remover(parseTipo(tipo), entidadeId, imagemId);
        return ResponseEntity.noContent().build();
    }

    private TipoImagem parseTipo(String valor) {
        try {
            return TipoImagem.valueOf(valor.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new FotoInvalidaException("Tipo deve ser empresa, corretor ou imovel.");
        }
    }
}
