package com.br.meuimovel.validator;

import com.br.meuimovel.enums.TipoDocumento;
import com.br.meuimovel.exception.DocumentoInvalidoException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentoValidatorTest {
    private final DocumentoValidator documentoValidator = new DocumentoValidator();

    @Test
    void validatesFormattedCpfAndReturnsDigitsOnly() {
        assertEquals("52998224725", documentoValidator.validar(TipoDocumento.CPF, "529.982.247-25"));
    }

    @Test
    void validatesFormattedCnpjAndReturnsDigitsOnly() {
        assertEquals("11222333000181", documentoValidator.validar(TipoDocumento.CNPJ, "11.222.333/0001-81"));
    }

    @Test
    void rejectsCpfWithInvalidCheckDigits() {
        assertThrows(DocumentoInvalidoException.class,
                () -> documentoValidator.validar(TipoDocumento.CPF, "529.982.247-24"));
    }

    @Test
    void rejectsCnpjWithInvalidCheckDigits() {
        assertThrows(DocumentoInvalidoException.class,
                () -> documentoValidator.validar(TipoDocumento.CNPJ, "11.222.333/0001-80"));
    }
}
