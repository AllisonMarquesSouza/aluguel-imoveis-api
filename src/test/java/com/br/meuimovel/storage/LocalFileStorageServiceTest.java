package com.br.meuimovel.storage;

import com.br.meuimovel.enums.TipoImagem;
import com.br.meuimovel.exception.ImagemNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageServiceTest {
    @TempDir
    Path diretorioTemporario;

    private final byte[] png = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01
    };

    @Test
    void salvaCarregaSubstituiERemoveImagemPorReferencia() throws Exception {
        LocalFileStorageService storage = new LocalFileStorageService(diretorioTemporario.toString());

        String url = storage.salvarImagem(arquivo("image/png", png), TipoImagem.IMOVEL, 42);
        ImagemUrl.ReferenciaImagem referencia = ImagemUrl.analisar(url);

        assertEquals(TipoImagem.IMOVEL, referencia.tipo());
        assertEquals(42, referencia.entidadeId());
        assertDoesNotThrow(() -> UUID.fromString(referencia.imagemId()));
        StoredImage imagem = storage.carregar(TipoImagem.IMOVEL, 42, referencia.imagemId());
        assertEquals("image/png", imagem.contentType());
        assertArrayEquals(png, imagem.resource().getContentAsByteArray());

        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01};
        storage.substituir(TipoImagem.IMOVEL, 42, referencia.imagemId(), arquivo("image/jpeg", jpeg));
        assertEquals("image/jpeg", storage.carregar(TipoImagem.IMOVEL, 42, referencia.imagemId()).contentType());

        storage.deletar(TipoImagem.IMOVEL, 42, referencia.imagemId());
        assertThrows(ImagemNotFoundException.class,
                () -> storage.carregar(TipoImagem.IMOVEL, 42, referencia.imagemId()));
    }

    @Test
    void rejeitaIdentificadorQueNaoSejaUuid() {
        LocalFileStorageService storage = new LocalFileStorageService(diretorioTemporario.toString());

        assertThrows(ImagemNotFoundException.class,
                () -> storage.carregar(TipoImagem.EMPRESA, 1, "../../application.properties"));
    }

    private MockMultipartFile arquivo(String tipo, byte[] bytes) {
        return new MockMultipartFile("arquivo", "foto", tipo, bytes);
    }
}
