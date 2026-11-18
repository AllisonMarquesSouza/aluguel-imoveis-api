package com.br.meuimovel.storage;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentoStorageService {
    String salvar(MultipartFile arquivo, Integer imovelId);

    StoredDocument carregar(Integer imovelId, String arquivoId);

    void substituir(Integer imovelId, String arquivoId, MultipartFile arquivo);

    void deletar(Integer imovelId, String arquivoId);
}
