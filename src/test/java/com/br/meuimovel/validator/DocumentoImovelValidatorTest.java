package com.br.meuimovel.validator;

import com.br.meuimovel.exception.DocumentoImovelInvalidoException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class DocumentoImovelValidatorTest {
    private final DocumentoImovelValidator validator = new DocumentoImovelValidator();

    @Test
    void aceitaPdfPelaAssinaturaDoArquivo() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "matricula.pdf", "application/pdf",
                "%PDF-1.7 conteúdo".getBytes());

        assertDoesNotThrow(() -> validator.validar(arquivo));
    }

    @Test
    void rejeitaConteudoQueNaoEArquivoPermitido() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "matricula.pdf", "application/pdf",
                "não é pdf".getBytes());

        assertThrows(DocumentoImovelInvalidoException.class, () -> validator.validar(arquivo));
    }

    @Test
    void rejeitaContentTypeDiferenteDoConteudo() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "matricula.pdf", "image/png",
                "%PDF-1.7 conteúdo".getBytes());

        assertThrows(DocumentoImovelInvalidoException.class, () -> validator.validar(arquivo));
    }

    @Test
    void removeCaminhoOriginalDoNome() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "C:\\pasta\\matricula.pdf", "application/pdf",
                "%PDF-1.7 conteúdo".getBytes());

        assertEquals("matricula.pdf", validator.nomeArquivo(arquivo));
    }
}
