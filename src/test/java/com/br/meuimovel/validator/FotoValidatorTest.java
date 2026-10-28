package com.br.meuimovel.validator;

import com.br.meuimovel.exception.FotoInvalidaException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FotoValidatorTest {
    private final FotoValidator validator = new FotoValidator();

    @Test
    void aceitaImagemPngComAssinaturaValida() {
        MockMultipartFile imagem = new MockMultipartFile("arquivo", "foto.png", "image/png", png());

        assertDoesNotThrow(() -> validator.validar(imagem));
    }

    @Test
    void rejeitaArquivoComExtensaoOuMimeDeImagemMasConteudoInvalido() {
        MockMultipartFile imagem = new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[]{1, 2, 3});

        assertThrows(FotoInvalidaException.class, () -> validator.validar(imagem));
    }

    @Test
    void rejeitaArquivoMaiorQueCincoMegabytes() {
        byte[] grande = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile imagem = new MockMultipartFile("arquivo", "foto.png", "image/png", grande);

        assertThrows(FotoInvalidaException.class, () -> validator.validar(imagem));
    }

    private byte[] png() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01};
    }
}
