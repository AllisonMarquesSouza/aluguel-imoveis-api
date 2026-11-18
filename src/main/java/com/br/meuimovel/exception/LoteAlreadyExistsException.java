package com.br.meuimovel.exception;

public class LoteAlreadyExistsException extends RuntimeException {
    public LoteAlreadyExistsException(String message) {
        super(message);
    }
}
