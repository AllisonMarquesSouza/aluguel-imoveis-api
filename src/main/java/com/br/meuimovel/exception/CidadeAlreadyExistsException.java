package com.br.meuimovel.exception;

public class CidadeAlreadyExistsException extends RuntimeException {
    public CidadeAlreadyExistsException(String message) {
        super(message);
    }
}
