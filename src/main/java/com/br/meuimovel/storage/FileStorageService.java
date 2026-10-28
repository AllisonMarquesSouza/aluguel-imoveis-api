package com.br.meuimovel.storage;

import com.br.meuimovel.enums.TipoImagem;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String salvarImagem(MultipartFile arquivo, TipoImagem tipo, Integer entidadeId);

    StoredImage carregar(TipoImagem tipo, Integer entidadeId, String imagemId);

    void substituir(TipoImagem tipo, Integer entidadeId, String imagemId, MultipartFile arquivo);

    void deletar(TipoImagem tipo, Integer entidadeId, String imagemId);

    default String salvarLogoEmpresa(MultipartFile arquivo, Integer empresaId) {
        return salvarImagem(arquivo, TipoImagem.EMPRESA, empresaId);
    }

    default String salvarFotoCorretor(MultipartFile arquivo, Integer usuarioId) {
        return salvarImagem(arquivo, TipoImagem.CORRETOR, usuarioId);
    }

    default String salvarFotoImovel(MultipartFile arquivo, Integer imovelId, Integer ordem) {
        return salvarImagem(arquivo, TipoImagem.IMOVEL, imovelId);
    }

    void deletar(String caminho);
}
