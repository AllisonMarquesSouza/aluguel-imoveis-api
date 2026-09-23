package com.br.meuimovel.exception;

public class UsuarioAlreadyExistsException extends RuntimeException {
    public UsuarioAlreadyExistsException(String message) {
        super(message);
    }
}
