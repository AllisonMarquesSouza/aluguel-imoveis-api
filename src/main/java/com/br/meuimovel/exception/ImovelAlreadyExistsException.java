package com.br.meuimovel.exception;

public class ImovelAlreadyExistsException extends RuntimeException {
    public ImovelAlreadyExistsException(String message) {
        super(message);
    }
}
