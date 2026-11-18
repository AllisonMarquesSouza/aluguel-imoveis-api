package com.br.meuimovel.storage;

import com.br.meuimovel.exception.DocumentoImovelNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalDocumentoStorageServiceTest {
    @TempDir
    Path storageLocation;

    @Test
    void salvaCarregaSubstituiEExcluiArquivoDoDocumento() throws Exception {
        LocalDocumentoStorageService storageService = new LocalDocumentoStorageService(storageLocation.toString());
        String arquivoId = storageService.salvar(pdf("versão inicial"), 15);

        StoredDocument original = storageService.carregar(15, arquivoId);
        assertEquals("application/pdf", original.contentType());
        assertTrue(Files.isRegularFile(original.resource().getFile().toPath()));
        assertTrue(Files.readString(original.resource().getFile().toPath()).contains("versão inicial"));

        storageService.substituir(15, arquivoId, pdf("versão atualizada"));
        StoredDocument atualizado = storageService.carregar(15, arquivoId);
        assertTrue(Files.readString(atualizado.resource().getFile().toPath()).contains("versão atualizada"));

        Path caminho = atualizado.resource().getFile().toPath();
        storageService.deletar(15, arquivoId);
        assertFalse(Files.exists(caminho));
        assertThrows(DocumentoImovelNotFoundException.class, () -> storageService.carregar(15, arquivoId));
    }

    private MockMultipartFile pdf(String texto) {
        return new MockMultipartFile("arquivo", "documento.pdf", "application/pdf",
                ("%PDF-1.7 " + texto).getBytes());
    }
}
