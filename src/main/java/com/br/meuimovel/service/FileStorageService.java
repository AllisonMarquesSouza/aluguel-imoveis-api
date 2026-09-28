package com.br.meuimovel.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String salvarLogoEmpresa(
            MultipartFile arquivo,
            Integer empresaId
    );

    String salvarFotoCorretor(
            MultipartFile arquivo,
            Integer usuarioId
    );

    void deletar(String caminho);
}