package com.br.meuimovel.exception;

public class ApartamentoAlreadyExistsException extends RuntimeException {
    public ApartamentoAlreadyExistsException(String message) {
        super(message);
    }
}
