package com.br.meuimovel.controller;

import com.br.meuimovel.dtos.imovel.DocumentoImovelResponseDto;
import com.br.meuimovel.enums.TipoDocumentoImovel;
import com.br.meuimovel.service.DocumentoImovelService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/imovel/{imovelId}/documentos")
public class DocumentoImovelController {
    private final DocumentoImovelService documentoImovelService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoImovelResponseDto> adicionar(
            @PathVariable Integer imovelId,
            @RequestParam("tipo") @NotNull TipoDocumentoImovel tipo,
            @RequestParam("dataDocumento") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @NotNull LocalDate dataDocumento,
            @RequestPart("arquivo") MultipartFile arquivo
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentoImovelService.adicionar(imovelId, tipo, dataDocumento, arquivo));
    }

    @GetMapping
    public ResponseEntity<List<DocumentoImovelResponseDto>> listar(@PathVariable Integer imovelId) {
        return ResponseEntity.ok(documentoImovelService.listar(imovelId));
    }

    @PutMapping(value = "/{documentoId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoImovelResponseDto> atualizar(
            @PathVariable Integer imovelId,
            @PathVariable Integer documentoId,
            @RequestParam("dataDocumento") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @NotNull LocalDate dataDocumento,
            @RequestPart("arquivo") MultipartFile arquivo
    ) {
        return ResponseEntity.ok(documentoImovelService.atualizar(imovelId, documentoId, dataDocumento, arquivo));
    }

    @GetMapping("/{documentoId}/arquivo")
    public ResponseEntity<Resource> buscarArquivo(
            @PathVariable Integer imovelId,
            @PathVariable Integer documentoId
    ) {
        DocumentoImovelService.ArquivoDocumento documento =
                documentoImovelService.buscarArquivo(imovelId, documentoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(documento.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(documento.nomeArquivo()).build().toString())
                .body(documento.resource());
    }
}
