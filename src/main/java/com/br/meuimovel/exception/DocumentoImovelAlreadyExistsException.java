package com.br.meuimovel.exception;

public class DocumentoImovelAlreadyExistsException extends RuntimeException {
    public DocumentoImovelAlreadyExistsException(String message) {
        super(message);
    }
}
